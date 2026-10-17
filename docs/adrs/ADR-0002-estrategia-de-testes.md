# ADR-0002 — Estratégia de testes

- **Status:** Aceita
- **Data:** 2026-10-17
- **Relacionada a:** [ADR-0001 — Fidelidade ao material teórico](ADR-0001-fidelidade-ao-material-teorico.md)

## 1. Contexto

Os principais defeitos possíveis neste trabalho são **erros dinâmicos**: corrupção de pilha na
troca de contexto, condições de disputa na fila, perda de sinais, recursos não liberados. Como
apresentado nos slides de Condições de Disputa, esses erros não são visíveis no código-fonte,
manifestam-se apenas em certos entrelaçamentos de execução e podem demorar a ocorrer. A
verificação manual é, portanto, insuficiente.

Além disso, a avaliação é competitiva e baseada em métricas (taxa de satisfação e tempo total),
que precisam ser medidas de forma repetível para fundamentar decisões de projeto.

## 2. Decisão

1. **Diretório `tests/` separado de `src/`.** Nenhuma linha de teste no código de produção; o
   código apresentado permanece enxuto. Os testes são mantidos no repositório como evidência
   de corretude.
2. **Sem framework de testes.** Utiliza-se `assert.h` e uma macro própria, curta, em
   `tests/teste.h`, que registra a falha (arquivo, linha, expressão), **não aborta** e, ao final,
   imprime o resumo (`passaram/total`) e retorna código de saída diferente de zero se houver falha.
3. **Alvos do `Makefile`:** `make` compila apenas o trabalho; `make test` compila e executa
   todos os testes; `make clean` remove artefatos.
4. **Três níveis de teste:**

   | Nível | Escopo | Exemplo de critério |
   |---|---|---|
   | Unitário | Fila, cálculo de hora de chegada e de satisfação | Ordem de retirada correta; satisfeito ⇔ `(agora − chegada) ≤ paciência` |
   | Núcleo | Troca de contexto, preempção, exclusão mútua | Duas *threads* incrementam um contador 10⁶ vezes cada; total exatamente 2·10⁶ |
   | Integração | Programa completo | `./atendimento N X` termina; Analista imprime os `N` PIDs; nenhum semáforo remanescente em `/dev/shm`; nenhum processo zumbi |

5. **Separação entre detecção e depuração.**
   - *Detecção*: testes de volume (milhares de iterações) aumentam a probabilidade de exercitar
     o entrelaçamento defeituoso; o resultado é binário.
   - *Depuração*: ao falhar, o teste informa a primeira iteração divergente; o caso é
     reproduzido no **modo cooperativo** (determinístico, troca apenas em `CedeProcessador`); se o defeito
     depender da preempção, varia-se o quantum para isolá-lo; por fim, usam-se `gdb` e
     `valgrind` sobre o caso reduzido.
6. **Restrição herdada da ADR-0001:** os testes também não utilizam bibliotecas de
   paralelismo (por exemplo, `pthread`), evitando qualquer ambiguidade na avaliação.

## 3. Alternativas consideradas

| Alternativa | Motivo da rejeição |
|---|---|
| Framework (Check, Criterion, CUnit, Unity) | Check e Criterion isolam testes via `fork` e podem usar *threads*/sinais internamente, interferindo justamente nos mecanismos sob teste; exigem instalação por todos os membros e pelo professor; volume previsto (≈ 15–25 casos) não justifica. |
| Apenas testes manuais | Insuficientes para erros dinâmicos; não produzem medições repetíveis. |
| Testes dentro de `src/` (por exemplo, `#ifdef TEST`) | Polui o código apresentado. |
| Remover os testes antes da entrega | Perde-se a evidência de corretude útil na arguição. |

## 4. Consequências

**Positivas**
- Erros de concorrência detectados cedo e de forma reproduzível.
- Métricas de desempenho medidas por *script*, alimentando a ADR da política da fila.
- Nenhuma dependência além de `gcc` e `make`.

**Negativas**
- A macro própria oferece menos recursos que um framework (sem *fixtures*, sem *mocks*,
  sem isolamento por processo).

## 5. Critério de revisão

Esta decisão deve ser reavaliada, por meio de nova ADR, se o número de casos ultrapassar
aproximadamente 50, ou se surgir necessidade de *fixtures* complexas ou de simulação de
componentes.
