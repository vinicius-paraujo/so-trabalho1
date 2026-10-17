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
| Exclusão mútua interna | `Acquire/Release` (`InterlockedCompareExchange32`) | `EntraRegiaoCritica/SaiRegiaoCritica`: TSL (`me4-tsl.c`) cedendo o processador na disputa (ADR-0006) |

### 3.2 Thread 2 — Recepção

Implementada em `recepcao.c`/`recepcao.h`; recebe uma estrutura `RECEPCAO` compartilhada (N, fila,
sinalizadores `SemaforosCriados`, `Encerrar` e o contador `Criados`). O relógio do programa
(`relogio.c`, `CLOCK_MONOTONIC`) fornece a hora de chegada.

1. Remove semáforos remanescentes e cria `/sem_atend` e `/sem_block` livres; marca
   `SemaforosCriados` — o Analista só deve ser criado depois disso, para não abrir um semáforo
   que será substituído.
2. Antes de cada cliente, se a fila estiver cheia (100), cede o processador até haver espaço.
   A espera ocorre **antes** da criação, de modo que a hora de chegada não inclui o tempo de fila
   cheia. Vale para `N = 0` (nota 9) e também para `N > 0`.
3. Registra a hora de chegada e cria o cliente com `fork` + `execl("./cliente")`.
4. Espera o cliente parar (`waitpid` com `WUNTRACED | WNOHANG`, cedendo o processador entre
   consultas); só então o arquivo de demanda está escrito. Um cliente por vez (nota 7).
5. Lê e apaga `demanda.txt`, sorteia a prioridade (50/50) e insere na fila.
6. Se o cliente terminar sem parar (falha no `execl`), sinaliza `Encerrar` e para, em vez de
   criar processos indefinidamente.

### 3.3 Fila de clientes

Implementada em `fila.c`/`fila.h`. Vetor de `CAPACIDADE_FILA` (100) posições de `CLIENTE`
(`Pid`, `HoraChegada`, `Prioridade`, `Demanda` — os campos do diagrama), com uma `TRAVA` própria:
toda operação (`InsereFila`, `RetiraFila`, `TamanhoFila`) é uma região crítica. As operações não
bloqueiam: devolvem 0 quando a fila está cheia ou vazia, e quem chamou decide se cede o
processador e tenta de novo.

A retirada atual é por ordem de chegada (posição 0, deslocando as demais). A **ordem de retirada**
é a principal alavanca de desempenho e será decidida em ADR própria, por medição; o vetor simples
permite retirar de qualquer posição sem mudar a estrutura.

### 3.4 Thread 1 — Atendente

Implementada em `atendente.c`/`atendente.h` (ADR-0008). Para cada cliente:

1. Retira o próximo cliente da fila (se vazia, cede o processador).
2. Acorda o cliente (`SIGCONT`) e espera seu término com `waitpid(..., WNOHANG)`, cedendo o
   processador entre consultas; o término implica que o cliente já liberou `/sem_atend`.
3. Registra a hora de término e grava o PID no LNG dentro de `/sem_block` (`sem_trywait`).
4. Satisfeito se `(término − chegada) ≤ paciência` (`X/2` para alta, `X` para baixa).
5. Acorda o Analista quando há ≥ 10 PIDs pendentes e ele está parado (ADR-0007).

O `main` de `atendimento.c` monta tudo: relógio, núcleo, fila, temporizador (quantum de 1 ms),
Recepção; cria o Analista depois que os semáforos existem; cria o Atendente; espera as *threads*;
chama `EsvaziaLng`; encerra o Analista, remove semáforos e arquivos e imprime a taxa de
satisfação e o tempo total.

### 3.5 Processo 2 — Analista

Implementado em `analista.c`, no modelo do `printer.c`: grava seu PID em `pid_analista.txt` e, em
laço, dorme (`raise(SIGSTOP)`); ao receber `SIGCONT`, entra na região crítica de `/sem_block`,
imprime até `MAX_IMPRESSOS` (10) PIDs do início de `lng.txt`, remove-os (cópia para `lng.tmp` +
`rename`) e libera o semáforo. O Atendimento só o acorda depois de confirmar, por `waitpid` com
`WUNTRACED`, que ele está parado (ADR-0007). Nomes de arquivos e semáforos compartilhados ficam em
`comum.h`.

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
