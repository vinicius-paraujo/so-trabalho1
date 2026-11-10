# ADR-0010 — Política de retirada da fila: prazo mais próximo entre os ainda satisfazíveis

- **Status:** Aceita
- **Data:** 2026-11-09
- **Relacionada a:** [ADR-0002](ADR-0002-estrategia-de-testes.md), [ADR-0008](ADR-0008-fim-do-atendimento-por-waitpid.md)

## 1. Contexto

A avaliação é competitiva pela taxa de satisfação (R14). Um cliente está satisfeito quando
`(término − chegada) ≤ paciência`, com paciência `X/2` (alta) ou `X` (baixa). A estrutura da fila
é livre (R6), e a **ordem de retirada** é a única decisão que muda quem fica satisfeito sem mexer
no resto do sistema. Até aqui, a retirada era por ordem de chegada (FIFO).

Medições iniciais mostraram dois regimes:

- **Sem disputa por CPU** (8 núcleos): o Atendente atende mais rápido do que a Recepção cria
  clientes; a fila quase nunca tem mais de um cliente e a política é irrelevante (todas ≈ 100 %
  para `X ≥ 5`).
- **Com disputa por CPU** (simulada com `taskset -c 0`, um núcleo): a Recepção enche a fila
  (100) e o atendimento vira gargalo. Aqui o FIFO é desastroso: atende sempre o cliente mais
  antigo, que já esperou demais, e todos chegam atrasados em cascata.

Como não se sabe a carga da máquina de avaliação, a política precisa ser boa no segundo regime
sem piorar o primeiro.

## 2. Decisão

1. **Prazo** de um cliente = `HoraChegada + paciência` — o último instante em que o atendimento
   pode terminar com ele satisfeito.
2. O Atendente estima quando o próximo atendimento terminará: `agora + duração média`, sendo a
   duração média uma média móvel exponencial (peso 1/8) do tempo entre a retirada e o término dos
   atendimentos anteriores.
3. `RetiraFila` recebe essa previsão e retira, **entre os clientes cujo prazo ≥ previsão**, o de
   **menor prazo** (EDF — *earliest deadline first*).
4. Se nenhum cliente ainda pode ser satisfeito, retira o mais antigo. Esses clientes serão
   insatisfeitos de qualquer forma; atendê-los antes dos demais só atrasaria quem ainda pode ser
   salvo.
5. A fila continua sendo o vetor de 100 posições; a escolha é uma busca linear dentro da região
   crítica, e a remoção desloca os elementos seguintes. Com no máximo 100 clientes, o custo é
   desprezível perto do `fork`/`waitpid` de cada atendimento.
6. A paciência passou a ser calculada em um só lugar (`PacienciaCliente`, em `fila.c`), usado
   tanto pela fila quanto por `ClienteSatisfeito`.

## 3. Alternativas consideradas (medidas)

`N = 1000`, média de 5 execuções por célula. Taxa de satisfação; tempo total semelhante entre
políticas em todos os cenários (≈ 1,7 s com um núcleo; ≈ 0,5 s com oito).

| Política | 1 núcleo, X=10 | X=30 | X=100 | X=300 | 8 núcleos, X=1 | X=2 |
|---|---|---|---|---|---|---|
| FIFO (anterior) | 0,029 | 0,064 | 0,348 | 0,766 | 0,645 | 0,995 |
| EDF entre satisfazíveis, sem previsão (`agora`) | 0,710 | 0,817 | 0,886 | 1,000 | 0,643 | 0,991 |
| **EDF entre satisfazíveis, previsão = agora + média** | **0,737** | **0,882** | **0,911** | **1,000** | 0,639 | 0,995 |
| EDF entre satisfazíveis, previsão = agora + 3 × média | 0,419 | 0,901 | 0,932 | 1,000 | 0,634 | 0,985 |
| LIFO (mais recente primeiro) | 0,756 | 0,878 | 0,894 | 0,899 | 0,643 | 0,993 |

Em uma rodada preliminar (3 execuções), também foram medidas:

| Política | 1 núcleo, X=10 | X=50 | X=200 |
|---|---|---|---|
| Prioridade alta primeiro (FIFO dentro da prioridade) | 0,613 | 0,581 | 0,647 |
| EDF puro (inclui clientes já perdidos) | 0,432 | 0,408 | 0,945 |

Observações:

- **EDF puro** sofre o efeito dominó clássico: sob sobrecarga, gasta o atendimento com clientes
  cujo prazo já passou.
- **Prioridade alta primeiro** ignora há quanto tempo cada um espera; clientes de prioridade
  baixa expiram em massa.
- **LIFO** é bom sob sobrecarga extrema (atende quem acabou de chegar, com mais folga), mas
  nunca alcança 100 %: com folga de sobra (`X = 300`), os primeiros a chegar ficam esquecidos no
  fundo da fila.
- **Margem de 3 × a média** é pessimista demais com paciência curta: descarta clientes que
  ainda poderiam ser salvos.
- A política escolhida fica entre as duas melhores em todos os cenários e é a única que
  alcança 100 % quando há folga.
- Com oito núcleos, as diferenças estão dentro do ruído de medição: a política não piora o caso
  sem disputa. `X = 1` é limitado pela resolução de 1 ms do relógio, não pela fila.

## 4. Consequências

**Positivas**
- Sob disputa por CPU, a taxa de satisfação sobe de 3–77 % (FIFO) para 74–100 %.
- A regra é explicável em uma frase e segue um algoritmo de escalonamento de tempo real visto em
  aula (EDF), com o ajuste de não insistir em quem já perdeu o prazo.

**Negativas**
- A ordem de retirada deixa de ser a de chegada; o teste da Recepção passou a verificar a ordem
  de inserção diretamente no vetor da fila.
- A previsão de término é uma média: atendimentos muito mais longos que a média (por exemplo,
  durante uma rajada de carga externa) ainda podem perder o prazo.

## 5. Verificação

- `tests/teste_fila.c`, `TestaOrdemDeRetirada`: retira primeiro o de prazo mais próximo mesmo
  tendo chegado depois; pula quem já não pode ser satisfeito; prazo inclusivo; sem ninguém
  satisfazível, retira o mais antigo. O teste produtor/consumidor sob preempção (mesma
  prioridade) continua exigindo a ordem de chegada.
- Medição: cada política foi compilada como um executável separado e executada com
  `taskset -c 0 ./atendimento 1000 X` (um núcleo) e `./atendimento 1000 X` (todos os núcleos),
  5 vezes por célula.

## 6. Questões em aberto

- Sob disputa por CPU, o tempo por cliente é dominado pela espera ativa (`waitpid` com `WNOHANG`
  e cessão do processador): o Atendimento consome sua fatia de CPU enquanto os clientes precisam
  dela. Ceder também a CPU ao sistema operacional quando todas as *threads* estão só consultando
  pode reduzir esse tempo, mas é uma mudança no núcleo, fora do escopo desta ADR.
