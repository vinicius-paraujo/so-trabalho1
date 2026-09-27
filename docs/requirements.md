# Requisitos do Trabalho 01

## 1. Identificação

- Disciplina: CK0234 — Sistemas Operacionais.
- Atividade: Tarefa 01 — Escalonamento de Processos.
- Equipe: três integrantes.
- Implementação adotada: Java 21.

## 2. Fontes normativas

Ordem de precedência:

1. `Notas de aula/Tarefa 01 - Escalonamento de Processos-1.pdf`.
2. Notas de aula fornecidas pelo professor, especialmente `Notas de aula/Escalonamento de Processos.pdf`.
3. Decisões da equipe registradas em ADR.
4. Referências externas aprovadas pela equipe.

Uma decisão da equipe não pode alterar silenciosamente o comportamento exigido pelo enunciado. Divergências ou interpretações necessárias devem ser documentadas.

## 3. Objetivo

Implementar um programa que simule o escalonamento de um conjunto de processos e apresente o comportamento e as métricas produzidas por cada algoritmo exigido.

## 4. Requisitos funcionais

| ID | Requisito | Critério de aceite |
| --- | --- | --- |
| RF-01 | Implementar FCFS. | O programa executa o escalonamento por ordem de chegada e apresenta os resultados exigidos. |
| RF-02 | Implementar SJF não preemptivo. | Entre os processos prontos, o programa seleciona o trabalho de menor duração sem interrompê-lo por nova chegada. |
| RF-03 | Implementar SRTF preemptivo. | A cada decisão aplicável, o programa executa o processo pronto com menor tempo restante. |
| RF-04 | Implementar prioridade não preemptiva. | O processo pronto com maior prioridade é escolhido e permanece até terminar. |
| RF-05 | Implementar prioridade preemptiva. | A execução pode ser interrompida pela disponibilidade de processo com maior prioridade. |
| RF-06 | Implementar Round-Robin com quantum e sem prioridade. | Os processos prontos recebem a CPU por até um quantum e retornam à fila quando não terminam. |
| RF-07 | Implementar Round-Robin com prioridade e aging. | A seleção considera prioridade dinâmica nos limites do quantum; o aging ocorre a cada quantum e não há preempção por prioridade durante o quantum. |
| RF-08 | Ler os processos pela entrada padrão. | Cada linha válida é convertida em um processo com chegada, duração e prioridade. |
| RF-09 | Ler quantum e taxa de aging de arquivo textual. | Os valores no formato definido pelo enunciado são carregados antes da simulação. |
| RF-10 | Produzir resultados para cada algoritmo. | A saída contém turnaround médio, espera média, trocas de contexto e linha do tempo por segundo. |
| RF-11 | Aplicar a ordem de desempate exigida. | Em empate, preserva-se o processo na CPU; depois, escolhe-se o menor tempo restante; persistindo o empate, realiza-se escolha aleatória. |
| RF-12 | Documentar a implementação. | A documentação explica classes, estruturas de dados, decisões e padrões de projeto utilizados, quando houver. |

## 5. Entrada

### 5.1 Processos

A entrada é recebida por `stdin`. Cada linha representa um processo e contém três números inteiros separados por um ou mais espaços:

```text
instante_de_criacao duracao prioridade_estatica
```

Exemplo fornecido pelo enunciado:

```text
0 5 2
0 2 3
1 4 1
3 3 4
```

A entrada pode não estar ordenada pelo instante de criação.

### 5.2 Configuração

O quantum e a taxa de aging são fornecidos em arquivo de texto simples:

```text
quantum:2
aging:1
```

O enunciado não determina o nome nem o caminho desse arquivo.

## 6. Saída

Para cada algoritmo, a saída padrão deve apresentar:

- tempo médio de turnaround (`tt`);
- tempo médio de espera (`tw`);
- número de trocas de contexto;
- linha do tempo da execução, com uma linha por segundo.

O formato visual exato não é fixado. A saída deve identificar sem ambiguidade o algoritmo, o instante e o processo executado.

O tempo de resposta é discutido nas notas, mas não aparece entre as saídas obrigatórias do enunciado.

## 7. Regras de escalonamento

### 7.1 Prioridade

Conforme as notas de aula, valores numéricos maiores representam prioridades maiores.

### 7.2 Prioridade dinâmica e aging

- A prioridade estática permanece inalterada.
- A prioridade dinâmica é inicializada com a prioridade estática.
- Processos que aguardam recebem o incremento definido pela taxa de aging.
- Quando selecionado, o processo retorna sua prioridade dinâmica ao valor da prioridade estática.
- No Round-Robin prioritário, a prioridade é reavaliada nos limites do quantum; uma chegada não provoca preempção imediata por prioridade.

### 7.3 Desempates

A ordem obrigatória é:

1. processo que já ocupa a CPU, evitando troca de contexto;
2. processo com menor tempo restante;
3. escolha aleatória.

## 8. Requisitos de qualidade adotados pela equipe

| ID | Requisito | Critério de aceite |
| --- | --- | --- |
| RQ-01 | Manter correspondência visível entre teoria e código. | Seleção, execução, preempção e atualização das filas podem ser identificadas na implementação de cada algoritmo. |
| RQ-02 | Definir testes antes dos algoritmos. | Entradas e resultados esperados são revisados antes do código correspondente. |
| RQ-03 | Garantir construção reproduzível. | Compilação e testes são executados pelo Gradle Wrapper com Java 21. |
| RQ-04 | Automatizar a verificação. | Os casos definidos pela equipe são executados com JUnit 5. |
| RQ-05 | Documentar decisões relevantes. | Decisões com impacto em comportamento, arquitetura, testes ou apresentação possuem registro explícito. |
| RQ-06 | Preservar legibilidade. | Abstrações e padrões somente são adotados quando não ocultam o fluxo teórico. |

## 9. Decisões ainda necessárias

O enunciado não define integralmente o contrato de terminal. A equipe decidiu que o caminho da configuração será o único argumento, conforme ADR 0005, e fixou a saída textual no ADR 0006.

As convenções de identificação, tempo, métricas, trocas de contexto, CPU ociosa e aleatoriedade estão registradas no ADR 0004. As regras 12 a 16 foram acrescentadas durante a implementação dos algoritmos e aguardam revisão independente da equipe.

Os requisitos RF-01 a RF-11 estão implementados e validados (ver `testing.md` §9). O RF-12 é atendido por `architecture.md` e pelos ADRs, e será consolidado no documento de entrega (T16).

## 10. Interface gráfica

Embora opcional no enunciado, a interface gráfica foi incorporada ao escopo pela equipe. Sua implementação ocorrerá depois que todos os requisitos obrigatórios estiverem concluídos e validados. A decisão está registrada no ADR 0003.
