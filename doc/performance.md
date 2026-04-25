# Resultados de Performance — Evidência Final

**Data da execução:** 2026-04-25  
**Ambiente:** Docker Compose local (WSL2)  
**Configuração do teste:** `DURATION_SECONDS=30 WORKERS=8 make perf`

---

## 1. Configuração do Teste

| Parâmetro | Valor |
|---|---|
| Duração | 30 segundos |
| Workers concorrentes | 8 |
| URL base | `http://localhost:8080` (via API Gateway) |
| Distribuição de carga | `list_contracts` 25%, `list_inventory` 25%, `create_sale` 50% |

O teste simula carga mista com ênfase no fluxo principal de negócio (`create_sale`), que atravessa gateway → vendas-service → contratos-service → compras-service.

### Pré-requisito de execução

Após `docker compose up --build -d`, é necessário aguardar a propagação completa do cache do Eureka em todos os clientes (~60–90s após startup) antes de iniciar o teste. Isso garante que o `vendas-service` consiga resolver `contratos-service` e `compras-service` via service discovery sem erros de "no instance found".

Sequência recomendada:
```bash
docker compose up --build -d
# aguardar todos os containers ficarem healthy
# aguardar ~60s para propagação do cache Eureka
make perf
```

---

## 2. Resultados Globais

| Métrica | Valor |
|---|---|
| Total de requisições | 11.114 |
| Throughput médio | **370,47 req/s** |
| Erros técnicos | 0 |
| Disponibilidade técnica (SLI) | **100,00%** |
| Sucesso de negócio | **100,00%** |
| P50 global | ~22 ms |
| P95 global | ~48 ms |
| P99 global | ~65 ms |

---

## 3. Resultados por Operação

### `create_sale` — POST /api/vendas/pedidos (via gateway)

Este é o endpoint principal de negócio. Envolve 4 etapas:
1. Validação de contrato (Feign → contratos-service)
2. Verificação de estoque (Feign → compras-service)
3. Baixa de estoque (Feign → compras-service)
4. Persistência da venda (PostgreSQL local)

| Métrica | Valor |
|---|---|
| Requisições | 5.571 |
| Erros | 0 |
| Status | 201: 5.571 (100%) |
| P50 | **33,79 ms** |
| P95 | **56,89 ms** |
| P99 | **74,28 ms** |

### `list_contracts` — GET /api/contratos (via gateway)

| Métrica | Valor |
|---|---|
| Requisições | 2.734 |
| Erros | 0 |
| Status | 200: 2.734 (100%) |
| P50 | 6,17 ms |
| P95 | 13,95 ms |
| P99 | 20,33 ms |

### `list_inventory` — GET /api/compras/insumos (via gateway)

| Métrica | Valor |
|---|---|
| Requisições | 2.809 |
| Erros | 0 |
| Status | 200: 2.809 (100%) |
| P50 | 5,80 ms |
| P95 | 13,06 ms |
| P99 | 18,43 ms |

---

## 4. Aderência aos SLOs

| SLO / SLA | Meta | Resultado (`create_sale`) | Status |
|---|---|---|---|
| SLO — Disponibilidade ≥ 99% | 99,00% | 100,00% | ✓ OK |
| SLO — P95 ≤ 500 ms | 500,00 ms | 56,89 ms | ✓ OK |
| SLO — P99 ≤ 1000 ms | 1000,00 ms | 74,28 ms | ✓ OK |
| SLA — Disponibilidade ≥ 95% | 95,00% | 100,00% | ✓ OK |
| SLA — P95 ≤ 800 ms | 800,00 ms | 56,89 ms | ✓ OK |
| SLA — P99 ≤ 1500 ms | 1500,00 ms | 74,28 ms | ✓ OK |

**Todos os SLOs e SLAs foram atingidos com ampla margem.**

---

## 5. Análise e Conclusão

### 5.1 Desempenho observado

O sistema apresentou desempenho sólido sob a carga do teste:

- **370,47 req/s** de throughput global demonstra capacidade adequada para carga acadêmica e prova de conceito
- O endpoint mais complexo (`create_sale`, que envolve 3 chamadas Feign + 1 escrita no banco) respondeu com P95 de **56,89 ms** — aproximadamente **8,8× abaixo da meta SLO de 500 ms**
- O P99 de **74,28 ms** ficou **13,5× abaixo da meta SLO de 1000 ms**, indicando que picos de latência são raros e controlados
- **Zero erros** em 11.114 requisições (5.571 de `create_sale`), com disponibilidade técnica e de negócio de 100%

### 5.2 Observação sobre cold start e Eureka

Em um teste realizado imediatamente após o startup dos containers (sem aguardar a propagação do cache do Eureka), foram observados ~35 erros 500 nos primeiros segundos — todos por `"Load balancer does not contain an instance for the service contratos-service"`. Isso é comportamento normal da inicialização do Eureka: o servidor registra os serviços, mas os clientes Feign precisam de ~30–60s para atualizar seu cache local.

Os resultados desta seção foram obtidos com cache Eureka já propagado, representando o comportamento em regime estável (steady state), que é o cenário relevante para avaliação de SLO.

### 5.3 Observações sobre o ambiente

- O teste foi executado em ambiente local (WSL2), onde todos os serviços e o cliente de teste compartilham os mesmos recursos de CPU e memória da máquina host
- Em produção, com serviços isolados em containers dedicados, esperaríamos latências ainda menores para as chamadas Feign
- A JVM já estava aquecida no momento do teste, o que contribui para a estabilidade das latências

### 5.4 Recomendações para produção

- Reduzir a probabilidade de amostragem do tracing de 100% para 10–20% para reduzir overhead em alta carga
- O Rate Limiter removido do `POST /api/vendas/pedidos` poderá ser reintroduzido no API Gateway com limite adequado ao perfil de carga real
- O banco compartilhado (única instância PostgreSQL com 3 databases) é o principal ponto de contenção; em produção, separar em instâncias dedicadas por serviço

---

## 6. Arquivo de Evidência

O arquivo JSON completo com todos os dados do teste está em:

```
tests/performance/results/summary.json
```

Gerado em: `2026-04-25` (startup limpo + cache Eureka propagado)
