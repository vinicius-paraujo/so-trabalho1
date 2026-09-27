# ADR 0004: Convenções de simulação e testes

- Status: Aceita
- Data: 20/09/2026
- Responsável pela proposta: Marcos

## Contexto

O enunciado define algoritmos, entradas, métricas e desempates, mas não especifica todas as convenções necessárias para produzir resultados determinísticos. Sem essas convenções, implementações corretas podem divergir na linha do tempo, na contagem de trocas ou na saída numérica.

## Decisão proposta

1. O simulador utilizará tempo discreto e registrará um processo por intervalo `[t, t + 1)`.
2. Processos serão identificados pela ordem das linhas originais da entrada.
3. Chegadas no instante `t` serão consideradas antes da decisão correspondente a `[t, t + 1)`.
4. Turnaround será calculado por `conclusão - criação`; espera será calculada por `turnaround - duração`.
5. Médias não sofrerão arredondamento interno e serão apresentadas com duas casas decimais.
6. A linha do tempo representará ausência de processo pronto por `OCIOSO`.
7. Somente a substituição direta de um processo por outro contará como troca de contexto. Carga inicial e transições envolvendo `OCIOSO` não serão contabilizadas.
8. No Round-Robin, a ordem da fila é parte do algoritmo e não constitui empate. Ao consumir o quantum sem concluir, o processo deixa a CPU e retorna ao final da fila.
9. O desempate aleatório receberá uma instância de `java.util.Random`. Testes usarão semente `0`; a execução normal não fixará semente.
10. No Round-Robin prioritário, o aging ocorrerá ao término de cada quantum completo e afetará processos que aguardam. Não ocorrerá na inicialização nem após término antecipado.
11. A chegada de processo mais prioritário não interromperá o quantum em andamento no Round-Robin prioritário.

### Complemento de 26/09/2026 (T07–T12)

Regras que surgiram na implementação dos algoritmos e que o enunciado não fixa:

12. Nos algoritmos preemptivos (SRTF e prioridade preemptiva), a preempção exige candidato estritamente melhor. Em empate, o processo em execução permanece, aplicando o primeiro desempate do enunciado.
13. No Round-Robin prioritário, o processo que esgota o quantum deixa a CPU e disputa a seleção seguinte sem a preferência de "processo que já ocupa a CPU". A escolha é feita por maior prioridade dinâmica, depois menor tempo restante e, por fim, sorteio. Essa leitura é a única compatível com o CT-05 e é coerente com o item 8.
14. No Round-Robin prioritário, o aging é aplicado quando o quantum é consumido por completo, mesmo que o processo termine exatamente no limite. Somente o término com menos segundos que o quantum dispensa o aging. O caso de referência do enunciado só produz a linha do tempo esperada com esta regra (término de `P2` em `t = 2`).
15. No Round-Robin prioritário, um processo que chega exatamente no limite do quantum não recebe o aging daquele limite, pois ainda não aguardou. Processos que chegaram durante o quantum recebem o aging (CT-12).
16. As prioridades dos algoritmos de prioridade sem Round-Robin usam somente a prioridade estática, pois o aging é exigido apenas no Round-Robin prioritário.

## Relação com as fontes

- A ordem de desempate e a ausência de preempção por prioridade no Round-Robin prioritário vêm do enunciado.
- O uso de prioridades estática e dinâmica e o incremento da prioridade de quem aguarda vêm das notas de aula.
- A interpretação da fila Round-Robin preserva a rotação demonstrada pelo diagrama do enunciado.
- As demais regras resolvem pontos não especificados e, por isso, constituem decisões da equipe.

## Consequências

- Os resultados se tornam reproduzíveis em testes e entre os ambientes dos integrantes.
- A contagem de trocas de contexto possui definição objetiva.
- O comportamento do aging em término antecipado fica explicitamente limitado.
- Caso o professor determine convenção diferente, os oráculos, esta decisão e as implementações afetadas deverão ser atualizados em conjunto.

## Verificação

Os resultados esperados devem ser confrontados com as fórmulas, invariantes e casos específicos registrados em `docs/testing.md`. Revisão por outro integrante é recomendada, mas não bloqueia os cards de infraestrutura.
