# Arquitetura do Trabalho 01

## 1. Estado

Arquitetura em definição. O enunciado integral foi incorporado e a linguagem e as ferramentas foram decididas. A estrutura definitiva de classes será fechada após a especificação dos casos de teste.

A política de documentação e decisão está registrada no [ADR 0001](adrs/0001-governanca-e-fontes-do-projeto.md).

A escolha de linguagem, ferramentas e diretrizes de implementação está registrada no [ADR 0002](adrs/0002-linguagem-ferramentas-e-visibilidade-dos-algoritmos.md).

A adoção de interface gráfica está registrada no [ADR 0003](adrs/0003-interface-grafica.md).

O contrato de entrada e a saída textual estão registrados nos [ADRs 0005](adrs/0005-contrato-de-entrada.md) e [0006](adrs/0006-formato-da-saida-textual.md).

## 2. Direcionadores confirmados

- Correção dos algoritmos de escalonamento.
- Legibilidade e capacidade de explicação pela equipe.
- Testabilidade determinística.
- Rastreabilidade às notas do professor.
- Execução reproduzível.
- Separação entre regra de domínio e apresentação, quando compatível com o enunciado.
- Visibilidade do fluxo teórico de cada algoritmo.

## 3. Plataforma decidida

- Linguagem: Java 21.
- Construção: Gradle Wrapper.
- Testes automatizados: JUnit 5.
- Estilo principal: implementação imperativa, explícita e orientada à exposição das regras de escalonamento.

## 4. Responsabilidades conceituais

As responsabilidades abaixo orientam a análise, mas ainda não determinam arquivos, classes ou módulos:

- representação de processo e de seus atributos;
- coleção de processos prontos;
- política de seleção do próximo processo;
- controle do tempo e das transições da simulação;
- coleta das métricas solicitadas;
- leitura de entrada e apresentação de resultados;
- testes com resultados esperados independentes da implementação.

### 4.1 Estrutura implementada no T05

- `ProcessControlBlock`: mantém identidade, tempos, prioridades e estado mutável de um processo simulado.
- `ProcessState`: explicita os estados `NEW`, `READY`, `RUNNING` e `TERMINATED`.
- `ProcessInputParser`: valida e converte as linhas de `stdin`, sem reordenar os processos.
- `ConfigurationParser`: valida as chaves `quantum` e `aging` do arquivo textual.
- `SchedulerConfiguration`: representa uma configuração já validada.

O ponto de entrada coordenará diretamente os dois parsers quando os algoritmos forem integrados. Não há camada intermediária de carregadores, pois ela apenas repetiria chamadas sem acrescentar regra de domínio.

As transições permanecem explícitas no PCB. Aging somente pode ser aplicado no estado `READY`, e a prioridade dinâmica somente pode ser restaurada após o processo entrar em `RUNNING`.

### 4.2 FCFS

`FcfsScheduler` implementa diretamente o ciclo teórico:

1. admite processos cujo instante de criação foi alcançado;
2. registra CPU ociosa quando não existe processo pronto;
3. seleciona o processo pronto com chegada mais antiga;
4. aplica os desempates do enunciado;
5. executa o processo até o término, sem preempção;
6. registra o término e volta à seleção.

`SchedulingResult` contém a linha do tempo por segundo e os PCBs resultantes. Cada execução utiliza cópias novas dos PCBs para que os sete algoritmos possam receber a mesma entrada sem compartilhar estado mutável.

A escolha aleatória somente é consultada quando chegada e tempo restante permanecem empatados. Enquanto o FCFS executa um processo, novas chegadas são admitidas na fila, mas não provocam preempção.

### 4.3 Apoio comum aos escalonadores

`SchedulingSupport`, visível somente no pacote `scheduler`, concentra operações que não participam da política de seleção:

