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

## Consequências

- A saída permanece legível e próxima ao exemplo do enunciado.
- Os testes de integração podem comparar resultados completos.
- A interface gráfica poderá consumir os mesmos resultados sem conter regras de escalonamento.

