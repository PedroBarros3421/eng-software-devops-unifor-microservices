# ADR-001: Escolha dos Bancos de Dados por Serviço

**Status:** Aceito
**Data:** 2026-04-12

## Contexto

O sistema é composto por três microsserviços de negócio com domínios distintos:

- **compras-service**: gerencia insumos e pedidos a fornecedores (dados altamente relacionais com integridade transacional)
- **contratos-service**: gerencia contratos com vigência, termos e status (dados estruturados com esquema fixo)
- **vendas-service**: processa pedidos de clientes com itens variáveis e necessidade de flexibilidade no esquema

O padrão **Database per Service** exige que cada serviço tenha sua própria base de dados para garantir independência de deploy e evitar acoplamento forte.

## Decisão

Adotar **poliglotismo de persistência**:

| Serviço | Banco | Justificativa |
|---|---|---|
| compras-service | PostgreSQL | Dados relacionais, necessidade de `JOIN` entre insumos e pedidos, integridade transacional |
| contratos-service | PostgreSQL | Esquema fixo e estruturado, consultas por vigência e status, auditoria |
| vendas-service | MongoDB | Itens de pedido com estrutura variável (documentos embarcados), flexibilidade de esquema |

Ambos os PostgreSQL rodam na mesma instância em ambiente de desenvolvimento (databases separados: `compras_db` e `contratos_db`), porém o isolamento lógico garante independência. Em produção, cada serviço teria sua própria instância.

## Consequências

**Positivo:**
- Cada serviço pode evoluir seu esquema independentemente sem impactar os demais
- O banco escolhido é adequado ao padrão de acesso de cada domínio
- Demonstra o princípio de Poliglotismo de Persistência da arquitetura de microsserviços

**Negativo:**
- Aumenta a complexidade operacional (dois tipos de banco a manter)
- Consultas que atravessam domínios (ex: relatório consolidado) requerem agregação no nível da aplicação
- Sem transações distribuídas (necessário padrão Saga para operações cross-service)
