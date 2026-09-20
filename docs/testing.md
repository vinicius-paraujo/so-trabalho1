# Estratégia e Casos de Teste

## 1. Objetivo

Definir como a solução será testada e estabelecer os resultados esperados antes da implementação dos algoritmos. Este documento é a referência para os testes automatizados e para a validação manual.

As convenções adotadas estão justificadas no [ADR 0004](adrs/0004-convencoes-de-simulacao-e-testes.md).

## 2. Execução dos testes

No Windows:

```powershell
.\gradlew.bat clean test
```

No Linux ou WSL:

```bash
./gradlew clean test
```

O relatório HTML é gerado em `build/reports/tests/test/index.html`. Um card de implementação somente pode ser concluído quando todos os testes correspondentes passarem.

## 3. Convenções verificáveis

- O tempo é discreto. Uma entrada `P1` no instante `t` representa execução no intervalo `[t, t + 1)`.
- Processos que chegam em `t` participam da decisão tomada no início desse instante.
- No fim de um quantum, processos que chegam naquele instante entram na fila antes do processo que será recolocado por preempção.
- Os identificadores `P1`, `P2`, ... seguem a ordem das linhas da entrada original, mesmo quando a entrada não está ordenada por chegada.
- `turnaround = instante de conclusão - instante de criação`.
- `espera = turnaround - duração`.
- A média é calculada sem arredondamento interno. Comparações numéricas usam tolerância de `1e-9`; a apresentação textual usa duas casas decimais.
- `OCIOSO` representa intervalos sem processo pronto.
- A carga inicial da CPU, a passagem para `OCIOSO` e a saída de `OCIOSO` não contam como troca de contexto.
- Uma troca é contada quando dois intervalos consecutivos executados pela CPU pertencem a processos diferentes.
- A continuidade do mesmo processo após uma decisão não conta como troca.
- No Round-Robin, a cabeça da fila determina o próximo processo. O processo cujo quantum termina volta ao final da fila; isso não é tratado como empate.
- Para desempates que ainda dependam de escolha aleatória, os candidatos são ordenados por identificador e os testes usam `java.util.Random` com semente `0`.
- No Round-Robin prioritário, o aging é aplicado aos processos que aguardam sempre que um quantum completo termina. Não é aplicado na inicialização nem quando o processo termina antes de consumir o quantum.
- A chegada de processo mais prioritário não interrompe o quantum em andamento.

## 4. Níveis de teste

### 4.1 Unidade

Devem verificar isoladamente:

- leitura dos processos;
- leitura da configuração;
- transições de estado do processo;
- seleção e preempção de cada algoritmo;
- atualização da prioridade dinâmica;
- cálculo de métricas;
- contagem de trocas de contexto;
- formatação da linha do tempo.

### 4.2 Integração

Devem executar cada algoritmo a partir de uma entrada completa e comparar linha do tempo, métricas e trocas com os oráculos deste documento.

### 4.3 Validação manual

Deve executar o programa pelo Gradle Wrapper com os arquivos de `src/test/resources/cases/` e comparar a saída padrão com os resultados especificados.

## 5. Caso de referência do enunciado

Configuração:

```text
quantum:2
aging:1
```

Entrada:

```text
0 5 2
0 2 3
1 4 1
3 3 4
```

### 5.1 Linhas do tempo e métricas

Os vetores seguem a ordem `P1, P2, P3, P4`. `C` representa os instantes de conclusão.

