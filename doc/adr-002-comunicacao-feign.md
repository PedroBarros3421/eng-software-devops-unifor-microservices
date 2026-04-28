# ADR-002: Comunicação entre Serviços

## Contexto

O `vendas-service` precisa consultar dados do `contratos-service` para validar contratos vinculados a pedidos de venda e do `compras-service` para verificar e baixar estoque. Foi necessário definir:

1. O protocolo de comunicação entre microsserviços

## Alternativas Consideradas

| Alternativa | Prós | Contras |
|---|---|---|
| RestTemplate manual | Sem dependência adicional | Boilerplate HTTP, sem integração nativa com Eureka/LoadBalancer |
| OpenFeign (escolhido) | Declarativo, integração nativa com Eureka e Resilience4j, código limpo | Comunicação síncrona mantém acoplamento temporal |
| Mensageria assíncrona (Kafka/RabbitMQ) | Desacoplamento temporal, tolerância a falhas | Complexidade de implementação fora do escopo do projeto |
| gRPC | Alta performance, contrato forte | Curva de aprendizado, incompatível com infraestrutura HTTP atual |

## Decisão

Adotar **OpenFeign** para comunicação síncrona HTTP, integrado ao **Eureka** para resolução de endereços:

### Comunicação: OpenFeign + Eureka

- O `ContratosClient` usa `@FeignClient(name = "contratos-service")` e o `ComprasClient` usa `@FeignClient(name = "compras-service")`, com nomes resolvidos via Eureka (sem URLs hardcoded)
- O Spring Cloud LoadBalancer distribui a carga caso existam múltiplas instâncias

## Nota de revisão (2026-04-25)

A versão anterior desta ADR documentava um **Rate Limiter de 10 req/s** no endpoint `POST /api/vendas/pedidos`. Durante os testes de performance, identificou-se que esse limite causava rejeições artificiais (HTTP 429/500) com apenas 8 workers concorrentes, distorcendo as métricas de disponibilidade.

Após análise, o Rate Limiter foi **removido do endpoint de criação de pedidos**. O controle de sobrecarga permanece via Circuit Breaker nas chamadas downstream e via recursos do API Gateway. O Rate Limiter poderá ser reintroduzido em camada de borda (gateway) com limites mais adequados ao perfil de carga real, caso necessário.

## Consequências

**Positivo:**
- Código declarativo: os clients Feign parecem interfaces locais, sem boilerplate HTTP
- O Eureka elimina o acoplamento por endereço IP ou URL fixa
- Circuit Breaker e Retry garantem robustez sem afetar a experiência do usuário final (via fallback)

**Negativo:**
- Comunicação síncrona mantém acoplamento temporal: lentidão no `contratos-service` impacta o tempo de resposta do `vendas-service` mesmo com retry
- Para operações críticas de negócio, comunicação assíncrona via mensageria seria mais adequada (fora do escopo deste projeto)

## Histórico de Revisões

| Campo | Valor |
|---|---|
| Sistema | Sistema de Gestão Comercial — Microsserviços |
| Autores | Edval Júnior, Iago Barbosa, Mary Santos, Pedro Barros, Victor Kauan |
| Revisores | Equipe do grupo (revisão após testes de performance) |
| Supersede | — |
| Supersedido por | — |

| Versão | Data | Autor | Alteração |
|---|---|---|---|
| 1.0 | 2026-04-12 | Equipe | Criação inicial — incluía Rate Limiter de 10 req/s no endpoint `POST /api/vendas/pedidos` |
| 1.1 | 2026-04-25 | Equipe | Revisão após peer review: remoção do Rate Limiter do endpoint; adicionada seção de alternativas e documentação do impacto nos testes de performance |

**Status atual:** Aceito
