# Resultados de Performance

**Data da execução:** 2026-04-25  
**Ambiente:** Docker Compose local (WSL2)  
**Comando:** `DURATION_SECONDS=30 WORKERS=8 make perf`

## 1. Estratégia de medição

O teste gera carga mista pelo `api-gateway` com três operações:

- `GET /api/contratos`
- `GET /api/compras/insumos`
- `POST /api/vendas/pedidos`

A distribuição de carga usada nesta execução foi:

- `list_contracts`: 25%
- `list_inventory`: 25%
- `create_sale`: 50%

Essa configuração permite medir:

- **SLIs por microsserviço**, usando operações representativas do domínio
- **SLI/SLO do fluxo principal**, usando `create_sale`
- **SLA da aplicação**, usando os resultados globais agregados

## 2. Definições adotadas

### 2.1 SLI por microsserviço

Cada microsserviço foi associado a uma operação de referência:

| Microsserviço | Operação de referência |
|---|---|
| `contratos-service` | `GET /api/contratos` |
| `compras-service` | `GET /api/compras/insumos` |
| `vendas-service` | `POST /api/vendas/pedidos` |

### 2.2 SLI/SLO do fluxo principal

O fluxo principal é:

1. cliente chama `POST /api/vendas/pedidos`
2. `vendas-service` valida contrato no `contratos-service`
3. `vendas-service` valida disponibilidade no `compras-service`
4. `vendas-service` baixa estoque no `compras-service`
5. `vendas-service` persiste a venda

### 2.3 SLA da aplicação

O **SLA** foi tratado como um compromisso global da solução, usando:

- disponibilidade global
- P95 global
- P99 global

## 3. SLA da aplicação

### 3.1 SLIs globais observados

| Métrica | Valor |
|---|---|
| Total de requisições | 7.335 |
| Throughput médio global | **244,50 req/s** |
| Disponibilidade global | **100,00%** |
| Sucesso de negócio global | **100,00%** |
| P50 global | **31,25 ms** |
| P95 global | **61,97 ms** |
| P99 global | **83,16 ms** |

### 3.2 Metas de SLA

| SLA da aplicação | Meta | Resultado | Status |
|---|---|---|---|
| Disponibilidade global | >= 95,00% | 100,00% | OK |
| P95 global | <= 800,00 ms | 61,97 ms | OK |
| P99 global | <= 1500,00 ms | 83,16 ms | OK |

## 4. SLI e SLO por microsserviço

### 4.1 `contratos-service`

| Métrica | Valor |
|---|---|
| Operação de referência | `GET /api/contratos` |
| Requests | 1.849 |
| Throughput | 61,63 req/s |
| Disponibilidade | 100,00% |
| Sucesso de negócio | 100,00% |
| P50 | 14,71 ms |
| P95 | 25,98 ms |
| P99 | 35,69 ms |

| SLO | Meta | Resultado | Status |
|---|---|---|---|
| Disponibilidade | >= 99,00% | 100,00% | OK |
| P95 | <= 200,00 ms | 25,98 ms | OK |
| P99 | <= 400,00 ms | 35,69 ms | OK |

### 4.2 `compras-service`

| Métrica | Valor |
|---|---|
| Operação de referência | `GET /api/compras/insumos` |
| Requests | 1.842 |
| Throughput | 61,40 req/s |
| Disponibilidade | 100,00% |
| Sucesso de negócio | 100,00% |
| P50 | 13,76 ms |
| P95 | 23,82 ms |
| P99 | 29,07 ms |

| SLO | Meta | Resultado | Status |
|---|---|---|---|
| Disponibilidade | >= 99,00% | 100,00% | OK |
| P95 | <= 250,00 ms | 23,82 ms | OK |
| P99 | <= 500,00 ms | 29,07 ms | OK |

### 4.3 `vendas-service`

| Métrica | Valor |
|---|---|
| Operação de referência | `POST /api/vendas/pedidos` |
| Requests | 3.644 |
| Throughput | 121,47 req/s |
| Disponibilidade | 100,00% |
| Sucesso de negócio | 100,00% |
| P50 | 46,10 ms |
| P95 | 70,21 ms |
| P99 | 89,15 ms |

| SLO | Meta | Resultado | Status |
|---|---|---|---|
| Disponibilidade | >= 99,00% | 100,00% | OK |
| P95 | <= 500,00 ms | 70,21 ms | OK |
| P99 | <= 1000,00 ms | 89,15 ms | OK |

## 5. SLI e SLO do fluxo principal

O fluxo ponta a ponta foi medido usando `POST /api/vendas/pedidos`, pois essa operação atravessa os três microsserviços de negócio.

| Métrica | Valor |
|---|---|
| Fluxo | `fluxo_venda_fim_a_fim` |
| Operação medida | `POST /api/vendas/pedidos` |
| Requests | 3.644 |
| Throughput | 121,47 req/s |
| Disponibilidade | 100,00% |
| Sucesso de negócio | 100,00% |
| P50 | 46,10 ms |
| P95 | 70,21 ms |
| P99 | 89,15 ms |

| SLO do fluxo | Meta | Resultado | Status |
|---|---|---|---|
| Disponibilidade | >= 99,00% | 100,00% | OK |
| Sucesso de negócio | >= 99,00% | 100,00% | OK |
| P95 | <= 500,00 ms | 70,21 ms | OK |
| P99 | <= 1000,00 ms | 89,15 ms | OK |

## 6. Interpretação

### 6.1 Leitura arquitetural

Os resultados mostram três pontos relevantes:

- os serviços de leitura (`contratos-service` e `compras-service`) ficaram com latências baixas e estáveis
- o `vendas-service`, mesmo sendo o mais complexo, ficou amplamente abaixo do SLO definido
- o fluxo fim a fim manteve 100% de disponibilidade e 100% de sucesso de negócio durante a execução

### 6.2 Conclusão sobre aderência

Com base nesta execução:

- **todos os SLOs por microsserviço foram atendidos**
- **o SLO do fluxo principal foi atendido**
- **o SLA global da aplicação foi atendido**

Mesmo no endpoint mais custoso (`POST /api/vendas/pedidos`), o P95 ficou em **70,21 ms**, bem abaixo da meta de **500 ms**, e o P99 ficou em **89,15 ms**, também bem abaixo da meta de **1000 ms**.

### 6.3 Observação operacional

Os testes devem ser executados apenas após:

1. `docker compose up --build -d`
2. todos os containers estarem `healthy`
3. o cache do Eureka já ter propagado entre os clientes

Sem esse aquecimento inicial, podem ocorrer falhas transitórias de discovery, que não representam o comportamento estável do sistema.

## 7. Evidências geradas

Os artefatos gerados automaticamente pelo runner estão em:

- [`tests/performance/results/summary.json`](../tests/performance/results/summary.json)
- [`tests/performance/results/summary.md`](../tests/performance/results/summary.md)

Esses arquivos consolidam:

- percentis por operação
- SLI por microsserviço
- SLI/SLO do fluxo principal
- SLA da aplicação
