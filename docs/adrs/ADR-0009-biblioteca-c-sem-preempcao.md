# ADR-0009 — Chamadas à biblioteca C com interrupções desabilitadas

- **Status:** Aceita
- **Data:** 2026-10-17
- **Relacionada a:** [ADR-0004](ADR-0004-preempcao-por-sigalrm.md), [ADR-0006](ADR-0006-exclusao-mutua-por-tsl.md)

## 1. Contexto

Durante o cartão T-09, testes de estresse revelaram falhas intermitentes no programa completo:
cerca de 1 execução em 20–70 terminava em *segmentation fault* ou ficava presa em laço infinito
(estado `R`) depois de já ter liberado os recursos.

Causa: a `glibc` detecta que o processo tem **uma única *thread* do sistema operacional** e, para
ganhar desempenho, **dispensa as travas internas** do `malloc` e da `stdio`. Nossas *threads* são
invisíveis para ela. Quando a Recepção era preemptada no meio de um `fopen` (que aloca memória) e
o Atendente executava outro `fopen`, as estruturas internas do alocador eram corrompidas; o
efeito aparecia mais tarde, em qualquer outra chamada.

A ADR-0004 já havia registrado esse risco para `printf`/`malloc`; ele se mostrou mais amplo.

## 2. Decisão

Toda chamada à biblioteca C que possa usar `malloc` ou `stdio` — `fopen`, `fscanf`, `fprintf`,
`fclose`, `remove`, `printf`, `fork`, `sem_open`, `sem_close`, `rand` — executada enquanto mais de
uma *thread* nossa está ativa fica entre `DesabilitaInterrupcoes()` e `HabilitaInterrupcoes()`, o
equivalente a `cli`/`sti`. O trecho torna-se indivisível em relação ao nosso escalonador.

Chamadas de sistema diretas e atômicas (`kill`, `waitpid`, `sem_trywait`, `sem_post`, `read`)
não precisam da proteção.

## 3. Alternativas consideradas

| Alternativa | Motivo da rejeição |
|---|---|
| Uma `TRAVA` global para a biblioteca C | Funciona, mas uma *thread* que espera a trava ainda poderia ser preemptada dentro de outra chamada não protegida; desabilitar interrupções é mais simples e cobre qualquer chamada. |
| Forçar a `glibc` a usar travas criando uma *thread* real | Exigiria `pthread_create`, vedado pelo enunciado. |
| Evitar `stdio` e usar `open`/`read`/`write` diretamente | Reescreveria trechos já alinhados ao estilo do `printer.c`; e `fork`/`sem_open` continuariam expostos. |

## 4. Verificação

Antes da correção: falhas reproduzidas em 1 a cada 20–70 execuções. Depois: **0 falhas em 240
execuções** (200 com `N = 30`, 40 no modo `N = 0`), além de `make test` completo.
