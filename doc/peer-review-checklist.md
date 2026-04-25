# Checklist de Peer Review

**Projeto:** Sistema de Gestão Comercial — Microsserviços  
**Repositório:** eng-software-devops-unifor-microservices  
**Revisão por:** ___________________________  
**Data da revisão:** ___________________________  
**Branch revisado:** ___________________________

---

## Instruções

Este checklist deve ser preenchido pelo revisor durante a sessão de peer review. Para cada item, marque com:
- `[x]` — Conforme / Implementado corretamente
- `[ ]` — Não conforme / Ausente
- `[~]` — Parcialmente implementado / Necessita melhoria
- `[N/A]` — Não aplicável ao escopo do projeto

Ao final, registre os feedbacks e indique quais foram incorporados ao projeto.

---

## 1. Arquitetura e Estrutura

- [ ] Os serviços estão adequadamente separados por domínio (Bounded Contexts)
- [ ] Cada serviço tem seu próprio banco de dados (Database per Service)
- [ ] Não há acoplamento direto entre bancos de dados de serviços diferentes
- [ ] O API Gateway é o único ponto de entrada para clientes externos
- [ ] O service discovery (Eureka) está sendo usado corretamente (sem URLs hardcoded)
- [ ] A comunicação entre serviços usa apenas APIs públicas (sem chamadas diretas ao banco alheio)

**Observações:**

```
(espaço para notas do revisor)
```

---

## 2. ADRs (Architecture Decision Records)

- [ ] Existem no mínimo 3 ADRs documentados
- [ ] Cada ADR segue o template: contexto, alternativas consideradas, decisão, consequências
- [ ] Os ADRs refletem o estado atual do código (sem referências a tecnologias não usadas)
- [ ] Pelo menos um ADR registra revisão após peer review
- [ ] As decisões nos ADRs são justificadas com base em trade-offs reais

**Observações:**

```
(espaço para notas do revisor)
```

---

## 3. Observabilidade

- [ ] Os três pilares estão implementados: logs, métricas e traces
- [ ] O `X-Correlation-ID` é gerado pelo gateway e propagado para todos os serviços
- [ ] Os logs incluem `correlationId`, `traceId` e `spanId`
- [ ] Prometheus coleta métricas de todos os serviços via `/actuator/prometheus`
- [ ] O Jaeger recebe traces de todos os serviços via OTLP
- [ ] O Grafana está configurado com datasources para Prometheus, Loki e Jaeger
- [ ] É possível rastrear uma requisição end-to-end usando o `correlationId` ou `traceId`

**Observações:**

```
(espaço para notas do revisor)
```

---

## 4. Health Checks

- [ ] Todos os serviços expõem endpoint `/health` (ou `/actuator/health`)
- [ ] O `docker-compose.yml` define `healthcheck` para cada serviço
- [ ] As dependências de inicialização respeitam a ordem via `depends_on: condition: service_healthy`
- [ ] O health check inclui indicadores de banco de dados e dependências críticas

**Observações:**

```
(espaço para notas do revisor)
```

---

## 5. Resiliência

- [ ] O Circuit Breaker está implementado nas chamadas inter-serviço críticas
- [ ] O Retry com backoff está configurado para falhas transientes
- [ ] Existe Fallback definido para evitar falhas em cascata
- [ ] O estado do Circuit Breaker é observável via Actuator ou Grafana
- [ ] O comportamento de falha foi testado (contrato inválido, estoque insuficiente)

**Observações:**

```
(espaço para notas do revisor)
```

---

## 6. Performance

- [ ] Existe um teste de performance automatizado
- [ ] Os resultados incluem P50, P95, P99 e throughput
- [ ] Os SLOs estão definidos e documentados
- [ ] O sistema atinge os SLOs definidos no teste de performance
- [ ] Os resultados do teste estão documentados como evidência

**Observações:**

```
(espaço para notas do revisor)
```

---

## 7. Qualidade de Código e Testes

- [ ] Existem testes unitários nos serviços
- [ ] Os testes cobrem os cenários principais de negócio (sucesso, contrato inválido, estoque insuficiente)
- [ ] O código segue uma estrutura consistente entre os serviços
- [ ] Não há credenciais ou configurações sensíveis hardcoded no código
- [ ] As variáveis de ambiente têm valores default razoáveis para desenvolvimento

**Observações:**

```
(espaço para notas do revisor)
```

---

## 8. Docker e Infraestrutura

- [ ] O `docker-compose.yml` sobe todo o sistema com um único comando (`docker compose up --build -d`)
- [ ] Todos os serviços de negócio e infraestrutura estão incluídos
- [ ] As variáveis de ambiente estão corretamente configuradas
- [ ] Os volumes persistem dados entre reinicializações
- [ ] As redes estão configuradas para isolamento adequado

**Observações:**

```
(espaço para notas do revisor)
```

---

## 9. Fluxo Principal de Negócio

- [ ] O fluxo `POST /api/vendas/pedidos` funciona de ponta a ponta
- [ ] O cenário de sucesso gera o status correto (201 Created)
- [ ] O cenário de contrato inválido retorna erro adequado (422)
- [ ] O cenário de estoque insuficiente retorna erro adequado (422)
- [ ] O X-Correlation-ID aparece nos logs de todos os serviços envolvidos

**Observações:**

```
(espaço para notas do revisor)
```

---

## 10. Resumo do Peer Review

### 10.1 Pontos Fortes

```
(liste os aspectos mais bem implementados)

1. 
2. 
3. 
```

### 10.2 Pontos de Melhoria Identificados

```
(liste o que poderia ser melhorado — separe crítico de sugestão)

Críticos:
1. 
2. 

Sugestões:
1. 
2. 
```

### 10.3 Feedbacks Incorporados ao Projeto

Marque quais feedbacks desta revisão foram efetivamente incorporados ao código ou documentação:

| Feedback | Incorporado? | Como foi tratado |
|---|---|---|
| | [ ] Sim / [ ] Não | |
| | [ ] Sim / [ ] Não | |
| | [ ] Sim / [ ] Não | |

> Os feedbacks incorporados devem ser registrados nos ADRs correspondentes quando impactarem decisões arquiteturais (ver ADR-001 e ADR-002 como exemplo).

### 10.4 Avaliação Geral

- [ ] Aprovado sem ressalvas
- [ ] Aprovado com sugestões menores
- [ ] Requer ajustes antes da entrega
- [ ] Necessita revisão significativa

**Comentário final:**

```
(síntese da avaliação pelo revisor)
```

---

**Assinatura do Revisor:** ___________________________  
**Data de conclusão da revisão:** ___________________________
