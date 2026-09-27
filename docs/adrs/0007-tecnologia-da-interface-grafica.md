# ADR 0007: Tecnologia da interface gráfica

- Status: Aceita
- Data: 27/09/2026
- Responsável pela proposta: Tiago

## Contexto

O ADR 0003 incorporou a interface gráfica ao escopo e exigiu que sua tecnologia fosse decidida antes da implementação. O enunciado concede bônus a interfaces visuais, "especialmente com animações", e cita simuladores web como referência.

Restrições já decididas:

- núcleo em Java 21, construído exclusivamente pelo Gradle Wrapper (ADR 0002);
- a interface não pode conter nem duplicar regras de seleção, preempção, aging ou métricas (ADR 0003);
- o núcleo deve continuar executável e testável sem a interface;
- a entrega deve ser reproduzível nas máquinas dos três integrantes e do professor.

## Alternativas consideradas

### Swing (JDK)

Faz parte do JDK 21 e não acrescenta dependências. Consome diretamente o `SimulationRunner`, é executado pelo mesmo Gradle Wrapper e permite desenho próprio (`Graphics2D`) para o diagrama de Gantt e para a animação. O custo é uma aparência menos moderna e mais código de layout.

### JavaFX

Oferece componentes e animações mais modernos, mas não faz parte do JDK desde o Java 11. Exigiria plugin Gradle e bibliotecas nativas específicas de cada plataforma, o que aumenta o risco de a entrega não executar na máquina do professor.

### Interface web (HTML/JavaScript)

Aproxima-se dos exemplos citados no enunciado. Entretanto, exigiria outra linguagem, um servidor ou uma exportação de dados entre Java e navegador, e uma segunda cadeia de construção. Também haveria risco de reimplementar regras no cliente.

## Decisão

1. A interface gráfica será implementada em Swing, no pacote `br.ufc.so.escalonamento.gui`.
2. A interface terá ponto de entrada próprio (`GuiMain`), executado por `gradlew runGui` ou pelo script `simulador-escalonamento-gui` gerado por `installDist`. O contrato de terminal do ADR 0005 permanece inalterado.
3. A interface obterá os resultados exclusivamente de `SimulationRunner`, isto é, a mesma lista de `AlgorithmReport` usada pela saída textual.
4. Os processos editados na interface serão validados pelo `ProcessInputParser`, e os arquivos importados, pelos mesmos parsers da aplicação de terminal.
5. A animação apenas revela, segundo a segundo, a linha do tempo já calculada. Nenhuma simulação ocorre durante a animação.
6. O estado exibido de cada processo (não criado, pronto, executando, terminado) será derivado da criação, da conclusão e da linha do tempo, sem decisões de escalonamento. Essa distinção resolve na interface a divergência visual registrada no ADR 0006.
7. A interface oferecerá semente fixa opcional para os desempates aleatórios, permitindo repetir uma demonstração.

## Consequências

### Positivas

- Nenhuma dependência nova; a interface é construída e executada pelo mesmo Gradle Wrapper.
- As regras de escalonamento continuam em um único lugar e são as mesmas da saída textual.
- A lógica de apresentação que não depende de Swing (`TimelinePresentation` e os modelos de tabela) é testada automaticamente, e o diagrama é verificado por renderização em memória no modo headless.

### Custos e riscos

- Aparência dependente do sistema operacional.
- Os componentes Swing em si (janela, botões, diálogos) não são cobertos por testes automatizados; sua verificação é manual.
- O diagrama de Gantt com largura fixa por segundo exige rolagem horizontal em simulações longas.
