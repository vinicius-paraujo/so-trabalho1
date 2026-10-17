# ADR-0006 — Exclusão mútua por TSL com cessão do processador

- **Status:** Aceita
- **Data:** 2026-10-17
- **Relacionada a:** [ADR-0001](ADR-0001-fidelidade-ao-material-teorico.md), [ADR-0004](ADR-0004-preempcao-por-sigalrm.md)

## 1. Contexto

As *threads* compartilharão a fila de clientes e outros dados. Com preempção, uma sequência
"ler–modificar–escrever" pode ser interrompida no meio, gerando condição de disputa (slides de
Condições de Disputa). O material oferece duas referências: o `Acquire`/`Release` do `kernel.c`
(`InterlockedCompareExchange32` em laço) e o `me4-tsl.c` (`__sync_fetch_and_or` em laço, com
espera ocupada).

## 2. Decisão

```c
typedef volatile int TRAVA;   // 0 = livre, 1 = ocupada

void EntraRegiaoCritica(TRAVA *Trava)
{
	while(__sync_fetch_and_or(Trava, 1))
		CedeProcessador();
}

void SaiRegiaoCritica(TRAVA *Trava)
{
	__sync_lock_release(Trava);
}
```

1. **TSL** (`__sync_fetch_and_or`), como no `me4-tsl.c`: teste e ocupação numa única instrução
   atômica. Como a preempção (`SIGALRM`) só é entregue entre instruções, nenhuma *thread* vê a
   trava livre entre o teste e a ocupação.
2. **Cessão do processador na disputa**, em vez da espera ocupada pura do `me4-tsl.c`. Num único
   processador, a *thread* que detém a trava só a libera quando voltar a executar; girar no laço
   apenas desperdiçaria o restante do quantum.
3. **Liberação com `__sync_lock_release`**, e não `*Trava = 0`, para que o compilador não mova
   escritas da região crítica para depois da liberação.
4. A função atende aos requisitos dos slides: exclusão mútua; espera limitada (o detentor sempre
   volta a executar no Round-Robin); independência de outras tarefas e de fatores físicos.

## 3. Alternativas consideradas

| Alternativa | Motivo da rejeição |
|---|---|
| Espera ocupada pura (`me4-tsl.c` literal) | Correta, mas desperdiça o quantum inteiro a cada disputa em um único processador. |
| `DesabilitaInterrupcoes` como mecanismo de exclusão | Correta, mas custa duas chamadas de sistema por entrada/saída e bloqueia todas as *threads*, não só as que disputam o recurso. Fica reservada a trechos curtos do próprio núcleo e de chamadas não reentrantes da biblioteca C. |
| Semáforos POSIX (`sem_wait`) entre *threads* do Atendimento | Bloquearia o processo inteiro (todas as nossas *threads* compartilham uma *thread* do sistema operacional). |

## 4. Verificação

`tests/teste_regiao_critica.c`, com quantum de 100 µs:
- sem trava, um incremento com janela entre leitura e escrita **perde atualizações** — prova que
  o teste detecta a condição de disputa;
- com trava, duas *threads* × 5·10⁶ incrementos resultam exatamente em 10⁷;
- três *threads* nunca encontram outra dentro da região crítica, e a trava termina livre.

Estável em 40 execuções (20 em `-O0`, 20 em `-O2`).
