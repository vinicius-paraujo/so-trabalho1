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

No Windows:

```powershell
.\gradlew.bat run
```

No Linux ou WSL:

```bash
./gradlew run
```

## Documentação

- `docs/requirements.md`: requisitos confirmados e decisões ainda necessárias.
- `docs/architecture.md`: direcionadores e regras arquiteturais.
- `docs/board.md`: estado, dependências e critérios de aceite dos cards.
- `docs/testing.md`: execução dos testes, convenções e resultados esperados.
- `docs/adrs/`: decisões relevantes do projeto.
