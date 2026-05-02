# ADR-003: Estratégia de Observabilidade para Microsserviços

## Contexto

Em uma arquitetura de microsserviços, uma única operação de negócio atravessa múltiplos serviços e pontos de integração. Isso dificulta responder, com rapidez e evidência, perguntas como:

1. Onde uma requisição falhou e em qual serviço o problema começou
2. Qual chamada interna concentrou a maior latência
3. Se um incidente foi isolado ou em cascata
4. Se o sistema está atendendo metas operacionais de disponibilidade e tempo de resposta

Era necessário adotar uma estratégia de observabilidade que cobrisse os três pilares do domínio operacional: logs, métricas e rastreamento distribuído.

Além do aspecto técnico, a decisão precisava considerar o contexto do projeto: ambiente acadêmico, orçamento restrito, operação simplificada e necessidade de aprendizado com ferramentas amplamente adotadas no ecossistema cloud-native.

## Alternativas Consideradas

| Alternativa                                        | Prós                                                                                                     | Contras                                                                              |
| -------------------------------------------------- | -------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| Apenas logs estruturados                           | Menor esforço inicial e baixa complexidade                                                               | Não oferece visão end-to-end nem facilita análise de latência entre serviços         |
| ELK Stack                                          | Ecossistema maduro e forte capacidade de busca                                                           | Maior custo operacional e consumo de recursos para o porte atual do projeto          |
| Soluções SaaS pagas                                | Menor esforço de operação e experiência integrada                                                        | Custo recorrente, dependência de fornecedor e menor aderência ao contexto acadêmico  |
| Ferramentas isoladas para cada pilar               | Flexibilidade de composição                                                                              | Aumenta integração, manutenção e fragmentação da análise operacional                 |
| Stack open-source integrada do ecossistema Grafana | Boa cobertura dos três pilares, baixo custo de adoção e integração natural com ambientes containerizados | Exige operação própria e pode oferecer menos conveniência que plataformas comerciais |

## Decisão

Adotar uma stack open-source integrada de observabilidade, centrada no ecossistema Grafana e em padrões abertos de instrumentação e correlação.

Essa escolha foi feita por três razões principais:

1. **Custo**: elimina dependência imediata de licenças e custos recorrentes, o que é mais adequado ao orçamento do projeto.
2. **Simplicidade operacional relativa**: embora exista operação própria, a stack escolhida é suficientemente difundida, bem documentada e compatível com o nível de complexidade esperado para este ambiente.
3. **Flexibilidade futura**: ao usar padrões abertos para métricas, logs e traces, a equipe preserva a possibilidade de migrar para outra plataforma no futuro sem alterar a lógica de negócio dos serviços.

A decisão arquitetural, portanto, não é sobre portas, dashboards ou endpoints específicos, mas sobre priorizar uma solução com boa relação entre cobertura funcional, custo e independência tecnológica.

## Consequências

**Positivo:**

- Permite observar requisições distribuídas de forma mais consistente, reduzindo o tempo de diagnóstico em incidentes
- Mantém a solução aderente ao contexto financeiro e acadêmico do projeto
- Reduz risco de aprisionamento em fornecedor ao adotar padrões e ferramentas amplamente reconhecidos no mercado
- Cria uma base evolutiva: a equipe pode sofisticar a operação de observabilidade sem reescrever a lógica dos microsserviços

**Negativo:**

- A equipe assume responsabilidade pela operação e manutenção da stack escolhida
- Pode haver menor conveniência inicial quando comparado a plataformas SaaS especializadas
- A maturidade operacional necessária para extrair valor da observabilidade continua sendo responsabilidade do time, independentemente da ferramenta adotada

## Consequências para o Futuro

Esta decisão não impede migração futura para uma solução comercial ou gerenciada. Pelo contrário: ela registra que a escolha atual foi orientada por custo, simplicidade e adequação ao contexto, e não por uma dependência arquitetural irreversível.

Se o projeto evoluir para um cenário com maior orçamento, requisitos regulatórios mais rígidos ou necessidade de operação gerenciada, a equipe poderá reavaliar a plataforma de observabilidade sem alterar contratos de serviço ou regras de negócio centrais.

## Histórico de Revisões

| Campo           | Valor                                                               |
| --------------- | ------------------------------------------------------------------- |
| Sistema         | Sistema de Gestão Comercial — Microsserviços                        |
| Autores         | Edval Júnior, Iago Barbosa, Mary Santos, Pedro Barros, Victor Kauan |
| Revisores       | —                                                                   |
| Supersede       | —                                                                   |
| Supersedido por | —                                                                   |

| Versão  | Data       | Autor  | Alteração                                                                                                                 |
| ------- | ---------- | ------ | ------------------------------------------------------------------------------------------------------------------------- |
| 1.0     | 2026-04-25 | Equipe | Criação inicial                                                                                                           |
| 1.1 --- | ---        | ---    |
| 1.0     | 2026-04-25 | Equipe | Criação inicial                                                                                                           |
| 1.1     | 2026-04-25 | Equipe | Revisão após peer review: remoção de detalhes de implementação e reforço da justificativa técnica e financeira da decisão |

**Status atual:** Aceito
