# ADR-0005 — Nomenclatura em português

- **Status:** Aceita
- **Data:** 2026-10-17
- **Relacionada a:** [ADR-0001](ADR-0001-fidelidade-ao-material-teorico.md)

## 1. Contexto

O núcleo de *threads* havia sido escrito com identificadores em inglês, por proximidade com o
`kernel.c` do professor, enquanto o domínio do problema usava português. Na prática, os idiomas se
misturavam até dentro de uma mesma estrutura (`Rsp` e `Estado`, `Yield` e `ThreadsAtivas`), e o
leitor não conseguia prever o idioma de um nome.

Fidelidade ao material (ADR-0001) significa seguir **a lógica e a estrutura** do que o professor
ensinou, não reproduzir literalmente seus identificadores. Uma tradução consistente preserva o mesmo
código: `Scheduler(CurrentRsp)` e `Escalonador(RspAtual)` são o mesmo algoritmo.

## 2. Decisão

1. Todos os identificadores do projeto (funções, variáveis, tipos, constantes, arquivos-fonte)
   são escritos em **português**, sem acentos.
2. Exceções, por serem termos técnicos sem tradução corrente ou usados assim no próprio
   enunciado e nos slides:
   - **`thread`** (o enunciado usa "threads");
   - **nomes de registradores** (`Rsp`, `rax`…) e de instruções;
   - **`quantum`** (termo dos slides de escalonamento);
   - **APIs do sistema e da biblioteca C** (`fork`, `sigprocmask`, `setitimer`…), que não são nossas;
   - nomes de diretórios convencionais (`src/`, `tests/`, `docs/`).
3. Comentários e mensagens impressas: português.
4. Correspondência com o material do professor, para a arguição:

   | `kernel.c` (professor) | Projeto |
   |---|---|
   | `PROCESS Process[]` | `THREAD Thread[]` |
   | `STACK_SIZE` | `TAMANHO_PILHA` |
   | `CurrentProcess` | `ThreadAtual` |
   | `Scheduler(CurrentRsp)` | `Escalonador(RspAtual)` |
   | `timer_stub` | `TrocaContexto` (chamada por `TratadorTemporizador`) |
   | `InitProcess` | `CriaThread` |
   | `SetupApicTimer` | `ConfiguraTemporizador` |
   | `Acquire` / `Release` | `EntraRegiaoCritica` / `SaiRegiaoCritica` (vocabulário `enter`/`leave` dos slides) |
   | `cli` / `sti` | `DesabilitaInterrupcoes` / `HabilitaInterrupcoes` |

## 3. Alternativas consideradas

| Alternativa | Motivo da rejeição |
|---|---|
| Inglês no núcleo, português no domínio | Fronteira artificial; a tradução não afasta o código do material. |
| Tudo em inglês | Afasta o código do enunciado e dos slides. |

## 4. Consequências

- Nomes uniformes e previsíveis em todo o projeto.
- A correspondência com o `kernel.c` deixa de ser literal; a tabela acima registra o mapeamento.
