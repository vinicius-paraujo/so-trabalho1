# ADR-0007 — Despertar do Analista sem perda de sinal

- **Status:** Aceita
- **Data:** 2026-10-17
- **Relacionada a:** [ADR-0001](ADR-0001-fidelidade-ao-material-teorico.md)

## 1. Contexto

O Analista segue o modelo do `printer.c`: dorme com `raise(SIGSTOP)` e é acordado com `SIGCONT`.
`SIGCONT` enviado a um processo que **ainda não parou** não tem efeito e não fica pendente. Se o
Atendente acordar o Analista enquanto ele ainda imprime a leva anterior, o despertar se perde e os
PIDs ficam no LNG até o próximo `SIGCONT` (problema clássico de *lost wake-up*).

## 2. Decisão

1. O Analista é **filho** do Atendimento. O pai sabe que o filho adormeceu por
   `waitpid(pid, &status, WUNTRACED)`, que retorna quando o filho recebe `SIGSTOP`
   (`WIFSTOPPED(status)`).
2. O Atendente só envia `SIGCONT` quando sabe que o Analista está parado. Dentro do Atendimento,
   a consulta será feita com `WNOHANG`, cedendo o processador entre tentativas, para não bloquear
   as demais *threads*.
3. O Analista imprime e **esvazia o buffer de saída** (`fflush`) antes de voltar a dormir; do
   contrário, com a saída redirecionada, o texto ficaria retido no processo parado.
4. O LNG é reescrito por cópia para `lng.tmp` seguida de `rename`, dentro da região crítica de
   `/sem_block`.
5. O Analista só é encerrado (`SIGKILL`) quando está parado, isto é, fora da região crítica; caso
   contrário, `/sem_block` poderia ficar ocupado para sempre.

## 3. Alternativas consideradas

| Alternativa | Motivo da rejeição |
|---|---|
| Enviar `SIGCONT` sem verificar o estado | Despertares perdidos; no modo `N = 0` o LNG poderia não ser esvaziado antes da saída. |
| Semáforo dedicado para acordar o Analista | Diverge do mecanismo `SIGSTOP`/`SIGCONT` pedido no diagrama e usado no `printer.c`. |
| Reescrever o LNG no próprio arquivo (`r+`) | Exige truncar o arquivo manualmente; a cópia com `rename` é mais simples de ler e de explicar. |

## 4. Verificação

`tests/teste_analista.c` executa o binário real: nasce parado e publica o PID; com 25 PIDs no LNG,
três despertares imprimem 10, 10 e 5 na ordem; com o LNG vazio não imprime nada; com `/sem_block`
ocupado, acorda mas espera, e o LNG só muda após a liberação. Ao final não restam semáforos,
arquivos nem processos. Estável em 10 execuções.
