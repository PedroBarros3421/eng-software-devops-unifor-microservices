# Documento de Arquitetura — Sistema de Gestão Comercial (Microsserviços)

**Versão:** 1.0  
**Data:** 2026-04-25  
**Disciplina:** Arquitetura de Microsserviços e Escalabilidade

---

## 1. Contexto e Problema

### 1.1 Contexto

Este projeto implementa um sistema de gestão comercial simplificado como trabalho prático da disciplina de Arquitetura de Microsserviços e Escalabilidade. O sistema gerencia contratos com fornecedores, controle de estoque de insumos e processamento de pedidos de venda.

### 1.2 Problema

Sistemas monolíticos tradicionais tornam difíceis:

- Escalabilidade independente de domínios com diferentes perfis de carga
- Deploy isolado de funcionalidades sem risco de regressão global
- Evolução de um módulo sem impactar outros
- Rastreabilidade end-to-end em ambientes distribuídos

### 1.3 Solução Adotada

Decomposição do sistema em **microsserviços alinhados a Bounded Contexts de DDD**, com infraestrutura de suporte para service discovery, roteamento, resiliência e observabilidade.

---

## 2. Bounded Contexts

O domínio foi decomposto em três contextos delimitados:

| Bounded Context | Responsabilidade | Serviço |
|---|---|---|
| **Contratos** | Gerencia contratos com fornecedores: vigência, termos e status (ATIVO/INATIVO/PENDENTE) | `contratos-service` |
| **Compras** | Gerencia insumos e controla estoque: cadastro de itens, quantidade disponível, baixa por pedido | `compras-service` |
| **Vendas** | Orquestra o fluxo de pedido de venda: valida contrato, verifica e baixa estoque, persiste a venda | `vendas-service` |

Cada serviço é dono exclusivo de seu banco de dados, sem acesso direto ao banco de outro serviço (Database per Service pattern).

---

## 3. Diagrama de Serviços e Protocolos

```
Cliente
  │
  │  HTTP REST
  ▼
┌─────────────────────────────────┐
│         API Gateway             │  :8080
│  (Spring Cloud Gateway)         │
│  - Roteamento por prefixo       │
│  - Gera/preserva X-Correlation-ID│
│  - /health explícito            │
└────────┬──────────┬─────────────┘
         │          │
         │ HTTP/REST │ HTTP/REST
         ▼          ▼
┌───────────────┐  ┌───────────────┐
│contratos-svc  │  │ compras-svc   │  
│    :8081      │  │    :8082      │
│ /api/contratos│  │/api/compras/  │
│               │  │  insumos      │
└───────────────┘  └───────┬───────┘
                           │ (baixa estoque)
         ▲─────────────────┤
         │                 │
┌────────┴──────────────────────────┐
│          vendas-service           │  :8083
│   Feign + Resilience4j            │
│   → ContratosClient               │
│   → ComprasClient                 │
│   /api/vendas/pedidos             │
└───────────────────────────────────┘

Service Discovery: Eureka Server :8761
Todos os serviços registram-se e resolvem nomes via Eureka.
```

### 3.1 Fluxo Principal: Criar Pedido de Venda

```
POST /api/vendas/pedidos
        │
        ▼ (via gateway)
vendas-service
        │
        ├─[1]─► contratos-service: GET /api/contratos/{id}
        │        └─ valida status ATIVO
        │
        ├─[2]─► compras-service: GET /api/compras/insumos/{id}
        │        └─ verifica quantidade disponível
        │
        ├─[3]─► compras-service: PUT /api/compras/insumos/{id}/baixar
        │        └─ decrementa estoque
        │
        └─[4]─► persiste Venda no banco vendas_db
                └─ resposta 201 Created
```

### 3.2 Fluxos de Erro

| Cenário | Comportamento |
|---|---|
| Contrato inválido/inativo | vendas-service retorna 422 (Unprocessable Entity) |
| Estoque insuficiente | vendas-service retorna 422 (Unprocessable Entity) |
| contratos-service indisponível | Circuit Breaker ativa fallback; retorna status INDISPONIVEL; venda rejeitada |
| contratos-service lento (transiente) | Retry com backoff exponencial (3 tentativas, 500ms × 2) |

---

## 4. Infraestrutura de Suporte

| Componente | Tecnologia | Porta | Função |
|---|---|---|---|
| Service Discovery | Eureka Server | 8761 | Registro e resolução de serviços |
| API Gateway | Spring Cloud Gateway | 8080 | Ponto único de entrada, roteamento, Correlation ID |
| Banco de Dados | PostgreSQL | 5432 | Persistência de todos os serviços (3 databases isolados) |
| Tracing | Jaeger | 16686 | Rastreamento distribuído via OpenTelemetry/OTLP |
| Métricas | Prometheus | 9090 | Coleta de métricas via scrape do Actuator |
| Logs | Loki + Promtail | 3100 | Agregação de logs dos containers |
| Dashboards | Grafana | 3000 | Visualização unificada de métricas, logs e traces |

