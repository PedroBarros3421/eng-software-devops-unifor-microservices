# Resultados de Performance

**Data da execução:** 2026-05-02  
**Ambiente:** Docker Compose local  
**Comando:** `make perf`

## 1. Objetivo da medição

O teste mede a capacidade do sistema sob carga controlada e produz evidências para:

- avaliar a estabilidade dos microsserviços;
- verificar latência e throughput das operações principais;
- comparar os resultados observados com metas formais de operação;
- documentar SLI, SLO e SLA de forma objetiva.

O tráfego é gerado pelo `api-gateway` nas operações:

- `GET /api/contratos`
- `GET /api/compras/insumos`
- `POST /api/vendas/pedidos`

A distribuição de carga usada nesta execução foi:

- `list_contracts`: 25%
- `list_inventory`: 25%
- `create_sale`: 50%

## 2. Conceitos adotados

### 2.1 Métrica

Métrica é qualquer valor observável produzido pelo sistema ou pela ferramenta de monitoramento.

Exemplos usados neste projeto:

- tempo de resposta individual;
- quantidade total de requisições;
- throughput em `req/s`;
- percentis `P50`, `P95` e `P99`;
- taxa de respostas `5xx`.

Nem toda métrica vira um SLI oficial.

### 2.2 SLI

**SLI (Service Level Indicator)** é uma métrica operacional formalizada, com:

- definição clara;
- fórmula de cálculo;
- unidade;
- escopo;
- janela de observação.

Neste projeto, os SLIs oficiais são:

| SLI | Escopo | Fórmula | Unidade | Janela |
|---|---|---|---|---|
| Disponibilidade técnica | serviço, fluxo ou aplicação | `respostas não-5xx / total de requisições` | `%` | execução do teste |
| Sucesso de negócio | fluxo ou operação de negócio | `respostas 2xx esperadas / total de tentativas válidas` | `%` | execução do teste |
| Latência P95 | serviço, fluxo ou aplicação | percentil 95 do tempo de resposta | `ms` | execução do teste |
| Latência P99 | serviço, fluxo ou aplicação | percentil 99 do tempo de resposta | `ms` | execução do teste |
| Throughput | serviço, fluxo ou aplicação | `total de requisições / duração do teste` | `req/s` | execução do teste |

### 2.3 SLO

**SLO (Service Level Objective)** é a meta definida para um SLI.

Exemplos deste projeto:

- disponibilidade do `vendas-service` >= `99,00%`;
- latência `P95` do fluxo principal <= `500 ms`;
- latência `P99` da aplicação <= `1500 ms`.

### 2.4 SLA

**SLA (Service Level Agreement)** é o compromisso formal assumido com base em um conjunto de SLOs.

Neste trabalho, o SLA foi tratado como **global da aplicação**, e não por microsserviço isolado.  
Os microsserviços possuem SLIs e SLOs próprios; a solução como um todo possui um SLA agregado.

## 3. Estrutura adotada no projeto

Para evitar ambiguidade, a avaliação foi organizada assim:

| Nível | Indicadores | Objetivos |
|---|---|---|
| Microsserviço | SLI + SLO | Medir comportamento operacional de `contratos`, `compras` e `vendas` |
| Fluxo principal | SLI + SLO | Medir o comportamento ponta a ponta do fluxo de venda |
| Aplicação | SLI + SLA | Representar o compromisso global do sistema |

## 4. SLIs por microsserviço

Cada microsserviço foi associado a uma operação representativa:

| Microsserviço | Operação de referência |
|---|---|
| `contratos-service` | `GET /api/contratos` |
| `compras-service` | `GET /api/compras/insumos` |
| `vendas-service` | `POST /api/vendas/pedidos` |

### 4.1 `contratos-service`

| SLI | Valor observado |
|---|---|
| Requests | 4.021 |
| Throughput | 67,02 req/s |
| Disponibilidade técnica | 100,00% |
| Sucesso de negócio | 100,00% |
| Latência P50 | 10,20 ms |
| Latência P95 | 19,76 ms |
| Latência P99 | 26,45 ms |

| SLO | Meta | SLI | Status |
|---|---|---|---|
| Disponibilidade técnica | >= 99,00% | 100,00% | OK |
| Latência P95 | <= 200,00 ms | 19,76 ms | OK |
| Latência P99 | <= 400,00 ms | 26,45 ms | OK |

### 4.2 `compras-service`

| SLI | Valor observado |
|---|---|
| Requests | 3.972 |
| Throughput | 66,20 req/s |
| Disponibilidade técnica | 100,00% |
| Sucesso de negócio | 100,00% |
| Latência P50 | 10,52 ms |
| Latência P95 | 20,47 ms |
| Latência P99 | 27,35 ms |

