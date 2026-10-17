# ADR-0008 — Fim do atendimento detectado por `waitpid`, e não por `/sem_atend`

- **Status:** Aceita
- **Data:** 2026-10-17
- **Relacionada a:** [ADR-0001](ADR-0001-fidelidade-ao-material-teorico.md), [ADR-0007](ADR-0007-despertar-do-analista.md)

## 1. Contexto

O diagrama descreve o Atendente assim: acorda o cliente, "espera semáforo `/sem_atend` abrir",
espera `/sem_block`, escreve o PID no LNG, libera `/sem_block`, calcula a satisfação, tira o
cliente da fila e acorda o Analista.

Tomado literalmente, "esperar `/sem_atend` abrir" logo após o `SIGCONT` tem uma condição de
disputa: o cliente acordado pode ainda não ter executado seu `sem_wait`. Nesse instante o semáforo
está aberto, e o Atendente concluiria que o atendimento terminou antes de ele começar. Além
disso, `sem_wait` bloquearia o processo inteiro, inclusive a Recepção.

## 2. Decisão

1. O Atendente detecta o fim do atendimento pelo **término do processo cliente**:
   `waitpid(pid, NULL, WNOHANG)` em laço, cedendo o processador entre consultas. O cliente só
   termina depois de liberar `/sem_atend`, então a intenção do diagrama — esperar o atendimento
   acabar — é preservada sem a disputa. O mesmo `waitpid` recolhe o processo, evitando zumbis.
2. `/sem_atend` continua criado pela Recepção e usado pelo cliente, como o `cliente.c` exige.
3. O acesso ao LNG usa `/sem_block` com `sem_trywait` em laço, cedendo o processador, pelo mesmo
   motivo de não bloquear as demais *threads*.
4. A hora de término usada na satisfação é medida assim que o término do cliente é detectado,
   antes da escrita no LNG.
5. O cliente é retirado da fila **antes** do atendimento (o diagrama o retira ao final). A
   diferença apenas libera a vaga mais cedo para a Recepção; não altera o resultado.
6. O Analista é acordado quando há ao menos 10 PIDs pendentes e ele está comprovadamente parado
   (ADR-0007). Ao final, `EsvaziaLng` o acorda até não restar nenhum PID.

## 3. Alternativas consideradas

| Alternativa | Motivo da rejeição |
|---|---|
| `sem_wait(/sem_atend)` logo após o `SIGCONT` | Condição de disputa descrita acima; bloqueia o processo inteiro. |
| Esperar `/sem_atend` fechar e depois abrir | Depende de observar um intervalo curto; ainda sujeito a disputa e a espera ocupada longa. |
| Acordar o Analista a cada cliente | Mais trocas de processo e mais escrita na tela, sem ganho; nota 1 deixa a estratégia livre. |
