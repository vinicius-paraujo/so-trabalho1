# ADR 0006: Formato da saída textual

- Status: Aceita
- Data: 20/09/2026
- Responsável pela proposta: Marcos

## Contexto

O enunciado exige métricas e um diagrama vertical por segundo, mas não fixa os rótulos nem o espaçamento da saída. Um formato estável é necessário para testes automatizados e para execução pelo professor.

## Decisão

Cada algoritmo produzirá uma seção com a seguinte estrutura:

```text
algoritmo:FCFS
tt:7.50
tw:4.00
trocas_contexto:3
tempo P1 P2
0-1 -- ##
1-2 -- ##
```

1. Os algoritmos serão apresentados na ordem do enunciado.
2. As médias usarão duas casas decimais, ponto decimal e `Locale.ROOT`.
3. O cabeçalho do diagrama conterá todos os processos na ordem dos identificadores.
4. `##` indicará o processo executado e `--` indicará ausência de execução naquele processo.
5. Uma linha com somente `--` nas colunas de processos representará CPU ociosa.
6. Cada linha corresponderá exatamente a um intervalo de um segundo.
7. Uma linha vazia separará as seções dos algoritmos.

### Complemento de 27/09/2026 (T13)

8. Os nomes das seções, na ordem do enunciado, são:

   | Algoritmo do enunciado | Rótulo |
   | --- | --- |
   | FCFS | `FCFS` |
   | Shortest Job First | `SJF` |
   | Shortest Remaining Time First | `SRTF` |
   | Por prioridade, sem preempção | `PRIORIDADE_SEM_PREEMPCAO` |
   | Por prioridade, com preempção por prioridade | `PRIORIDADE_COM_PREEMPCAO` |
   | Round-Robin com quantum, sem prioridade | `ROUND_ROBIN` |
   | Round-robin com prioridade e envelhecimento | `ROUND_ROBIN_PRIORIDADE_AGING` |

   Os rótulos não têm espaços nem acentos, para que a saída possa ser processada por ferramentas simples.
9. As linhas são separadas por `\n` em qualquer sistema operacional, para que a saída seja idêntica byte a byte entre os ambientes da equipe.

### Divergência conhecida em relação ao exemplo do enunciado

No diagrama de exemplo do enunciado, as colunas de processos ainda não criados ou já concluídos aparecem em branco, e `--` indica apenas o processo que aguarda na fila. Pelo item 4 desta decisão, `--` significa "não executou neste segundo", sem essa distinção. A mudança é localizada em `ResultFormatter` e poderá ser feita se a equipe ou o professor preferirem a notação do exemplo.

A equipe manteve a notação desta decisão na saída textual. A interface gráfica (ADR 0007) exibe a distinção completa: não criado, pronto, executando e terminado.

## Consequências

- A saída permanece legível e próxima ao exemplo do enunciado.
- Os testes de integração podem comparar resultados completos.
- A interface gráfica poderá consumir os mesmos resultados sem conter regras de escalonamento.

