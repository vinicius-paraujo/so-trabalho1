# ADR 0002: Linguagem, ferramentas e visibilidade dos algoritmos

- Status: Aceita
- Data: 20/09/2026
- Responsável pela proposta: Marcos

## Contexto

O trabalho exige a implementação e a apresentação de sete algoritmos de escalonamento. A equipe deverá explicar estados de processos, estruturas de dados, decisões de seleção, preempções, métricas e trocas de contexto.

A redução de linhas de código ou a generalização excessiva não é um objetivo. Uma abstração que esconda o fluxo teórico pode dificultar a avaliação do professor e a defesa oral da solução.

## Alternativas consideradas

### Python

Permite implementação concisa e possui suporte adequado a testes. Entretanto, sua tipagem dinâmica e a facilidade de condensar operações podem tornar menos explícitos os estados, contratos e estruturas utilizados na simulação.

### Java

Oferece tipagem estática, classes, enumerações e coleções explícitas. Essas características favorecem a representação do controle de processos e a inspeção das estruturas empregadas por cada algoritmo. O custo é maior quantidade de código estrutural.

## Decisão

1. A aplicação será implementada em Java 21.
2. A construção será realizada pelo Gradle Wrapper.
3. Os testes automatizados utilizarão JUnit 5.
4. A implementação privilegiará fluxo imperativo e legível, com laços, decisões de seleção e transições de estado explicitamente identificáveis.
5. Cada algoritmo deverá preservar sua identidade no código. Não será criado um mecanismo genérico que oculte sua regra central de escalonamento.
6. Código comum será limitado a responsabilidades efetivamente compartilhadas, como modelo de processo, leitura, configuração, métricas e apresentação.
7. Streams, expressões funcionais ou padrões de projeto somente serão utilizados quando melhorarem a compreensão sem ocultar o procedimento teórico.
8. Os casos de teste e seus resultados esperados serão definidos antes da implementação dos algoritmos.
9. Comentários serão utilizados para explicar o motivo de decisões, restrições e comportamentos não evidentes. Comentários que apenas descrevam o que o código faz serão evitados.

## Consequências

### Positivas

- Estados e estruturas de dados ficam explícitos e verificáveis em compilação.
- A correspondência entre teoria, implementação e apresentação é facilitada.
- JUnit 5 permite testes isolados e regressão automatizada.
- O Gradle Wrapper torna compilação e testes reproduzíveis entre os integrantes.

### Custos e riscos

- A solução terá mais código estrutural que uma implementação equivalente em Python.
- A equipe deverá impedir que hierarquias de classes e padrões desnecessários introduzam complexidade.
- A separação entre código comum e código específico exigirá revisão para não esconder o fluxo dos algoritmos.

## Critérios de conformidade

- O código compila com Java 21 pelo Gradle Wrapper.
- A suíte JUnit 5 é executada pelo comando documentado do projeto.
- Um integrante consegue localizar e explicar, em cada algoritmo, seleção, execução, preempção, atualização de filas e término de processos.
- As estruturas de dados utilizadas por cada algoritmo estão justificadas na documentação técnica.
