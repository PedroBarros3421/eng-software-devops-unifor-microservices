# Sistema de Gestão Comercial — Microsserviços

Projeto integrador da disciplina **Arquitetura de Microsserviços e Escalabilidade**

---

## Subindo o ambiente

```bash
docker compose up --build -d
```

Aguardar todos os containers ficarem `healthy` (~60–90s). Verificar:

```bash
docker ps
```

Todos devem exibir `(healthy)` ou estar em execução estável.

---

## Serviços e portas

| Serviço | Porta | URL |
|---|---|---|
| API Gateway | 8080 | http://localhost:8080 |
| Eureka Server | 8761 | http://localhost:8761 |
| contratos-service | 8081 | interno (via gateway) |
| compras-service | 8082 | interno (via gateway) |
| vendas-service | 8083 | interno (via gateway) |
| Grafana | 3000 | http://localhost:3000 |
| Jaeger | 16686 | http://localhost:16686 |
| Prometheus | 9090 | http://localhost:9090 |

> Todo tráfego externo passa pelo gateway na porta **8080**.

---

## Demonstração ao vivo

### Pré-requisitos dos cenários

**Criar um contrato:**
```bash
curl -s -X POST http://localhost:8080/api/contratos \
  -H "Content-Type: application/json" \
  -d '{
    "fornecedor": "Fornecedor Demo",
    "descricao": "Contrato de fornecimento",
    "dataInicio": "2026-01-01",
    "dataFim": "2026-12-31",
    "status": "ATIVO"
  }' | jq .
```

**Criar um insumo com estoque:**
```bash
curl -s -X POST http://localhost:8080/api/compras/insumos \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Insumo Demo",
    "descricao": "Material de demonstração",
    "unidade": "UN",
    "precoUnitario": 10.00,
    "quantidadeEstoque": 100
  }' | jq .
```

> Anote os `id` retornados — serão usados nos cenários abaixo.

---

### Cenário 1 — Sucesso

Criar uma venda com contrato ativo e estoque disponível:

```bash
curl -s -X POST http://localhost:8080/api/vendas/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "contratoId": 1,
    "itens": [{ "insumoId": 1, "quantidade": 2, "precoUnitario": 10.00 }]
  }' | jq .
```

**Resultado esperado:** `201 Created` com o pedido persistido.

O que isso demonstra:
- contrato validado no `contratos-service`
- estoque verificado e baixado no `compras-service`
- venda persistida no `vendas-service`
- `X-Correlation-ID` propagado por todos os serviços

---

### Cenário 2 — Falha comercial (contrato inválido)

Alterar o contrato para status inativo:

```bash
curl -s -X PUT http://localhost:8080/api/contratos/1 \
  -H "Content-Type: application/json" \
  -d '{
    "fornecedor": "Fornecedor Demo",
    "descricao": "Contrato de fornecimento",
    "dataInicio": "2026-01-01",
    "dataFim": "2026-12-31",
    "status": "INATIVO"
  }' | jq .
```

Tentar criar a venda novamente:

```bash
curl -s -X POST http://localhost:8080/api/vendas/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "contratoId": 1,
    "itens": [{ "insumoId": 1, "quantidade": 2, "precoUnitario": 10.00 }]
  }' | jq .
```

**Resultado esperado:** `422 Unprocessable Entity` — contrato inativo rejeitado pelo `vendas-service`.

---

### Cenário 3 — Falha operacional (estoque insuficiente)

Reativar o contrato e tentar vender mais do que o estoque disponível:

```bash
# Reativar contrato
curl -s -X PUT http://localhost:8080/api/contratos/1 \
  -H "Content-Type: application/json" \
  -d '{
    "fornecedor": "Fornecedor Demo",
    "descricao": "Contrato de fornecimento",
    "dataInicio": "2026-01-01",
    "dataFim": "2026-12-31",
    "status": "ATIVO"
  }' | jq .

# Tentar venda com quantidade maior que o estoque
curl -s -X POST http://localhost:8080/api/vendas/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "contratoId": 1,
    "itens": [{ "insumoId": 1, "quantidade": 99999, "precoUnitario": 10.00 }]
  }' | jq .
```

**Resultado esperado:** `422 Unprocessable Entity` — estoque insuficiente no `compras-service`.

---

## Observabilidade

### Rastrear uma requisição no Jaeger

1. Acesse http://localhost:16686
2. Selecione o serviço `vendas-service`
3. Clique em **Find Traces**
4. Abra um trace do `POST /api/vendas/pedidos` e veja os spans de cada serviço

Ou busque pelo `X-Correlation-ID` retornado no header da resposta:

```bash
curl -si -X POST http://localhost:8080/api/vendas/pedidos \
  -H "Content-Type: application/json" \
  -d '{ "contratoId": 1, "itens": [{ "insumoId": 1, "quantidade": 1, "precoUnitario": 10.00 }] }' \
  | grep -i "x-correlation"
```

### Logs no Grafana (Loki)

1. Acesse http://localhost:3000
2. Vá em **Explore** → datasource **Loki**
3. Use a query: `{container=~"vendas-service|contratos-service|compras-service"}`
4. Filtre pelo `correlationId` de uma requisição específica

### Métricas no Prometheus / Grafana

- Prometheus direto: http://localhost:9090
- Grafana dashboards: http://localhost:3000
- Métricas disponíveis: latência, throughput, JVM, Resilience4j (circuit breaker, retry)

### Health checks

```bash
curl http://localhost:8080/health          # api-gateway
curl http://localhost:8761/actuator/health # eureka
```

---

## Testes

### Unitários

```bash
make test
```

### Performance (30s, 8 workers)

```bash
DURATION_SECONDS=30 WORKERS=8 make perf
```

**Referência:** 370 req/s | P95: 57ms | P99: 74ms | Disponibilidade: 100% — todos os SLOs atingidos.

Resultados documentados em [`doc/performance.md`](doc/performance.md).

> Aguardar ~60s após o `docker compose up` antes de rodar o teste (propagação do cache do Eureka).

---

## Documentação

| Documento | Descrição |
|---|---|
| [`doc/arquitetura.md`](doc/arquitetura.md) | Documento de arquitetura consolidado |
| [`doc/adr-001-escolha-banco.md`](doc/adr-001-escolha-banco.md) | ADR-001: Escolha do banco de dados por serviço |
| [`doc/adr-002-comunicacao-feign.md`](doc/adr-002-comunicacao-feign.md) | ADR-002: Comunicação via OpenFeign + Resilience4j |
| [`doc/adr-003-observabilidade.md`](doc/adr-003-observabilidade.md) | ADR-003: Estratégia de observabilidade |
| [`doc/performance.md`](doc/performance.md) | Resultados de performance e aderência aos SLOs |
| [`doc/peer-review-checklist.md`](doc/peer-review-checklist.md) | Checklist de Peer Review |
