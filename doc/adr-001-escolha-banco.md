# ADR-001: Estratégia de Persistência por Microsserviço

## Contexto

O sistema é composto por três serviços de negócio com domínios distintos e requisitos de dados específicos:

- **compras-service**: dados altamente relacionais com integridade transacional entre insumos e pedidos a fornecedores
- **contratos-service**: dados estruturados com esquema fixo, consultados por vigência e status
- **vendas-service**: processa pedidos de clientes com itens e valores - o fluxo de venda exige consistência entre a baixa de estoque e o registro do pedido

Era necessário decidir duas coisas: (1) como organizar a posse dos dados entre os serviços e (2) qual tecnologia de banco adotar para o `vendas-service`, que foi o serviço com maior discussão sobre modelo de dados.

## Alternativas Consideradas

### Estratégia de organização dos dados

| Alternativa | Prós | Contras |
|---|---|---|
| Banco de dados compartilhado | Consultas entre domínios diretas, operação simples | Acoplamento forte entre serviços, impossibilita deploy e evolução independentes |
| Database per Service (escolhido) | Independência de deploy, isolamento de domínios, evolução independente por contexto | Consultas cross-domínio exigem agregação na aplicação; sem transações distribuídas nativas |

### Tecnologia para o `vendas-service`

| Alternativa | Prós | Contras |
|---|---|---|
| MongoDB | Flexibilidade de esquema para itens de pedido | O modelo de Venda é estruturado e o fluxo exige consistência transacional - a flexibilidade não traz benefício real |
| PostgreSQL (escolhido) | Garantia ACID para o fluxo de venda que envolve múltiplas operações; adequação ao modelo relacional do domínio | Ligeiramente menos flexível para dados semi-estruturados |

## Decisão

Adotar o padrão **Database per Service**: cada microsserviço é dono exclusivo dos seus dados e nenhum outro serviço acessa seu banco diretamente.

A justificativa arquitetural é que serviços com domínios distintos precisam poder evoluir, ser implantados e escalar de forma independente. Um banco compartilhado criaria acoplamento estrutural que invalida os benefícios da separação por microsserviço - qualquer mudança de schema em um serviço poderia impactar os demais.

Para a tecnologia, **PostgreSQL** foi adotado em todos os serviços. A principal motivação veio do `vendas-service`: embora MongoDB fosse uma opção considerada para acomodar a estrutura variável de itens de pedido, o modelo de dados de Venda é suficientemente estruturado e o fluxo de negócio exige consistência transacional entre operações - características que favorecem o modelo relacional. Manter a mesma tecnologia nos três serviços também reduz complexidade operacional sem abrir mão de adequação técnica.

## Consequências

**Positivo:**

- Cada serviço pode evoluir seu esquema independentemente sem impactar os demais
- Falhas ou mudanças em um banco não propagam para os outros serviços
- Garantia ACID em todos os serviços, incluindo o fluxo de venda que envolve múltiplas operações
- Redução de complexidade operacional ao manter um único tipo de banco

**Negativo:**

- Consultas que atravessam domínios (ex: relatório consolidado) requerem agregação no nível da aplicação
- Sem transações distribuídas nativas - operações cross-service requerem padrões como Saga
- Perde-se a demonstração prática de poliglotismo de persistência

## Histórico de Revisões

| Campo           | Valor                                                               |
| --------------- | ------------------------------------------------------------------- |
| Sistema         | Sistema de Gestão Comercial - Microsserviços                        |
| Autores         | Edval Júnior, Iago Barbosa, Mary Santos, Pedro Barros, Victor Kauan |
| Revisores       | Equipe do grupo (revisão interna)                                   |
| Supersede       | - (primeiro ADR do projeto)                                         |
| Supersedido por | -                                                                   |

| Versão | Data       | Autor  | Alteração                                                                                                                                                    |
| ------ | ---------- | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 1.0    | 2026-04-12 | Equipe | Criação inicial - previa MongoDB para o `vendas-service`                                                                                                     |
| 1.1    | 2026-04-25 | Equipe | Revisão após peer review: substituição de MongoDB por PostgreSQL no `vendas-service`; adicionada seção de alternativas consideradas                          |
| 1.2    | 2026-05-02 | Equipe | Revisão conforme feedback do professor: foco deslocado para a decisão arquitetural (Database per Service); remoção de detalhes de implantação e configuração |

**Status atual:** Aceito
