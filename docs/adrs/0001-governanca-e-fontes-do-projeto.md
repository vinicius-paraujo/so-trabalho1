# ADR 0001: Governança de decisões e fontes do projeto

- Status: Aceita
- Data: 20/09/2026
- Responsável pela proposta: Marcos

## Contexto

O trabalho será desenvolvido por três integrantes e posteriormente defendido perante o professor. As decisões técnicas precisam ser compreendidas, justificadas e atribuíveis à equipe. O enunciado integral e as notas de aula estão disponíveis no repositório e constituem as fontes primárias do projeto.

O uso de ferramentas de assistência, inclusive inteligência artificial, não transfere a autoria nem a responsabilidade pelas decisões do projeto.

## Decisão

1. O enunciado oficial e as comunicações dos professores prevalecem sobre qualquer outra fonte.
2. As notas dos professores são a referência primária para algoritmos, terminologia e critérios relacionados à disciplina.
3. Decisões com impacto em arquitetura, comportamento, ambiente, testes, divisão estrutural do código ou apresentação devem ser registradas em ADR.
4. O registro de decisão deve apresentar contexto, alternativas relevantes, decisão e consequências.
5. Nenhum requisito ausente será inventado ou ocultado. Hipóteses serão identificadas como hipóteses até validação.
6. Abstrações de baixo nível somente serão adotadas quando reduzirem complexidade sem prejudicar legibilidade, teste ou explicação oral.
7. A equipe deve conseguir explicar e defender todo código e toda decisão incorporados à entrega.
8. Alterações no escopo ou em decisões aceitas devem atualizar o board, os requisitos e a arquitetura afetados.

## Consequências

### Positivas

- Rastreabilidade entre enunciado, notas, implementação e testes.
- Maior consistência durante a apresentação.
- Redução de decisões implícitas e de divergências entre integrantes.
- Evidência objetiva da participação humana no projeto.

### Custos e riscos

- Manutenção documental adicional.
- Necessidade de revisar documentos quando o enunciado completo for incorporado.
- Possibilidade de adiar decisões técnicas enquanto requisitos essenciais estiverem ausentes.

## Critério de aplicação

Uma decisão deve gerar ADR quando sua reversão puder alterar interfaces, algoritmos, resultados, ambiente de execução, estratégia de testes ou divisão relevante de responsabilidades.

Correções locais, formatação e refatorações sem impacto observável não exigem ADR próprio.
