# Sistema de Gestão Comercial com Microsserviços

Projeto integrador da disciplina **Arquitetura de Microsserviços e Escalabilidade**.

## 1. Visão geral

O sistema implementa um fluxo comercial distribuído com três capacidades de negócio:

- **Contratos**: vigência, status e elegibilidade comercial;
- **Compras/Estoque**: cadastro de insumos, disponibilidade e baixa de estoque;
- **Vendas**: criação do pedido e orquestração do fluxo de negócio.

O objetivo da solução é demonstrar:

- decomposição por **Bounded Contexts**, e não por camada técnica;
- **deploy independente** de serviços;
- **Database per Service**;
- comunicação síncrona via HTTP entre serviços;
- **observabilidade** com métricas, logs e traces;
- medição de performance com **p50, p95, p99 e throughput**;
- critérios formais de **SLI, SLO e SLA**.

## 2. Por que usar microsserviços neste caso

Microsserviços fazem sentido aqui porque o domínio tem responsabilidades diferentes e acoplamento de negócio explícito:

- **Contratos** responde se uma venda é comercialmente válida;
- **Compras** responde se há estoque suficiente para a operação;
- **Vendas** coordena essas decisões e persiste o pedido.

Se tudo estivesse em um único processo:

- qualquer mudança em venda, contrato ou estoque exigiria redeploy do sistema inteiro;
- a escalabilidade seria forçada para todos os módulos ao mesmo tempo;
- a observabilidade de falhas entre domínios ficaria diluída;
- a evolução do modelo de dados ficaria mais acoplada.

Aqui, a decomposição foi feita por **capacidade de negócio**, alinhada ao material da disciplina e ao princípio de evitar “decomposição por função técnica”.

## 3. Bounded Contexts

| Bounded Context | Responsabilidade | Serviço |
|---|---|---|
| Contratos | vigência, status e validação comercial do contrato | `contratos-service` |
| Compras | insumos, estoque disponível, baixa de estoque e pedidos de compra | `compras-service` |
| Vendas | criação do pedido e orquestração entre contrato e estoque | `vendas-service` |

## 4. Arquitetura da solução

```text
Cliente
  |
  v
API Gateway (:8080)
  |
  +--> contratos-service (:8082)
  |
  +--> compras-service (:8081)
  |
  +--> vendas-service (:8083)
          |
          +--> contratos-service
          |
          +--> compras-service

Eureka Server (:8761)
Prometheus (:9090)
Grafana (:3000)
Jaeger (:16686)
Loki + Promtail
PostgreSQL (:5432)
```

### Padrões usados

- **API Gateway** para ponto único de entrada;
- **Service Discovery** com Eureka;
- **OpenFeign** para comunicação síncrona entre serviços;
- **Circuit Breaker** e **Retry** no fluxo de vendas;
- **Correlation ID** para rastreabilidade distribuída;
- **Docker Compose** para execução local do ambiente completo.

## 5. Fluxo principal da apresentação

Fluxo principal: **criação de pedido de venda**.

1. o cliente envia `POST /api/vendas/pedidos` pelo gateway;
2. `vendas-service` valida o contrato no `contratos-service`;
3. `vendas-service` valida disponibilidade no `compras-service`;
4. `vendas-service` baixa o estoque no `compras-service`;
5. `vendas-service` persiste o pedido e retorna `201 Created`.

Esse fluxo permite demonstrar:

- comunicação entre microsserviços;
- dependência entre contextos delimitados;
- falha comercial por contrato inválido;
- falha operacional por estoque insuficiente;
- rastreamento de ponta a ponta com logs e traces.

## 6. SLI, SLO e SLA

### Conceitos

- **Métrica**: valor observável, como throughput ou tempo de resposta.
- **SLI**: métrica formalizada com fórmula, unidade, escopo e janela.
- **SLO**: meta definida para um SLI.
- **SLA**: compromisso global assumido com base em um conjunto de SLOs.

### Como foi aplicado neste projeto

- **SLIs e SLOs por microsserviço**:
  - `contratos-service`
  - `compras-service`
  - `vendas-service`
- **SLIs e SLOs do fluxo principal**:
  - `POST /api/vendas/pedidos`
- **SLIs globais e SLA da aplicação**:
  - disponibilidade global
  - latência P95 global
  - latência P99 global

### Resultado consolidado

Execução validada com os parâmetros padrão do script (60s, 12 workers):

```bash
make perf
```

Resumo global:

- disponibilidade global: **100,00%**
- P95 global: **94,68 ms**
- P99 global: **109,44 ms**
- throughput global: **265,07 req/s**

Resultado do fluxo principal:

- disponibilidade: **100,00%**
- sucesso de negócio: **100,00%**
- P95: **101,17 ms**
- P99: **116,28 ms**

Detalhamento completo em [doc/performance.md](doc/performance.md).

## 7. Observabilidade

O projeto implementa os três pilares da observabilidade:

| Pilar | Ferramenta | URL |
|---|---|---|
| Métricas | Prometheus + Grafana | http://localhost:9090 / http://localhost:3000 |
| Logs | Loki + Promtail + Grafana | http://localhost:3000 |
| Traces | Jaeger | http://localhost:16686 |

### Correlation ID

Cada requisição recebe ou reaproveita `X-Correlation-ID`.

Esse valor:

- é gerado ou preservado pelo gateway;
- é propagado entre os serviços;
- aparece nos logs junto de `traceId` e `spanId`;
- retorna ao cliente na resposta.

Isso atende ao critério da disciplina para **logging estruturado com Correlation ID**.

