# ADR-0004 — Preempção por `SIGALRM` e desabilitação de interrupções por máscara de sinais

- **Status:** Aceita
- **Data:** 2026-10-17
- **Nota:** identificadores atualizados conforme a [ADR-0005](ADR-0005-nomenclatura-em-portugues.md).
- **Relacionada a:** [ADR-0001](ADR-0001-fidelidade-ao-material-teorico.md)

## 1. Contexto

No `kernel.c` do professor, a preempção vem da interrupção periódica do APIC *timer*: o hardware
empilha `rip`/`rflags`, o `timer_stub` empilha os registradores gerais, chama `Scheduler(rsp)` e
restaura o contexto devolvido. Em espaço de usuário no Linux não há IDT nem APIC; é preciso um
equivalente que preserve essa estrutura.

## 2. Decisão

| `kernel.c` (UEFI) | Implementação (Linux) |
|---|---|
| APIC *timer* periódico (`SetupApicTimer`) | `setitimer(ITIMER_REAL)` gerando `SIGALRM` a cada quantum (`ConfiguraTemporizador`) |
| Hardware empilha `rip`/`rflags` e salta para `timer_stub` | O Linux salva o contexto interrompido na pilha da *thread* e chama `TratadorTemporizador` |
| `timer_stub`: `push` ×15, `Scheduler`, `mov rsp, rax`, `pop` ×15, `iretq` | `TrocaContexto`: `push` ×15, `Escalonador`, `mov rsp, rax`, `pop` ×15, `ret`; o retorno do tratador (`sigreturn`) faz o papel do `iretq` |
| `cli` / `sti` | `DesabilitaInterrupcoes` / `HabilitaInterrupcoes` (bloqueio de `SIGALRM` via `sigprocmask`) |

Regras de consistência da máscara de sinais:

1. `TrocaContexto` é sempre executada com `SIGALRM` bloqueado. No tratador, o próprio Linux já o
   bloqueia; em `CedeProcessador`, `FinalizaThread` e `CriaThread`, o bloqueio é explícito.
2. Uma *thread* retomada dentro do tratador recupera a máscara anterior ao sinal no `sigreturn`;
   uma retomada dentro de `CedeProcessador` reabilita ao sair; uma *thread* nova reabilita em `InicioThread`.
3. `SA_RESTART` faz chamadas de sistema interrompidas pelo *timer* serem retomadas.

A entrada de toda *thread* nova passa a ser `InicioThread`, que reabilita as interrupções, chama
`Entrada(Argumento)` e, ao término, `FinalizaThread`. Isso também elimina o *stub* de realinhamento de pilha
da versão cooperativa.

## 3. Alternativas consideradas

| Alternativa | Motivo da rejeição |
|---|---|
| `SA_NODEFER` (não bloquear `SIGALRM` no tratador) | Permite um sinal aninhado no meio do `Escalonador`, corrompendo a tabela de *threads*. |
| `ITIMER_VIRTUAL` (tempo de CPU) | Não avança enquanto o processo está bloqueado; o APIC do exemplo mede tempo real. |
| Somente escalonamento cooperativo | Diverge do modelo preemptivo apresentado em aula. |

## 4. Consequências

- Funções da biblioteca C não reentrantes (`printf`, `malloc`) **não** podem ser interrompidas por
  uma troca para outra *thread* que também as utilize: o *lock* interno da `stdio` pertence à
  única *thread* do sistema operacional e não protege as nossas *threads*. Esses trechos devem
  ser envolvidos por `DesabilitaInterrupcoes`/`HabilitaInterrupcoes` ou pela exclusão mútua do cartão T-04.
- Chamadas bloqueantes (`sem_wait`, `waitpid`) continuam bloqueando o processo inteiro; devem ser
  usadas em forma não bloqueante, cedendo o processador entre tentativas.