- cópia dos PCBs de entrada, garantindo que cada algoritmo receba estado novo;
- admissão dos processos cujo instante de criação foi alcançado, na ordem dos identificadores;
- sorteio entre candidatos ainda empatados, com os candidatos ordenados por identificador antes da consulta ao `Random`.

A seleção, a execução, a preempção e a atualização das filas permanecem no laço de cada escalonador.

### 4.4 Algoritmos não preemptivos: SJF e prioridade

`SjfScheduler` e `NonPreemptivePriorityScheduler` seguem o mesmo ciclo do FCFS e diferem apenas no critério de seleção:

- SJF escolhe a menor duração. Como nenhum processo é interrompido, duração e tempo restante coincidem no momento da escolha; empates seguem direto para o sorteio.
- A prioridade não preemptiva escolhe a maior prioridade estática, depois o menor tempo restante e, por fim, o sorteio.

Nos dois casos, chegadas durante a execução apenas entram na lista de prontos.

### 4.5 Algoritmos preemptivos: SRTF e prioridade

`SrtfScheduler` e `PreemptivePriorityScheduler` mantêm o processo em execução em uma variável própria, separada da lista de prontos, e reavaliam a decisão a cada segundo:

1. admitem as chegadas do instante;
2. preemptam o processo em execução somente se existir pronto estritamente melhor (menor tempo restante ou maior prioridade estática);
3. se a CPU estiver livre, selecionam entre os prontos: melhor critério, depois menor tempo restante e, por fim, sorteio;
4. executam um segundo e registram o término quando o tempo restante chega a zero.

O critério estrito implementa o primeiro desempate do enunciado: em empate, quem já ocupa a CPU permanece.

### 4.6 Round-Robin

`RoundRobinScheduler` usa uma `ArrayDeque` como fila FIFO. A cabeça da fila executa por até um quantum. Chegadas são admitidas a cada segundo, inclusive no limite do quantum, antes que o processo preemptado volte ao final da fila. Como a ordem da fila é a regra de seleção, o algoritmo não recebe `Random`.

### 4.7 Round-Robin com prioridade e aging

`PriorityRoundRobinScheduler` mantém uma lista de prontos e só decide nos limites do quantum:

1. seleciona a maior prioridade dinâmica, depois o menor tempo restante e, por fim, o sorteio;
2. restaura a prioridade dinâmica do selecionado para o valor estático;
3. executa por até um quantum, admitindo chegadas sem preemptar;
4. se o quantum foi consumido por completo, aplica aging aos prontos e só então admite quem chegou exatamente naquele instante;
5. recoloca o processo entre os prontos, sem preferência no desempate seguinte, ou registra seu término.

As regras dos passos 4 e 5 estão justificadas no ADR 0004.

## 5. Regras arquiteturais

1. A política de escalonamento não deve depender diretamente do formato de entrada ou saída.
2. Regras de desempate devem ser explícitas e testáveis.
3. Estado global mutável deve ser evitado quando dificultar isolamento dos testes.
4. A execução de um mesmo caso deve produzir o mesmo resultado, salvo exigência expressa em contrário.
5. Extensões para novos algoritmos não devem exigir duplicação do núcleo da simulação, desde que a abstração resultante permaneça legível.
6. Padrões de projeto somente serão empregados para resolver um problema identificado; seu uso não é objetivo independente.
7. Otimizações não devem preceder evidência de necessidade nem comprometer clareza.
8. A seleção, execução, preempção e atualização das filas devem permanecer visíveis na implementação de cada algoritmo.
9. Reuso de código não justifica ocultar diferenças teóricas entre os algoritmos.
10. Os testes e resultados esperados precedem a implementação correspondente.
11. Comentários devem explicar motivações, restrições ou decisões não evidentes; não devem repetir o comportamento já expresso pelo código.

## 6. Decisões pendentes

- tecnologia da interface gráfica;
- estrutura definitiva de módulos e diretórios.

Cada decisão relevante será registrada em ADR após confronto com o enunciado e as notas do professor.