| Algoritmo | Linha do tempo compacta | C | Turnaround | Espera | TT médio | TW médio | Trocas |
| --- | --- | --- | --- | --- | ---: | ---: | ---: |
| FCFS | `[0,2) P2; [2,7) P1; [7,11) P3; [11,14) P4` | `7, 2, 11, 14` | `7, 2, 10, 11` | `2, 0, 6, 8` | 7,50 | 4,00 | 3 |
| SJF | `[0,2) P2; [2,6) P3; [6,9) P4; [9,14) P1` | `14, 2, 6, 9` | `14, 2, 5, 6` | `9, 0, 1, 3` | 6,75 | 3,25 | 3 |
| SRTF | `[0,2) P2; [2,6) P3; [6,9) P4; [9,14) P1` | `14, 2, 6, 9` | `14, 2, 5, 6` | `9, 0, 1, 3` | 6,75 | 3,25 | 3 |
| Prioridade não preemptiva | `[0,2) P2; [2,7) P1; [7,10) P4; [10,14) P3` | `7, 2, 14, 10` | `7, 2, 13, 7` | `2, 0, 9, 4` | 7,25 | 3,75 | 3 |
| Prioridade preemptiva | `[0,2) P2; [2,3) P1; [3,6) P4; [6,10) P1; [10,14) P3` | `10, 2, 14, 6` | `10, 2, 13, 3` | `5, 0, 9, 0` | 7,00 | 3,50 | 4 |
| Round-Robin | `[0,2) P1; [2,4) P2; [4,6) P3; [6,8) P1; [8,10) P4; [10,12) P3; [12,13) P1; [13,14) P4` | `13, 4, 12, 14` | `13, 4, 11, 11` | `8, 2, 7, 8` | 9,75 | 6,25 | 7 |
| Round-Robin com prioridade e aging | `[0,2) P2; [2,4) P1; [4,7) P4; [7,9) P3; [9,12) P1; [12,14) P3` | `12, 2, 14, 7` | `12, 2, 13, 4` | `7, 0, 9, 1` | 7,75 | 4,25 | 5 |

No SRTF, a chegada de `P4` em `t = 3` empata com o tempo restante de `P3`; `P3` permanece na CPU. No Round-Robin, a sequência segue a fila FIFO demonstrada pelo diagrama do enunciado.

## 6. Casos específicos

### CT-01 — CPU ociosa

Entrada:

```text
2 1 1
4 1 1
```

Resultado comum aos sete algoritmos:

- linha do tempo: `[0,2) OCIOSO; [2,3) P1; [3,4) OCIOSO; [4,5) P2`;
- turnaround: `1, 1`;
- espera: `0, 0`;
- médias: `TT = 1,00`, `TW = 0,00`;
- trocas de contexto: `0`.

### CT-02 — Preempção por menor tempo ou maior prioridade

Entrada:

```text
0 5 1
2 1 3
```

Para SRTF e prioridade preemptiva:

- linha do tempo: `[0,2) P1; [2,3) P2; [3,6) P1`;
- turnaround: `6, 1`;
- espera: `1, 0`;
- médias: `TT = 3,50`, `TW = 0,50`;
- trocas de contexto: `2`.

Para SJF e prioridade não preemptiva:

- linha do tempo: `[0,5) P1; [5,6) P2`;
- turnaround: `5, 4`;
- espera: `0, 3`;
- médias: `TT = 4,50`, `TW = 1,50`;
- trocas de contexto: `1`.

### CT-03 — Término antes do quantum

Configuração: `quantum = 2`.

Entrada:

```text
0 1 1
0 3 1
```

Resultado para Round-Robin:

- linha do tempo: `[0,1) P1; [1,4) P2`;
- turnaround: `1, 4`;
- espera: `0, 1`;
- médias: `TT = 2,50`, `TW = 0,50`;
- trocas de contexto: `1`.

O segundo quantum de `P2` não produz troca artificial, pois não existe outro processo pronto.

### CT-04 — Ausência de preempção por prioridade durante o quantum

Configuração: `quantum = 2`, `aging = 1`.

Entrada:

```text
0 4 1
1 1 5
```

Resultado para Round-Robin prioritário:

- linha do tempo: `[0,2) P1; [2,3) P2; [3,5) P1`;
- turnaround: `5, 2`;
- espera: `1, 1`;
- médias: `TT = 3,50`, `TW = 1,00`;
- trocas de contexto: `2`.

### CT-05 — Aging altera a escolha

Configuração: `quantum = 2`, `aging = 2`.

Entrada:

```text
0 6 3
0 1 1
```

Resultado para Round-Robin prioritário:

