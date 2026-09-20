# Board do Trabalho 01

## Convenções

- Status possíveis: `Pendente`, `Em andamento`, `Em revisão`, `Concluído` e `Bloqueado`.
- Um card somente pode ser concluído quando seus critérios de aceite forem atendidos.
- A implementação dos algoritmos depende do registro prévio dos casos de teste e de seus resultados esperados.
- Cada responsável deve atualizar requisitos, arquitetura, ADRs e demais registros afetados durante a execução de seu card.
- Responsabilidades ainda não distribuídas permanecem como `A definir`.

## Estado dos cards

| ID | Card | Responsável | Dependências | Status |
| --- | --- | --- | --- | --- |
| T01 | Consolidar requisitos do enunciado e das notas de aula | Marcos | — | Concluído |
| T02 | Registrar linguagem, ferramentas e diretrizes de implementação | Marcos | T01 | Concluído |
| T03 | Inicializar o projeto Java 21 com Gradle Wrapper e JUnit 5 | Marcos | T02 | Concluído |
| T04 | Especificar casos de teste e resultados esperados | Marcos | T01, T03 | Concluído |
| T05 | Implementar configuração, entrada e modelo explícito de processo | Marcos | T04 | Concluído |
| T06 | Implementar FCFS | Marcos | T05 | Concluído |
| T07 | Implementar SJF não preemptivo | A definir | T05 | Pendente |
| T08 | Implementar SRTF preemptivo | A definir | T05 | Pendente |
| T09 | Implementar prioridade não preemptiva | A definir | T05 | Pendente |
| T10 | Implementar prioridade preemptiva | A definir | T05 | Pendente |
| T11 | Implementar Round-Robin sem prioridade | A definir | T05 | Pendente |
| T12 | Implementar Round-Robin com prioridade e aging | A definir | T05 | Pendente |
| T13 | Consolidar métricas, trocas de contexto e linha do tempo | A definir | T06–T12 | Pendente |
| T14 | Executar validação integrada e revisão cruzada | A definir | T13 | Pendente |
| T15 | Implementar interface gráfica | A definir | T14 | Pendente |
| T16 | Preparar entrega reproduzível e revisão final | A definir | T14, T15 | Pendente |
| T17 | Construir apresentação para o professor e dividir as falas | Equipe | T16 | Pendente |

## Critérios de aceite

### T01 — Consolidar requisitos

- O enunciado integral está disponível no repositório.
- Algoritmos, entradas, configuração, saídas e desempates obrigatórios estão registrados em `requirements.md`.
- Requisitos confirmados estão separados de decisões da equipe.

### T02 — Registrar linguagem e diretrizes

- Java 21, Gradle Wrapper e JUnit 5 estão registrados em ADR.
- A arquitetura exige que o fluxo teórico de cada algoritmo permaneça visível no código.
- Abstrações que ocultem decisões de escalonamento estão explicitamente vedadas.

### T03 — Inicializar o projeto

- O projeto compila exclusivamente pelo Gradle Wrapper.
- A versão Java utilizada é validada como Java 21.
- Um teste mínimo do JUnit 5 é executado com sucesso.
- As instruções de compilação e teste são reproduzíveis.

### T04 — Especificar casos de teste

- Cada algoritmo possui entradas, linha do tempo e métricas esperadas calculadas antes de sua implementação.
- Existem casos para chegadas simultâneas, CPU ociosa, empates e processos com durações diferentes.
- Existem casos específicos para preempção, término antes do quantum, aging e ausência de preempção por prioridade no Round-Robin prioritário.
- A contagem esperada de trocas de contexto está explícita.
- O desempate aleatório possui estratégia determinística para os testes.
- Os resultados esperados são verificáveis por fórmulas, invariantes e casos específicos.

### T05 — Implementar infraestrutura de domínio

- O modelo representa identificador, chegada, duração, tempo restante, prioridade estática, prioridade dinâmica e estado.
- Entrada padrão e arquivo de configuração são validados.
- Estruturas de dados e transições de estado são legíveis e documentadas.
- Testes definidos em T04 para esse escopo passam.

### T06 a T12 — Implementar algoritmos

- A implementação corresponde ao algoritmo apresentado nas notas de aula e ao enunciado.
- O laço de escalonamento, a seleção do processo e as preempções permanecem explícitos.
- O código não delega a regra central a abstrações genéricas que dificultem sua apresentação.
- Os casos correspondentes definidos em T04 passam.
- Particularidades e decisões não determinadas pelo enunciado estão registradas.

### T13 — Consolidar resultados

- Cada algoritmo informa turnaround médio, espera média, trocas de contexto e linha do tempo por segundo.
- O formato textual é estável e permite distinguir todos os algoritmos.
- CPU ociosa e continuidade do mesmo processo são representadas sem ambiguidade.

### T14 — Validar a solução

- Todos os testes automatizados passam em ambiente limpo.
- Cada integrante revisa código produzido por outro integrante.
- Casos do enunciado e das notas são executados manualmente.
- Divergências encontradas são corrigidas ou documentadas.

### T15 — Implementar interface gráfica

- A tecnologia gráfica e sua justificativa estão registradas antes da implementação.
- A interface apresenta processos, linha do tempo e métricas dos algoritmos.
- A interface não altera nem duplica as regras de escalonamento.
- O núcleo permanece executável e testável independentemente da interface.

### T16 — Preparar entrega

- A aplicação pode ser compilada, testada e executada pelas instruções versionadas.
- Arquitetura, requisitos, ambiente e ADRs refletem a implementação entregue.
- Não permanecem hipóteses apresentadas como requisitos do professor.
- Somente arquivos pertencentes à entrega permanecem no diretório do projeto.
- A entrega é revisada por todos os integrantes.

### T17 — Construir apresentação e dividir falas

- O formato exigido pelo professor é confirmado.
- A apresentação demonstra requisitos, algoritmos, estruturas, testes e decisões relevantes.
- As falas são distribuídas entre os três integrantes.
- Cada integrante consegue justificar sua parte e as decisões gerais do projeto.