---

## 5. SLOs (Service Level Objectives)

Os SLOs abaixo foram definidos para o endpoint principal (`POST /api/vendas/pedidos`) sob carga de 8 workers simultâneos:

| Indicador | Meta SLO | Meta SLA | Resultado (30s, 8 workers) | Status |
|---|---|---|---|---|
| Disponibilidade técnica | ≥ 99% | ≥ 95% | 100% | ✓ OK |
| Sucesso de negócio | ≥ 99% | ≥ 95% | 100% | ✓ OK |
| P95 latência | ≤ 500 ms | ≤ 800 ms | 63,72 ms | ✓ OK |
| P99 latência | ≤ 1000 ms | ≤ 1500 ms | 75,91 ms | ✓ OK |

Resultados completos em [`doc/performance.md`](./performance.md).

---

## 6. Observabilidade

### 6.1 Os Três Pilares

| Pilar | Ferramenta | Endpoint/Acesso |
|---|---|---|
| Métricas | Prometheus + Grafana | `localhost:9090`, `localhost:3000` |
| Logs | Loki + Promtail + Grafana | `localhost:3100` (Loki), explorar via Grafana |
| Traces | Jaeger + OpenTelemetry | `localhost:16686` |

### 6.2 Correlação de Requisições

Cada requisição recebe um `X-Correlation-ID` (UUID v4) gerado ou preservado pelo API Gateway. Esse ID é:

- Propagado como header HTTP para todos os serviços downstream
- Armazenado no MDC do SLF4J de cada serviço
- Incluído em cada linha de log no formato: `[servico,correlationId,traceId,spanId]`
- Retornado ao cliente na resposta

Isso permite rastrear todos os logs e spans de uma única requisição usando o `correlationId` como chave de busca no Grafana/Loki ou o `traceId` no Jaeger.

### 6.3 Health Checks

Todos os serviços expõem `/health` (implementado explicitamente) além do `/actuator/health` do Spring Boot. O `docker-compose.yml` usa esses endpoints para health checks dos containers, garantindo que dependências entre serviços sejam respeitadas na inicialização.

---

## 7. Resiliência

### 7.1 Padrões Implementados

| Padrão | Onde | Configuração |
|---|---|---|
| Circuit Breaker | vendas → contratos | 50% falhas em 10 chamadas; open por 10s |
| Retry | vendas → contratos | 3 tentativas, backoff exponencial 500ms × 2 |
| Fallback | vendas → contratos | Retorna status INDISPONIVEL quando circuit aberto |
| Service Discovery | Todos os serviços | Resolução de nomes via Eureka |

### 7.2 Monitoramento de Resiliência

O estado dos Circuit Breakers é exposto via `/actuator/circuitbreakers` e pode ser visualizado no Grafana.

---

## 8. Segurança

### 8.1 Escopo Atual

O projeto não implementa autenticação/autorização no nível de aplicação (fora do escopo da disciplina). As medidas de proteção presentes são:

| Medida | Descrição |
|---|---|
| Ponto único de entrada | Todo tráfego externo passa pelo API Gateway na porta 8080 |
| Isolamento de rede | Os serviços internos (vendas, contratos, compras) não precisam expor portas ao host em produção |
| Database per Service | Cada serviço acessa apenas seu próprio banco de dados |
| Variáveis de ambiente | Credenciais do banco injetadas via variáveis de ambiente no Docker Compose (não hardcoded) |

### 8.2 Recomendações para Produção

- Implementar OAuth2/JWT no API Gateway para autenticação de clientes externos
- Mutual TLS (mTLS) para comunicação inter-serviços
- Rotação automática de secrets via HashiCorp Vault ou AWS Secrets Manager
- Network policies no Kubernetes para isolamento de rede

---

## 9. Execução Local

```bash
# Subir toda a infraestrutura
docker compose up --build -d

# Verificar health de todos os serviços
curl http://localhost:8080/health         # gateway
curl http://localhost:8761/actuator/health # eureka

# Executar testes unitários
make test

# Executar teste de performance
DURATION_SECONDS=30 WORKERS=8 make perf

# Acessar observabilidade
# Grafana:    http://localhost:3000
# Jaeger:     http://localhost:16686
# Prometheus: http://localhost:9090
# Eureka:     http://localhost:8761
```

---

## 10. Decisões Arquiteturais

As decisões relevantes estão registradas nos ADRs:

| ADR | Decisão | Status |
|---|---|---|
| [ADR-001](./adr-001-escolha-banco.md) | Escolha do banco de dados por serviço (PostgreSQL para todos) | Aceito, revisado |
| [ADR-002](./adr-002-comunicacao-feign.md) | Comunicação inter-serviços via OpenFeign + Resilience4j | Aceito, revisado |
| [ADR-003](./adr-003-observabilidade.md) | Estratégia de observabilidade com Grafana stack + Jaeger + Correlation ID | Aceito |
