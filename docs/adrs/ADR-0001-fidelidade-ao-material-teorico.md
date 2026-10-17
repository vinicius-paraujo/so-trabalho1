# ADR-0001 — Fidelidade ao material teórico do professor

- **Status:** Aceita
- **Data:** 2026-10-17
- **Contexto da disciplina:** CK0234 — Sistemas Operacionais (UFC, 2026.2) — Trabalho 1 (parte prática), Prof. Yuri Lenon Barbosa Nogueira

## 1. Contexto

O Trabalho 1 exige um processo "Atendimento" em C, no Linux, composto por *threads* criadas
**por troca de contexto implementada pelo próprio aluno** (isto é, com escalonador próprio), sem
uso de bibliotecas que auxiliem o paralelismo ou a comunicação entre processos.

Existem, para quase todas as partes do problema, soluções "de indústria" mais curtas ou mais
eficientes (por exemplo, `pthread`, `ucontext`, filas de mensagens POSIX, `inotify`, `epoll`).
Contudo, o objetivo avaliativo é demonstrar domínio dos conceitos vistos em sala, e o professor
forneceu material próprio que define um vocabulário e um estilo de solução:

| Material | Autor | O que estabelece |
|---|---|---|
| `protokernel/aulafinal/kernel.c` | Yuri | Vetor `Process[]` como tabela de PCBs; pilha própria por tarefa (`STACK_SIZE`); `InitProcess` monta manualmente o quadro inicial da pilha; `timer_stub` (*naked*) salva registradores, chama `Scheduler(rsp)` e restaura o contexto devolvido; preempção por interrupção periódica de *timer*; exclusão mútua por `InterlockedCompareExchange` (*spinlock*). |
| `printer.c` | Yuri | Processo auxiliar que publica seu PID em arquivo, "dorme" com `raise(SIGSTOP)`, é "acordado" por `SIGCONT`, consome um arquivo de *spool* em região crítica protegida por semáforo nomeado. |
| `cliente.c` (anexo do enunciado) | Yuri | Contrato do processo Cliente: escreve sua demanda em `demanda.txt`, para em `SIGSTOP`, e após ser continuado executa sua demanda dentro do semáforo `/sem_atend`. |
| Slides "Implementação de Processos", "Escalonamento", "Condições de Disputa", "IPC" | Fernando | Vocabulário formal: PCB/TCB, contexto, *dispatcher* × escalonador, quantum, preempção, região crítica, `enter`/`leave`, exclusão mútua. |
| `coordenacao/me4-tsl.c`, `me5-xchg.c` (Maziero) | Fernando | Exclusão mútua por instrução atômica (TSL/XCHG) com espera ocupada. |
| Livro-texto (Maziero, *SO: Conceitos e Mecanismos*) | — | Referência teórica comum às duas frentes. |

## 2. Decisão

O projeto adotará, como regra de prioridade para toda decisão técnica:

1. **O enunciado** do Trabalho 1 é a especificação normativa. Nada que ele proíba será usado;
   nada que ele exija será omitido.
2. **O código do Prof. Yuri** (`kernel.c`, `printer.c`, `cliente.c`) é o padrão de referência
   para *como* resolver: a estrutura do escalonador, da troca de contexto, do processo
   "dormente" e do uso de arquivos/sinais seguirá a mesma lógica, nomes e organização,
   adaptados do ambiente UEFI para o espaço de usuário Linux.
3. **Os slides e o livro** fornecem o vocabulário e a justificativa teórica utilizados nos
   comentários e na documentação.
4. **Práticas de indústria** (eficiência máxima, arquitetura "limpa", abstrações genéricas) só
   serão adotadas quando não conflitarem com os itens anteriores e trouxerem ganho mensurável
   na métrica de avaliação (taxa de satisfação e tempo de execução).

Consequências práticas imediatas:

- Não serão usados `pthread_*`, `ucontext` (`getcontext`/`makecontext`/`swapcontext`),
  OpenMP, bibliotecas de corrotinas, nem filas de mensagens POSIX. *(A confirmar com o
  professor quanto a `ucontext` e `setjmp/longjmp` — ver §5.)*
- A troca de contexto será escrita em *assembly* x86-64 embutido, espelhando `timer_stub`.
- A preempção será obtida por sinal periódico (`setitimer` + `SIGALRM`), análogo direto da
  interrupção do APIC *timer* configurada em `SetupApicTimer`.
- O processo "Analista" seguirá o modelo de `printer.c` (PID em arquivo, `SIGSTOP`/`SIGCONT`,
  semáforo nomeado protegendo o arquivo compartilhado).
- O processo "Cliente" será usado **exatamente como fornecido**, sem modificações.
- Comentários poderão explicar **o quê** o código faz (e não apenas o porquê), relacionando
  cada trecho ao conceito teórico correspondente. A legibilidade didática prevalece sobre a
  concisão.

## 3. Alternativas consideradas

| Alternativa | Motivo da rejeição |
|---|---|
| `pthread` + `pthread_mutex`/`pthread_cond` | Vedado pelo enunciado ("não é permitido o uso de bibliotecas que ajudem no paralelismo") e não demonstra o escalonador. |
| `ucontext` (`swapcontext`) | Reduz a troca de contexto a uma chamada de biblioteca, afastando-se do `timer_stub` mostrado em aula; possivelmente enquadrado na vedação. |
| Escalonamento puramente cooperativo (sem *timer*) | Viável, mas diverge do modelo preemptivo apresentado no `kernel.c`. Poderá ser reavaliado em ADR própria caso a preempção prejudique o desempenho. |
| Arquitetura modular genérica (camadas, interfaces abstratas) | Aumenta a distância entre o código entregue e o código de aula, dificultando a arguição na apresentação. |

## 4. Consequências

**Positivas**
- Coerência direta com o que será arguido na apresentação de 13/11/2026.
- Cada decisão é rastreável a um material de aula, facilitando a defesa oral.
- Documentação e comentários servem também como material de estudo para a prova.

**Negativas / riscos**
- Código mais extenso e verboso do que uma solução de indústria.
- Troca de contexto em *assembly* e manipulação de sinais aumentam a superfície de erros
  sutis (alinhamento de pilha, funções não reentrantes chamadas dentro do tratador de sinal).
- A nota é competitiva por desempenho; a fidelidade não pode custar a corretude da métrica.
  Otimizações serão registradas em ADRs específicas, sempre justificadas pelo material.

## 5. Questões em aberto (a confirmar com o professor)

1. ~~Diagrama em anexo~~ — **resolvida**: é a página 2 do PDF do enunciado; incorporado ao
   `architecture.md`.
2. `setjmp`/`longjmp` ou `ucontext` são considerados "bibliotecas que ajudam no paralelismo"?
   (Decisão provisória: não usar.)
3. ~~Semáforos nomeados no Atendimento~~ — **resolvida**: o diagrama determina que a Recepção
   crie `/sem_atend` e `/sem_block`, e que o Atendente e o Analista os utilizem.
4. ~~Medição do tempo de atendimento~~ — **resolvida** (diagrama, notas 4 e 6): hora de
   chegada em ms desde o início do programa; satisfeito se
   `(tempo atual − hora de chegada) ≤ paciência`.