- linha do tempo: `[0,2) P1; [2,3) P2; [3,7) P1`;
- turnaround: `7, 3`;
- espera: `1, 2`;
- médias: `TT = 5,00`, `TW = 1,50`;
- trocas de contexto: `2`.

Ao término do primeiro quantum, `P2` alcança prioridade dinâmica `3` e vence o empate por possuir menor tempo restante.

### CT-06 — Desempate aleatório reproduzível

Entrada:

```text
0 1 1
0 1 1
```

Para algoritmos cuja política deixa ambos os processos empatados, a lista de candidatos é `P1, P2`. Com `Random(0)`, o primeiro índice sorteado é `1`:

- linha do tempo: `[0,1) P2; [1,2) P1`;
- turnaround: `2, 1`;
- espera: `1, 0`;
- médias: `TT = 1,50`, `TW = 0,50`;
- trocas de contexto: `1`.

Este caso não altera a ordem FIFO do Round-Robin.

### CT-07 — Entrada fora de ordem e separadores variados

Entrada, preservando tabulação e múltiplos espaços:

```text
5\t1 1
0   2 2
1 1 3
```

Os identificadores permanecem `P1`, `P2`, `P3` pela ordem das linhas. Para FCFS:

- linha do tempo: `[0,2) P2; [2,3) P3; [3,5) OCIOSO; [5,6) P1`;
- turnaround: `1, 2, 2`;
- espera: `0, 0, 1`;
- médias: `TT = 1,67`, `TW = 0,33`;
- trocas de contexto: `1`.

### CT-08 — Chegada no limite do quantum

Configuração: `quantum = 2`.

Entrada:

```text
0 4 1
2 1 1
```

Resultado para Round-Robin:

- linha do tempo: `[0,2) P1; [2,3) P2; [3,5) P1`;
- turnaround: `5, 1`;
- espera: `1, 0`;
- médias: `TT = 3,00`, `TW = 0,50`;
- trocas de contexto: `2`.

`P2` entra na fila antes da recolocação de `P1` no instante `2`.

### CT-09 — Prioridade igual não provoca preempção

Entrada:

```text
0 4 3
1 1 3
```

Resultado para prioridade preemptiva:

- linha do tempo: `[0,4) P1; [4,5) P2`;
- turnaround: `4, 4`;
- espera: `0, 3`;
- médias: `TT = 4,00`, `TW = 1,50`;
- trocas de contexto: `1`.

### CT-10 — Aging acumulado e restauração da prioridade

Configuração: `quantum = 1`, `aging = 2`.

Entrada:

```text
0 4 5
0 3 1
```

Resultado para Round-Robin prioritário:

- linha do tempo: `[0,3) P1; [3,4) P2; [4,5) P1; [5,7) P2`;
- turnaround: `5, 7`;
- espera: `1, 4`;
- médias: `TT = 6,00`, `TW = 2,50`;
- trocas de contexto: `3`.

`P2` acumula prioridade até superar `P1`. Quando selecionado, retorna à prioridade estática; no quantum seguinte, `P1` volta a possuir a maior prioridade dinâmica.

### CT-11 — Término antecipado não aplica aging

Configuração: `quantum = 3`, `aging = 2`.

Entrada:

```text
0 1 5
0 1 1
1 1 2
```

Resultado para Round-Robin prioritário:

- linha do tempo: `[0,1) P1; [1,2) P3; [2,3) P2`;
- turnaround: `1, 3, 1`;
- espera: `0, 2, 0`;
- médias: `TT = 1,67`, `TW = 0,67`;
- trocas de contexto: `2`.

O término de `P1` antes do quantum não envelhece `P2`; por isso, o recém-chegado `P3` é selecionado.

### CT-12 — Aging de processo que chega durante o quantum

Configuração: `quantum = 2`, `aging = 1`.

Entrada:

```text
0 4 3
1 1 2
```

Resultado para Round-Robin prioritário:

- linha do tempo: `[0,2) P1; [2,3) P2; [3,5) P1`;
- turnaround: `5, 2`;
- espera: `1, 1`;
- médias: `TT = 3,50`, `TW = 1,00`;
- trocas de contexto: `2`.