| SLO | Meta | SLI | Status |
|---|---|---|---|
| Disponibilidade técnica | >= 99,00% | 100,00% | OK |
| Latência P95 | <= 250,00 ms | 20,47 ms | OK |
| Latência P99 | <= 500,00 ms | 27,35 ms | OK |

### 4.3 `vendas-service`

| SLI | Valor observado |
|---|---|
| Requests | 7.911 |
| Throughput | 131,85 req/s |
| Disponibilidade técnica | 100,00% |
| Sucesso de negócio | 100,00% |
| Latência P50 | 78,33 ms |
| Latência P95 | 101,17 ms |
| Latência P99 | 116,28 ms |

| SLO | Meta | SLI | Status |
|---|---|---|---|
| Disponibilidade técnica | >= 99,00% | 100,00% | OK |
| Latência P95 | <= 500,00 ms | 101,17 ms | OK |
| Latência P99 | <= 1000,00 ms | 116,28 ms | OK |

## 5. SLI e SLO do fluxo principal

O fluxo ponta a ponta foi medido usando `POST /api/vendas/pedidos`, pois essa operação:

1. entra pelo `api-gateway`;
2. aciona o `vendas-service`;
3. valida contrato no `contratos-service`;
4. valida e baixa estoque no `compras-service`;
5. persiste a venda.

| SLI do fluxo principal | Valor observado |
|---|---|
| Fluxo | `fluxo_venda_fim_a_fim` |
| Operação medida | `POST /api/vendas/pedidos` |
| Requests | 7.911 |
| Throughput | 131,85 req/s |
| Disponibilidade técnica | 100,00% |
| Sucesso de negócio | 100,00% |
| Latência P50 | 78,33 ms |
| Latência P95 | 101,17 ms |
| Latência P99 | 116,28 ms |

| SLO do fluxo | Meta | SLI | Status |
|---|---|---|---|
| Disponibilidade técnica | >= 99,00% | 100,00% | OK |
| Sucesso de negócio | >= 99,00% | 100,00% | OK |
| Latência P95 | <= 500,00 ms | 101,17 ms | OK |
| Latência P99 | <= 1000,00 ms | 116,28 ms | OK |

## 6. SLI e SLA da aplicação

O SLA foi definido para a aplicação como um todo, usando os indicadores globais agregados da execução.

### 6.1 SLIs globais da aplicação

| SLI da aplicação | Valor observado |
|---|---|
| Total de requisições | 15.904 |
| Throughput médio global | 265,07 req/s |
| Disponibilidade técnica global | 100,00% |
| Sucesso de negócio global | 100,00% |
| Latência P50 global | 29,21 ms |
| Latência P95 global | 94,68 ms |
| Latência P99 global | 109,44 ms |

### 6.2 SLA da aplicação

| SLA da aplicação | Meta | SLI | Status |
|---|---|---|---|
| Disponibilidade técnica global | >= 99,00% | 100,00% | OK |
| Latência P95 global | <= 800,00 ms | 94,68 ms | OK |
| Latência P99 global | <= 1500,00 ms | 109,44 ms | OK |

## 7. Interpretação dos resultados

### 7.1 Leitura operacional

- os serviços de leitura (`contratos-service` e `compras-service`) ficaram com latência baixa e estável;
- o `vendas-service`, que é o serviço mais custoso, permaneceu amplamente abaixo dos seus SLOs;
- o fluxo principal de venda manteve 100% de disponibilidade técnica e 100% de sucesso de negócio;
- o SLA global da aplicação foi atendido com margem.

### 7.2 Conclusão formal

Com base nesta execução:

- todos os **SLIs** definidos foram medidos com fórmula, unidade, escopo e janela claros;
- todos os **SLOs** por microsserviço foram atendidos;
- o **SLO** do fluxo principal foi atendido;
- o **SLA** global da aplicação foi atendido.

## 8. Observações operacionais

Os testes devem ser executados apenas após:

1. `docker compose up --build -d`;
2. todos os containers estarem `healthy`;
3. o Eureka já ter propagado o registro entre os clientes;
4. Prometheus e Grafana já terem iniciado o scrape e carregado os dashboards.

Sem esse aquecimento, podem ocorrer falhas transitórias de discovery ou ausência temporária de métricas, o que não representa o comportamento estável do sistema.

## 9. Evidências geradas

Os artefatos gerados automaticamente pelo runner estão em:

- [`tests/performance/results/summary.json`](../tests/performance/results/summary.json)
- [`tests/performance/results/summary.md`](../tests/performance/results/summary.md)

Esses arquivos consolidam:

- métricas brutas da execução;
- SLIs por microsserviço;
- SLI e SLO do fluxo principal;
- SLIs e SLA da aplicação.
