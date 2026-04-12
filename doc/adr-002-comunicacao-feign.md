# ADR-002: Comunicação entre Serviços via OpenFeign com Resilience4j

**Status:** Aceito
**Data:** 2026-04-12

## Contexto

O `vendas-service` precisa consultar dados do `contratos-service` para validar contratos vinculados a pedidos de venda. Foi necessário definir:

1. O protocolo de comunicação entre microsserviços
2. A estratégia de resiliência para lidar com falhas temporárias ou indisponibilidade

## Decisão

Adotar **OpenFeign** para comunicação síncrona HTTP, integrado ao **Eureka** para resolução de endereços, e **Resilience4j** para resiliência em camadas:

### Comunicação: OpenFeign + Eureka
- O `ContratosClient` usa `@FeignClient(name = "contratos-service")` onde o nome é resolvido via Eureka (sem URLs hardcoded)
- O Spring Cloud LoadBalancer distribui a carga caso existam múltiplas instâncias do `contratos-service`

### Resiliência em camadas (vendas-service → contratos-service)

| Padrão | Configuração | Comportamento |
|---|---|---|
| **Retry** | 3 tentativas, backoff exponencial (500ms × 2) | Reenvio automático em falhas transitórias de rede |
| **Circuit Breaker** | 50% de falhas em janela de 10 chamadas abre o disjuntor por 10s | Interrompe chamadas a serviços instáveis, evitando cascata de falhas |
| **Fallback** | Retorna `ContratoDTO` com `status = "INDISPONIVEL"` | Resposta degradada mas funcional quando o serviço está fora do ar |
| **Rate Limiter** | 10 req/s no endpoint `POST /api/vendas/pedidos` | Protege o serviço de picos de tráfego |

### Fluxo de chamada com resiliência

```
VendasController → VendasService.consultarContrato()
                        │
                   @Retry (3x com backoff exponencial)
                        │
                   @CircuitBreaker (CLOSED → OPEN → HALF-OPEN)
                        │
                   ContratosClient (Feign + Eureka)
                        │
                   contratos-service /api/contratos/{id}
```

## Consequências

**Positivo:**
- Código declarativo: o `ContratosClient` parece uma interface local, sem boilerplate HTTP
- O Eureka elimina o acoplamento por endereço IP ou URL fixa
- As 4 camadas de resiliência garantem robustez sem afetar a experiência do usuário final (via fallback)

**Negativo:**
- Comunicação síncrona introduz acoplamento temporal: se o `contratos-service` estiver lento, impacta o tempo de resposta do `vendas-service` mesmo com retry
- Para operações críticas de negócio, comunicação assíncrona via mensageria (Kafka/RabbitMQ) seria mais adequada (fora do escopo deste projeto)
