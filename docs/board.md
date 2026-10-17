# Board Kanban — Trabalho 1 (Prof. Yuri Lenon)

**Apresentação:** 13/11/2026

Fluxo: **Backlog → A fazer → Em andamento → Revisão → Concluído**.
Para mover um cartão, recorte a linha da tabela de origem e cole na tabela de destino.
Um cartão só entra em **A fazer** quando todas as suas dependências estão em **Concluído**.

## Backlog

| ID | Título | Dependências | Responsável | Critério de pronto |
|---|---|---|---|---|
| T-06 | *Thread* Recepção | T-03, T-05 | — | Cria `N` clientes, lê a demanda, sorteia prioridade (50/50) e enfileira. |
| T-08 | *Thread* Atendente | T-06, T-07 | — | Atende, mede tempo, avalia satisfação, grava PID e acorda o Analista. |
| T-09 | Modo `N = 0` (tecla `s`) | T-08 | — | `s` encerra; Analista esvazia o arquivo antes da saída. |
| T-10 | Métricas finais e limpeza de recursos | T-08 | — | Imprime taxa de satisfação e tempo total; sem semáforos, arquivos ou zumbis remanescentes. |
| T-11 | Política da fila (ADR) | T-10 | — | Comparação medida entre políticas registrada em ADR; política escolhida implementada. |
| T-12 | README de compilação e execução | T-10 | — | Qualquer pessoa compila e executa seguindo apenas o README. |
| T-13 | Roteiro da apresentação | T-11, T-12 | — | Roteiro de até 15 minutos vinculado às ADRs. |

## A fazer

| ID | Título | Dependências | Responsável | Critério de pronto |
|---|---|---|---|---|

## Em andamento

| ID | Título | Dependências | Responsável | Critério de pronto |
|---|---|---|---|---|

## Revisão

| ID | Título | Dependências | Responsável | Critério de pronto |
|---|---|---|---|---|
| T-07 | Processo Analista | T-01 | Marcos | Nasce dormindo; a cada `SIGCONT` imprime e remove no máximo 10 PIDs. |

## Concluído

| ID | Título | Dependências | Responsável | Critério de pronto |
|---|---|---|---|---|
| T-00 | Documentação inicial (board, arquitetura, ADR-0001) | — | Marcos | Documentos em `docs/`. |
| T-00b | Estratégia de testes (ADR-0002) | T-00 | Marcos | ADR-0002 aceita. |
| T-01 | Estrutura do repositório e `Makefile` | — | Marcos | `make` compila `atendimento`, `analista` e `cliente`; `make test` executa `tests/` com a macro de `tests/teste.h` (ADR-0002); `make clean` limpa. |
| T-02 | Núcleo de *threads* (TCB, pilhas, `CriaThread`, troca de contexto cooperativa) | T-01 | Marcos | Duas *threads* alternam via `CedeProcessador`, com contexto salvo/restaurado em *assembly*. |
| T-03 | Preempção por `setitimer` + `SIGALRM` | T-02 | Marcos | Duas *threads* em laço infinito alternam sem `CedeProcessador` explícito. |
| T-04 | Exclusão mútua (`EntraRegiaoCritica`/`SaiRegiaoCritica`) | T-02 | Marcos | Contador incrementado por duas *threads* preemptadas não perde atualizações. |
| T-05 | Fila de atendimento | T-04 | Marcos | Inserção/remoção corretas sob preempção; testes passando. |
