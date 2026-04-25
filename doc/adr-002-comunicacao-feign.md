# ADR-002: Comunicação entre Serviços via OpenFeign com Resilience4j

**Status:** Aceito (revisado em 2026-04-25)
**Data:** 2026-04-12
**Revisão:** 2026-04-25

## Contexto

O `vendas-service` precisa consultar dados do `contratos-service` para validar contratos vinculados a pedidos de venda, e do `compras-service` para verificar e baixar estoque. Foi necessário definir:

1. O protocolo de comunicação entre microsserviços
2. A estratégia de resiliência para lidar com falhas temporárias ou indisponibilidade

## Alternativas Consideradas

| Alternativa | Prós | Contras |
|---|---|---|
| RestTemplate manual | Sem dependência adicional | Boilerplate HTTP, sem integração nativa com Eureka/LoadBalancer |
| OpenFeign (escolhido) | Declarativo, integração nativa com Eureka e Resilience4j, código limpo | Comunicação síncrona mantém acoplamento temporal |
| Mensageria assíncrona (Kafka/RabbitMQ) | Desacoplamento temporal, tolerância a falhas | Complexidade de implementação fora do escopo do projeto |
| gRPC | Alta performance, contrato forte | Curva de aprendizado, incompatível com infraestrutura HTTP atual |

## Decisão

Adotar **OpenFeign** para comunicação síncrona HTTP, integrado ao **Eureka** para resolução de endereços, e **Resilience4j** para resiliência em camadas:

### Comunicação: OpenFeign + Eureka

- O `ContratosClient` usa `@FeignClient(name = "contratos-service")` e o `ComprasClient` usa `@FeignClient(name = "compras-service")`, com nomes resolvidos via Eureka (sem URLs hardcoded)
- O Spring Cloud LoadBalancer distribui a carga caso existam múltiplas instâncias

### Resiliência em camadas (vendas-service → contratos-service)

| Padrão | Configuração | Comportamento |
|---|---|---|
| **Retry** | 3 tentativas, backoff exponencial (500ms × 2) | Reenvio automático em falhas transitórias de rede |
| **Circuit Breaker** | 50% de falhas em janela de 10 chamadas abre o disjuntor por 10s | Interrompe chamadas a serviços instáveis, evitando cascata de falhas |
| **Fallback** | Retorna `ContratoDTO` com `status = "INDISPONIVEL"` | Resposta degradada mas funcional quando o serviço está fora do ar |

### Fluxo de chamada com resiliência

```
VendasController → VendasService
                        │
                   consultarContrato()
                   @Retry (3x com backoff exponencial)
                   @CircuitBreaker (CLOSED → OPEN → HALF-OPEN)
                   ContratosClient (Feign + Eureka)
                        │
                   contratos-service /api/contratos/{id}
                        │
                   verificarEstoque() / baixarEstoque()
                   ComprasClient (Feign + Eureka)
                        │
                   compras-service /api/compras/insumos
```

## Nota de revisão (2026-04-25)

A versão anterior desta ADR documentava um **Rate Limiter de 10 req/s** no endpoint `POST /api/vendas/pedidos`. Durante os testes de performance, identificou-se que esse limite causava rejeições artificiais (HTTP 429/500) com apenas 8 workers concorrentes, distorcendo as métricas de disponibilidade.

Após análise, o Rate Limiter foi **removido do endpoint de criação de pedidos**. O controle de sobrecarga permanece via Circuit Breaker nas chamadas downstream e via recursos do API Gateway. O Rate Limiter poderá ser reintroduzido em camada de borda (gateway) com limites mais adequados ao perfil de carga real, caso necessário.

## Consequências

**Positivo:**
- Código declarativo: os clients Feign parecem interfaces locais, sem boilerplate HTTP
- O Eureka elimina o acoplamento por endereço IP ou URL fixa
- Circuit Breaker e Retry garantem robustez sem afetar a experiência do usuário final (via fallback)
- Resiliência observável via endpoint `/actuator/circuitbreakers` e `/actuator/retries`

**Negativo:**
- Comunicação síncrona mantém acoplamento temporal: lentidão no `contratos-service` impacta o tempo de resposta do `vendas-service` mesmo com retry
- Para operações críticas de negócio, comunicação assíncrona via mensageria seria mais adequada (fora do escopo deste projeto)
