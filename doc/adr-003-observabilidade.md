# ADR-003: Estratégia de Observabilidade com Rastreamento Distribuído e Correlação de Logs

## Contexto

Em uma arquitetura de microsserviços, uma única requisição de negócio atravessa múltiplos serviços (ex: gateway → vendas → contratos → compras). Isso torna difícil:

1. Correlacionar logs de diferentes serviços que pertencem à mesma requisição
2. Identificar gargalos de latência em chamadas entre serviços
3. Detectar falhas em cascata e suas origens
4. Monitorar métricas de disponibilidade e SLO em tempo real

Era necessário definir uma estratégia de observabilidade que cobrisse os três pilares: **logs**, **métricas** e **traces**.

## Alternativas Consideradas

| Alternativa | Prós | Contras |
|---|---|---|
| Apenas logs estruturados | Simplicidade | Sem correlação automática, sem rastreamento de latência end-to-end |
| ELK Stack (Elasticsearch + Logstash + Kibana) | Maturidade, poder de busca | Alto consumo de recursos, complexidade operacional |
| **Stack Grafana (Loki + Tempo + Prometheus + Grafana)** | Integração nativa, leve, logs sem índice, custo menor | Menos recursos de busca full-text que Elasticsearch |
| Datadog / New Relic (SaaS) | Observabilidade completa com mínima operação | Custo, dependência de fornecedor, não adequado para ambiente acadêmico |
| OpenTelemetry + Jaeger | Padrão aberto, vendor-neutral para traces | Requer complemento para logs e métricas |

## Decisão

Adotar a **stack Grafana + Jaeger** com OpenTelemetry como camada de instrumentação:

### Componentes

| Componente | Função |
|---|---|
| **Prometheus** | Coleta e armazenamento de métricas (scrape via `/actuator/prometheus`) |
| **Loki** | Agregação e armazenamento de logs estruturados |
| **Promtail** | Agente de coleta de logs dos containers Docker |
| **Jaeger** | Rastreamento distribuído (traces e spans via OTLP) |
| **Grafana** | Visualização unificada: dashboards de métricas, exploração de logs e traces |

### Correlação de Requisições: X-Correlation-ID

Implementou-se um mecanismo explícito de correlação que opera em camadas:

1. **API Gateway**: gera um `X-Correlation-ID` (UUID v4) se o header não estiver presente na requisição entrada; preserva o valor se já vier do cliente
2. **Propagação**: todos os serviços downstream (vendas, contratos, compras) leem o header e armazenam o valor no MDC (Mapped Diagnostic Context) do SLF4J
3. **Logs**: cada linha de log inclui `correlationId`, `traceId` e `spanId`
4. **Headers de resposta**: o `X-Correlation-ID` é retornado nas respostas para rastreabilidade pelo cliente

### Padrão de log estruturado

```
[vendas-service,<correlationId>,<traceId>,<spanId>] INFO  ...mensagem...
```

### Rastreamento distribuído com OpenTelemetry

- `spring-boot-starter-actuator` + `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`
- Probabilidade de amostragem: 100% (`probability: 1.0`) em desenvolvimento
- Traces enviados ao Jaeger via OTLP HTTP (`/v1/traces`)

## Consequências

**Positivo:**
- Diagnóstico completo de uma requisição end-to-end: um único `correlationId` ou `traceId` identifica todos os logs e spans relacionados
- Stack inteiramente open-source, executável em Docker Compose sem dependência de serviços externos
- Grafana unifica métricas, logs e traces em uma interface, com navegação entre pilares (ex: de um pico de latência no Prometheus direto para o trace no Jaeger)
- Métricas Prometheus expõem automaticamente JVM, conexões de banco, Feign, Resilience4j e métricas de negócio via Actuator

**Negativo:**
- Promtail coleta logs em texto dos containers e os repassa ao Loki; para logs estruturados (JSON) seria necessário configuração adicional de parsing
- Amostragem a 100% em produção geraria volume alto de dados; recomenda-se reduzir para 10–20% em ambientes de alta carga
- A correlação entre `X-Correlation-ID` e `traceId` do Jaeger é feita via log, mas não são o mesmo identificador — exige busca cruzada

## Referências

- Spring Boot Actuator: `/actuator/health`, `/actuator/prometheus`, `/actuator/circuitbreakers`
- Grafana: `http://localhost:3000`
- Jaeger UI: `http://localhost:16686`
- Prometheus: `http://localhost:9090`

## Histórico de Revisões

| Campo | Valor |
|---|---|
| Sistema | Sistema de Gestão Comercial — Microsserviços |
| Autores | Edval Júnior, Iago Barbosa, Mary Santos, Pedro Barros, Victor Kauan |
| Revisores | — |
| Supersede | — |
| Supersedido por | — |

| Versão | Data | Autor | Alteração |
|---|---|---|---|
| 1.0 | 2026-04-25 | Equipe | Criação inicial |

**Status atual:** Aceito
