# Arquitetura — Trabalho 1 (Prof. Yuri Lenon)

> Documento vivo. Itens marcados com *(provisório)* dependem de ADR futura.
> Princípio norteador: [ADR-0001 — Fidelidade ao material teórico](adrs/ADR-0001-fidelidade-ao-material-teorico.md).
> Fonte normativa: enunciado (`Trabalho final SO.pdf`, p. 1), **diagrama do professor (p. 2)**
> e `cliente.c` (p. 3).

## 1. Requisitos (síntese do enunciado e do diagrama)

| ID | Requisito | Fonte |
|---|---|---|
| R1 | Programa em C, no Linux, sem bibliotecas que auxiliem paralelismo ou IPC. | Enunciado |
| R2 | Processo **Atendimento** com ao menos as *threads* **Atendente** (Thread 1) e **Recepção** (Thread 2). Outras *threads* podem tratar a fila. | Enunciado; diagrama, nota 2 |
| R3 | *Threads* criadas por **troca de contexto**, com **escalonador próprio**. | Enunciado |
| R4 | Entradas: `N` (nº de clientes; `0` = infinito) e `X` (paciência, em ms). | Enunciado; nota 8 |
| R5 | Recepção cria os semáforos `/sem_atend` e `/sem_block` e cria os clientes, com prioridade aleatória 50/50: alta (paciência `X/2`) ou baixa (paciência `X`). | Diagrama; nota 3 |
| R6 | Cada elemento da fila contém **PID, hora de chegada, prioridade e tempo para atendimento**. A estrutura de dados é livre. | Diagrama; nota 2 |
| R7 | **Hora de chegada** = tempo, em ms, entre o início do programa e a criação do cliente. | Nota 4 |
| R8 | Cliente **satisfeito** ⇔ `(tempo atual − hora de chegada) ≤ paciência`. | Nota 6 |
| R9 | O PID de cada cliente atendido é escrito no arquivo **LNG** (Lista de Números Gerados), protegido por `/sem_block`. | Diagrama |
| R10 | Processo **Analista**: grava seu PID em arquivo e dorme; ao ser acordado, bloqueia o LNG, imprime os 10 primeiros valores, apaga-os, desbloqueia e dorme. A estratégia de quando acordá-lo é livre. | Diagrama; nota 1 |
| R11 | `N > 0`: termina quando os `N` clientes forem atendidos; imprime taxa de satisfação e tempo total. | Enunciado; nota 8 |
| R12 | `N = 0`: Recepção cria clientes indefinidamente, **pausando quando a fila tiver 100 clientes** e retomando quando houver espaço. A tecla `s` encerra; a taxa considera só os atendidos; o LNG deve ser esvaziado antes da saída. | Enunciado; nota 9 |
| R13 | Arquivo **Demanda** único, compartilhado por todos os clientes; escrito pelo cliente, lido pelo Atendimento na inserção na fila; pode ser apagado após a leitura. | Diagrama; nota 7 |
| R14 | Avaliação competitiva por taxa de satisfação e tempo de execução. | Enunciado |

## 2. Visão geral (segundo o diagrama do professor)

```
┌──────────────── Processo 1 — ATENDIMENTO ────────────────┐      ┌──── Processo 2 — ANALISTA ────┐
│                                                          │      │ grava seu PID em arquivo; dorme│
│  Thread 2 — Recepção          Fila de clientes           │      │ ao acordar:                    │
│  · cria /sem_atend, /sem_block  {PID, chegada,           │      │  bloqueia LNG (/sem_block)     │
│  · cria clientes (fork/exec)     prioridade, demanda}    │      │  imprime 10 primeiros PIDs     │
│  · lê Demanda, sorteia prio ──►  [ ][ ][ ][ ][ ] ──┐     │      │  apaga-os; desbloqueia LNG     │
│                                                    ▼     │      │  dorme novamente               │
│  Thread 1 — Atendente                                    │      └───────────────▲────────────────┘
│  · lê próximo cliente da fila                            │                      │ SIGCONT
│  · acorda o cliente (SIGCONT) ───────────────────────────┼──► Processo 3        │
│  · espera /sem_atend abrir                               │    CLIENTE           │
│  · espera /sem_block; escreve PID no LNG; libera         │    (fornecido)       │
│  · calcula satisfação; tira o cliente da fila            │                      │
│  · acorda o Analista ────────────────────────────────────┼──────────────────────┘
│                                                          │
│  Núcleo: TCB[], pilhas, TrocaContexto, Escalonador      │
└──────────────────────────────────────────────────────────┘
   Arquivos: Demanda (cliente → Recepção), LNG (Atendente → Analista), PID do Analista
```

## 3. Componentes

### 3.1 Processo Atendimento — núcleo de *threads* em espaço de usuário

Espelha `protokernel/aulafinal/kernel.c`, transposto do UEFI para o Linux:

