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

## Documentação

- `docs/requirements.md`: requisitos confirmados e decisões ainda necessárias.
- `docs/architecture.md`: direcionadores e regras arquiteturais.
- `docs/board.md`: estado, dependências e critérios de aceite dos cards.
- `docs/testing.md`: execução dos testes, convenções e resultados esperados.
- `docs/adrs/`: decisões relevantes do projeto.
