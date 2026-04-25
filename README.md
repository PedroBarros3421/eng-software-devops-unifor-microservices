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
| compras-service | 8081 | interno (via gateway) |
| contratos-service | 8082 | interno (via gateway) |
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
CONTRATO_ID=$(curl -s -X POST http://localhost:8080/api/contratos \
  -H "Content-Type: application/json" \
  -d '{
    "numero": "CTR-DEMO-README",
    "nomeContratante": "Cliente Demo",
    "valorTotal": 1000.00,
    "dataInicio": "2026-01-01",
    "dataFim": "2026-12-31",
    "status": "ATIVO",
    "termos": "Contrato para demonstracao"
  }' | jq -r '.id')

echo "$CONTRATO_ID"
```

Payload inválido deve retornar `400 Bad Request`.

**Criar um insumo com estoque:**
```bash
INSUMO_ID=$(curl -s -X POST http://localhost:8080/api/compras/insumos \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Insumo Demo",
    "descricao": "Material de demonstracao",
    "unidadeMedida": "UN",
    "precoUnitario": 10.00,
    "quantidadeEstoque": 100
  }' | jq -r '.id')

echo "$INSUMO_ID"
```

Payload inválido deve retornar `400 Bad Request`.

> Os comandos acima armazenam os `id` em `CONTRATO_ID` e `INSUMO_ID`.

---

### Cenário 1 — Sucesso

Criar uma venda com contrato ativo e estoque disponível:

```bash
curl -s -X POST http://localhost:8080/api/vendas/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "nomeCliente": "Cliente Demo",
    "contratoId": '"$CONTRATO_ID"',
    "itens": [{
      "insumoId": '"$INSUMO_ID"',
      "nomeInsumo": "Insumo Demo",
      "quantidade": 2,
      "precoUnitario": 10.00
    }]
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

Alterar o contrato para status suspenso:

```bash
curl -s -X PATCH "http://localhost:8080/api/contratos/$CONTRATO_ID/status?status=SUSPENSO" | jq .
```

Confirmar a invalidação do contrato:

```bash
curl -s "http://localhost:8080/api/contratos/$CONTRATO_ID/validacao" | jq .
```

Tentar criar a venda novamente:

```bash
curl -i -X POST http://localhost:8080/api/vendas/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "nomeCliente": "Cliente Demo",
    "contratoId": '"$CONTRATO_ID"',
    "itens": [{
      "insumoId": '"$INSUMO_ID"',
      "nomeInsumo": "Insumo Demo",
      "quantidade": 2,
      "precoUnitario": 10.00
    }]
  }'
```

**Resultado esperado:** `422 Unprocessable Entity` porque o contrato não está mais elegível para uso.

---

### Cenário 3 — Falha operacional (estoque insuficiente)

Reativar o contrato e tentar vender mais do que o estoque disponível:

```bash
# Reativar contrato
curl -s -X PATCH "http://localhost:8080/api/contratos/$CONTRATO_ID/status?status=ATIVO" | jq .

# Confirmar indisponibilidade do estoque para a quantidade desejada
curl -s "http://localhost:8080/api/compras/insumos/$INSUMO_ID/disponibilidade?quantidade=99999" | jq .

# Tentar venda com quantidade maior que o estoque
curl -i -X POST http://localhost:8080/api/vendas/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "nomeCliente": "Cliente Demo",
    "contratoId": '"$CONTRATO_ID"',
    "itens": [{
      "insumoId": '"$INSUMO_ID"',
      "nomeInsumo": "Insumo Demo",
      "quantidade": 99999,
      "precoUnitario": 10.00
    }]
  }'
```

**Resultado esperado:** `422 Unprocessable Entity` por indisponibilidade de estoque.

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
  -d '{ "nomeCliente": "Cliente Demo", "contratoId": '"$CONTRATO_ID"', "itens": [{ "insumoId": '"$INSUMO_ID"', "nomeInsumo": "Insumo Demo", "quantidade": 1, "precoUnitario": 10.00 }] }' \
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
curl http://localhost:8761/health          # eureka
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

**Referência:** use os números consolidados em [`doc/performance.md`](doc/performance.md), que refletem a execução validada mais recente.

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