| Conceito (aula) | `kernel.c` (UEFI) | Atendimento (Linux) |
|---|---|---|
| PCB/TCB | `PROCESS Process[2]` | `THREAD Thread[MAX_THREADS]` com `Rsp`, `Pilha`, `Estado`, `Entrada`, `Argumento` |
| Pilha própria | `kalloc(STACK_SIZE)` | área estática de `TAMANHO_PILHA` bytes por *thread* |
| Contexto inicial | `InitProcess` monta o quadro da pilha | `CriaThread` monta o quadro da pilha |
| Interrupção de *timer* | APIC x2, vetor 32, periódico | `ConfiguraTemporizador`: `setitimer(ITIMER_REAL)` + `SIGALRM` (ADR-0004) |
| Salvamento de contexto | `timer_stub` (*naked*) | `TrocaContexto` (*naked*), chamada por `CedeProcessador` e `TratadorTemporizador` |
| Escalonador | `Scheduler(CurrentRsp)` devolve novo `rsp` | `Escalonador(RspAtual)`, mesma semântica |
| Exclusão mútua interna | `Acquire/Release` (`InterlockedCompareExchange32`) | `EntraRegiaoCritica/SaiRegiaoCritica` com instrução atômica (modelo `me4-tsl.c`) |

### 3.2 Thread 2 — Recepção

1. Cria `/sem_atend` e `/sem_block` (antes do primeiro cliente; sem eles o cliente ignora o semáforo).
2. Cria o cliente com `fork()` + `execve()`.
3. Aguarda o cliente parar (`SIGSTOP`), garantindo que o arquivo Demanda foi escrito; lê a demanda.
4. Registra hora de chegada (ms desde o início), sorteia a prioridade e insere na fila.
5. Com `N = 0`, suspende a criação enquanto a fila tiver 100 clientes.

### 3.3 Fila de clientes

Estrutura própria, compartilhada entre as *threads* e acessada somente dentro de
`EntraRegiaoCritica()`/`SaiRegiaoCritica()`. A **ordem de retirada** é a principal alavanca de desempenho e será
decidida em ADR própria, por medição.

### 3.4 Thread 1 — Atendente

Segue literalmente a sequência do diagrama:
lê o próximo cliente → acorda o cliente → espera `/sem_atend` → espera `/sem_block` →
escreve o PID no LNG → libera `/sem_block` → calcula a satisfação → retira o cliente da fila →
acorda o Analista (estratégia de despertar a definir).

### 3.5 Processo 2 — Analista

Modelo de `printer.c`: grava seu PID em arquivo e executa `raise(SIGSTOP)`. Ao receber
`SIGCONT`: `sem_wait(/sem_block)` → lê e imprime até 10 PIDs do LNG → reescreve o LNG sem eles
→ `sem_post(/sem_block)` → `raise(SIGSTOP)`.

### 3.6 Processo 3 — Cliente

Fornecido e **inalterado** (nota 5: todas as equipes usam o mesmo). Contrato:
- sorteia `tempo ∈ {15, 5, 1}` com probabilidades 10 %, 30 %, 60 %;
- escreve `tempo` no arquivo Demanda (`demanda.txt`) e executa `raise(SIGSTOP)`;
- ao ser continuado: `sem_wait(/sem_atend)`, `usleep(tempo)` (microssegundos), `sem_post(/sem_atend)`.

## 4. Encerramento e métricas

- **Taxa de satisfação** = satisfeitos / atendidos; **tempo total** medido com relógio monotônico.
- `N = 0`: leitura de `s` com terminal em modo não canônico e leitura não bloqueante. Antes
  de sair, o Analista é acordado até o LNG ficar vazio.
- Limpeza: `sem_unlink`, remoção de arquivos temporários, término do Analista e coleta dos
  filhos (evitar zumbis).

## 5. Estrutura de diretórios prevista

```
Trabalho-Yuri-1/
├── docs/            board.md, architecture.md, adrs/
├── src/
│   ├── atendimento.c      main, entradas, métricas
│   ├── nucleo.c / .h      TCB, CriaThread, Escalonador, TrocaContexto, região crítica
│   ├── fila.c / .h        fila de clientes
│   ├── recepcao.c         Thread 2
│   ├── atendente.c        Thread 1
│   ├── analista.c         Processo 2
│   └── cliente.c          Processo 3 (fornecido, inalterado)
├── tests/           teste.h (macro própria) e testes unitários, de núcleo e de integração (ADR-0002)
├── Makefile         alvos: (padrão), test, clean
└── README.md
```

## 6. Riscos técnicos conhecidos

| Risco | Mitigação |
|---|---|
| Funções não reentrantes (`printf`, `malloc`) chamadas a partir do tratador de `SIGALRM`. | O tratador apenas troca contexto; nenhuma E/S dentro dele. |
| Chamadas bloqueantes (`sem_wait`, `waitpid`) bloqueiam o processo inteiro, inclusive a outra *thread*. | Versões não bloqueantes (`sem_trywait`, `WNOHANG`) seguidas de cessão do processador ao escalonador. |
| Sinal interrompendo chamada de sistema (`EINTR`). | `SA_RESTART` e/ou laço de nova tentativa. |
| Disputa pelo arquivo Demanda. | Recepção cria um cliente por vez (nota 7). |
| Alinhamento de pilha (16 bytes, ABI System V). | Replicar o `& ~15` de `InitProcess`. |
