# ADR-001: Escolha dos Bancos de Dados por Serviço

**Status:** Aceito (revisado após Peer Review em 2026-04-25)
**Data:** 2026-04-12
**Revisão:** 2026-04-25

## Contexto

O sistema é composto por três microsserviços de negócio com domínios distintos:

- **compras-service**: gerencia insumos e pedidos a fornecedores (dados altamente relacionais com integridade transacional)
- **contratos-service**: gerencia contratos com vigência, termos e status (dados estruturados com esquema fixo)
- **vendas-service**: processa pedidos de clientes com itens, quantidades e valores — necessita de integridade transacional para garantir consistência entre baixa de estoque e registro da venda

O padrão **Database per Service** exige que cada serviço tenha sua própria base de dados para garantir independência de deploy e evitar acoplamento forte.

## Alternativas Consideradas

| Alternativa | Prós | Contras |
|---|---|---|
| PostgreSQL para todos | Operação unificada, familiaridade da equipe, ACID em todos os serviços | Menos expressivo para domínios com esquema variável |
| MongoDB para vendas-service | Flexibilidade de esquema para itens de pedido | Dificulta integridade referencial com contratos e insumos, curva de aprendizado |
| PostgreSQL para todos (escolhido) | Garantia ACID para fluxo de venda que envolve baixa de estoque, operação unificada | Ligeiramente menos flexível para dados semi-estruturados |

## Decisão

Adotar **PostgreSQL como banco único** para os três serviços de negócio, com isolamento lógico por banco de dados separado por serviço:

| Serviço | Banco | Database | Justificativa |
|---|---|---|---|
| compras-service | PostgreSQL | `compras_db` | Dados relacionais, integridade transacional, consultas com JOIN entre insumos e pedidos |
| contratos-service | PostgreSQL | `contratos_db` | Esquema fixo e estruturado, consultas por vigência e status, auditoria |
| vendas-service | PostgreSQL | `vendas_db` | Integridade transacional necessária para garantir consistência entre registro da venda e dados relacionados |

> **Nota de revisão (Peer Review 2026-04-25):** A decisão inicial previa MongoDB para o `vendas-service` como demonstração de poliglotismo. Após peer review, foi identificado que o uso de MongoDB no escopo atual traria complexidade operacional sem benefício real de flexibilidade de esquema, uma vez que o modelo de Venda é estruturado e o fluxo exige consistência transacional. A decisão foi revisada para PostgreSQL, simplificando a infraestrutura e alinhando a persistência ao padrão transacional exigido.

Em ambiente de desenvolvimento, os três bancos rodam na mesma instância PostgreSQL (isolamento por database). Em produção, cada serviço teria sua própria instância.

## Consequências

**Positivo:**
- Cada serviço pode evoluir seu esquema independentemente sem impactar os demais
- Operação simplificada: um único tipo de banco a manter
- Garantia ACID em todos os serviços, incluindo o fluxo de venda que envolve múltiplos serviços
- Uso de Flyway para versionamento de schema em todos os serviços

**Negativo:**
- Consultas que atravessam domínios (ex: relatório consolidado) requerem agregação no nível da aplicação
- Sem transações distribuídas (necessário padrão Saga para operações cross-service)
- Perde-se a demonstração prática de poliglotismo de persistência em ambiente de desenvolvimento