No fim do primeiro quantum, `P2` recebe aging mesmo tendo aguardado somente parte do intervalo.

### CT-13 — Processo único e limites do quantum

Entrada:

```text
0 2 1
```

Para Round-Robin com quantum `1`, `2` ou valor maior que `2`:

- linha do tempo: `[0,2) P1`;
- turnaround: `2`;
- espera: `0`;
- médias: `TT = 2,00`, `TW = 0,00`;
- trocas de contexto: `0`.

### CT-14 — Chegada no término de outro processo

Entrada:

```text
0 2 1
2 1 1
```

Resultado para FCFS:

- linha do tempo: `[0,2) P1; [2,3) P2`;
- turnaround: `2, 1`;
- espera: `0, 0`;
- médias: `TT = 1,50`, `TW = 0,00`;
- trocas de contexto: `1`.

### CT-15 — Validação da entrada de processos

| Situação | Resultado esperado |
| --- | --- |
| Entrada vazia | Rejeitar. |
| Linha vazia ou somente com espaços | Rejeitar e informar a linha. |
| Menos ou mais de três campos | Rejeitar e informar a linha. |
| Campo não inteiro | Rejeitar e informar a linha e o campo. |
| Criação negativa | Rejeitar. |
| Duração zero ou negativa | Rejeitar. |
| Prioridade zero ou negativa | Rejeitar. |
| Múltiplos espaços ou tabulações | Aceitar. |
| Processos fora de ordem de chegada | Aceitar sem alterar os identificadores. |

### CT-16 — Validação da configuração

| Situação | Resultado esperado |
| --- | --- |
| `quantum` e `aging` válidos em qualquer ordem | Aceitar. |
| Espaços ao redor de chave, `:` ou valor | Aceitar. |
| Arquivo inexistente | Rejeitar. |
| Arquivo vazio ou linha vazia | Rejeitar. |
| Chave ausente, desconhecida ou duplicada | Rejeitar. |
| Linha sem `:` ou com mais de um `:` | Rejeitar. |
| Valor não inteiro | Rejeitar. |
| `quantum <= 0` | Rejeitar. |
| `aging < 0` | Rejeitar. |
| `aging = 0` | Aceitar. |

### CT-17 — Formato da saída padrão

Para a entrada de processo único do CT-13, cada seção deve conter:

```text
algoritmo:NOME
tt:2.00
tw:0.00
trocas_contexto:0
tempo P1
0-1 ##
1-2 ##
```

Devem existir sete seções, na ordem do enunciado, separadas por uma linha vazia. O contrato integral está no ADR 0006.

### CT-18 — Invariantes de qualquer simulação válida

Para todos os algoritmos e entradas válidas:

- nenhum processo executa antes de seu instante de criação;
- cada processo executa exatamente sua duração;
- somente um processo ocupa a CPU em cada instante;
- todo processo termina exatamente uma vez;
- `turnaround = espera + duração`;
- turnaround e espera nunca são negativos;
- a soma dos intervalos não ociosos é igual à soma das durações.

## 7. Classes de teste previstas

- `ProcessParserTest`.
- `ConfigurationParserTest`.
- `FcfsSchedulerTest`.
- `SjfSchedulerTest`.
- `SrtfSchedulerTest`.
- `NonPreemptivePrioritySchedulerTest`.
- `PreemptivePrioritySchedulerTest`.
- `RoundRobinSchedulerTest`.
- `PriorityRoundRobinSchedulerTest`.
- `MetricsCalculatorTest`.
- `ContextSwitchCounterTest`.
- `SchedulerApplicationTest`.
- `SimulationInvariantTest`.

Os nomes descrevem responsabilidades de teste; não obrigam a criação de abstrações que escondam os algoritmos.

## 8. Verificação dos oráculos

Os oráculos são verificados por cálculo das métricas, invariantes gerais e casos específicos. Uma revisão por outro integrante é recomendada para reduzir erros de interpretação, mas não constitui dependência para os cards de infraestrutura.