## 8. Health check

Todos os serviços expõem `/health`. Os serviços de negócio são acessados exclusivamente pelo gateway; os health checks externos ficam disponíveis nos pontos de entrada públicos:

```bash
curl http://localhost:8080/health
curl http://localhost:8761/health
```

O `docker-compose.yml` usa esses endpoints internamente para health check operacional dos containers.

## 9. Segurança

O foco da entrega não é autenticação completa, mas a arquitetura já considera:

- ponto único de entrada no `api-gateway`;
- isolamento por serviço e por banco;
- configuração por variáveis de ambiente;
- validação de entrada e tratamento consistente de erro;
- estrutura preparada para evolução futura com JWT/mTLS.

## 10. Subindo o ambiente

```bash
docker compose up --build -d
```

Verificar o estado:

```bash
docker compose ps
```

Serviços principais:

| Serviço | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Eureka Server | http://localhost:8761 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| Jaeger | http://localhost:16686 |

Os serviços de negócio (`compras-service`, `contratos-service`, `vendas-service`) não expõem porta no host — são acessados exclusivamente pelo gateway em `:8080`.

## 11. Demonstração ao vivo

Use um identificador único para tornar os comandos reexecutáveis:

```bash
RUN_ID=$(date +%s)
```

### 11.1 Criar contrato

```bash
CONTRATO_ID=$(curl -s -X POST http://localhost:8080/api/contratos \
  -H "Content-Type: application/json" \
  -d '{
    "numero": "CTR-DEMO-'"$RUN_ID"'",
    "nomeContratante": "Cliente Demo",
    "valorTotal": 1000.00,
    "dataInicio": "2026-01-01",
    "dataFim": "2026-12-31",
    "status": "ATIVO",
    "termos": "Contrato para demonstracao"
  }' | jq -r '.id')
```

### 11.2 Criar insumo

```bash
INSUMO_ID=$(curl -s -X POST http://localhost:8080/api/compras/insumos \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Insumo Demo '"$RUN_ID"'",
    "descricao": "Material de demonstracao",
    "unidadeMedida": "UN",
    "precoUnitario": 10.00,
    "quantidadeEstoque": 100
  }' | jq -r '.id')
```

### 11.3 Cenário de sucesso

```bash
curl -s -X POST http://localhost:8080/api/vendas/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "nomeCliente": "Cliente Demo",
    "contratoId": '"$CONTRATO_ID"',
    "itens": [{
      "insumoId": '"$INSUMO_ID"',
      "nomeInsumo": "Insumo Demo '"$RUN_ID"'",
      "quantidade": 2,
      "precoUnitario": 10.00
    }]
  }' | jq .
```

Resultado esperado: `201 Created`.

### 11.4 Falha comercial

```bash
curl -s -X PATCH "http://localhost:8080/api/contratos/$CONTRATO_ID/status?status=SUSPENSO" | jq .

curl -i -X POST http://localhost:8080/api/vendas/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "nomeCliente": "Cliente Demo",
    "contratoId": '"$CONTRATO_ID"',
    "itens": [{
      "insumoId": '"$INSUMO_ID"',
      "nomeInsumo": "Insumo Demo '"$RUN_ID"'",
      "quantidade": 2,
      "precoUnitario": 10.00
    }]
  }'
```

Resultado esperado: `422 Unprocessable Entity`.

### 11.5 Falha operacional

```bash
curl -s -X PATCH "http://localhost:8080/api/contratos/$CONTRATO_ID/status?status=ATIVO" | jq .

curl -i -X POST http://localhost:8080/api/vendas/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "nomeCliente": "Cliente Demo",
    "contratoId": '"$CONTRATO_ID"',
    "itens": [{
      "insumoId": '"$INSUMO_ID"',
      "nomeInsumo": "Insumo Demo '"$RUN_ID"'",
      "quantidade": 99999,
      "precoUnitario": 10.00
    }]
  }'
```

Resultado esperado: `422 Unprocessable Entity`.

## 12. Swagger e documentação da API

A documentação de todos os serviços está agregada no gateway:

| URL |
|---|
| http://localhost:8080/swagger-ui.html |

## 13. Testes

### Unitários

```bash
make test
```

### Performance

```bash
make perf
```

Arquivos gerados:

- [tests/performance/results/summary.json](tests/performance/results/summary.json)
- [tests/performance/results/summary.md](tests/performance/results/summary.md)

## 14. Artefatos da disciplina

| Artefato | Local |
|---|---|
| Documento de arquitetura consolidado | [doc/arquitetura.md](doc/arquitetura.md) |
| ADR-001 | [doc/adr-001-escolha-banco.md](doc/adr-001-escolha-banco.md) |
| ADR-002 | [doc/adr-002-comunicacao-feign.md](doc/adr-002-comunicacao-feign.md) |
| ADR-003 | [doc/adr-003-observabilidade.md](doc/adr-003-observabilidade.md) |
| Resultado de performance | [doc/performance.md](doc/performance.md) |
| Checklist de Peer Review | [doc/peer-review-checklist.md](doc/peer-review-checklist.md) |

## 15. Critérios de avaliação atendidos

Este repositório atende à estrutura solicitada no plano de ensino com:

- documento de arquitetura consolidado;
- mínimo de três ADRs;
- microsserviços funcionais com lógica de negócio;
- `/health` implementado;
- logging estruturado com `Correlation ID`;
- `docker compose up --build` para o ambiente completo;
- medição de performance com `p50`, `p95`, `p99` e throughput;
- checklist de Peer Review.
