# Simulador de Escalonamento de Processos

Trabalho 01 da disciplina CK0234 — Sistemas Operacionais.

## Requisitos

- Java Development Kit 21.
- Acesso à internet na primeira execução, caso a distribuição do Gradle e as dependências ainda não estejam em cache.

Não é necessária uma instalação global do Gradle. O projeto utiliza o Gradle Wrapper versionado.

## Compilação e testes

No Windows:

```powershell
.\gradlew.bat clean test
```

No Linux ou WSL:

```bash
./gradlew clean test
```

## Execução

O único argumento é o caminho do arquivo de configuração (`quantum` e `aging`). Os processos são lidos da entrada padrão, um por linha: `criacao duracao prioridade`.

No Windows (PowerShell):

```powershell
Get-Content processos.txt | .\gradlew.bat -q run --args="config.txt"
```

No Linux ou WSL:

```bash
./gradlew -q run --args="config.txt" < processos.txt
```

Também é possível gerar um executável independente do Gradle:

```bash
./gradlew installDist
build/install/simulador-escalonamento/bin/simulador-escalonamento config.txt < processos.txt
```

Exemplos de entrada estão em `src/test/resources/cases/`. A saída esperada completa do caso do enunciado está em `src/test/resources/cases/reference/expected-output.txt`.

Códigos de saída: `0` para sucesso, `1` para configuração ou entrada inválida e `2` para uso incorreto. Os erros são escritos em `stderr`, sem resultado parcial em `stdout`.

## Interface gráfica

No Windows:

```powershell
.\gradlew.bat runGui
```

No Linux ou WSL:

```bash
./gradlew runGui
```

Após `installDist`, a interface também pode ser aberta pelo script `build/install/simulador-escalonamento/bin/simulador-escalonamento-gui`.

A janela abre com o exemplo do enunciado já simulado. Os processos podem ser editados na tabela ou importados pelo menu **Arquivo**, nos mesmos formatos aceitos pelo terminal. A aba **Comparação** mostra as métricas dos sete algoritmos. Cada aba de algoritmo tem o diagrama de Gantt animado (Reproduzir, Passo, Reiniciar, Mostrar tudo) e o resultado por processo. A opção **Semente fixa** torna reproduzíveis os desempates aleatórios.

## Pacote de entrega

```bash
./gradlew clean test distZip
```

Gera `build/distributions/simulador-escalonamento-1.0.0-SNAPSHOT.zip`, com os scripts `bin/simulador-escalonamento` (terminal) e `bin/simulador-escalonamento-gui` (interface) e as bibliotecas em `lib/`. O pacote exige somente um JDK ou JRE 21 instalado.

## Documentação

- `docs/documento-tecnico.md`: **documento de entrega**, com classes, estruturas de dados, PCB, algoritmos, padrões e decisões.
- `docs/requirements.md`: requisitos do enunciado e decisões da equipe para os pontos não definidos.
- `docs/architecture.md`: direcionadores, regras arquiteturais e estrutura implementada.
- `docs/testing.md`: estratégia, casos de teste, oráculos e relatório de validação.
- `docs/board.md`: estado, dependências e critérios de aceite dos cards.
- `docs/adrs/`: decisões relevantes do projeto (0001 a 0008).

## Estrutura do repositório

| Caminho | Conteúdo |
| --- | --- |
| `src/main/java` | Código do simulador (terminal e interface gráfica). |
| `src/test/java`, `src/test/resources/cases` | Testes automatizados e casos de entrada versionados. |
| `docs/` (exceto os itens abaixo) | Documentação do simulador. |
| `docs/Notas de aula/` | Material de apoio: enunciado (`Tarefa 01 - Escalonamento de Processos-1.pdf`), notas de aula e exemplos da disciplina. |
| `docs/ambiente.md` | Material de apoio: ambiente EDK II/UEFI do protokernel. **Não é necessário para o simulador.** |

O material de apoio foi mantido por decisão da equipe (ADR 0008) e não participa da construção.
