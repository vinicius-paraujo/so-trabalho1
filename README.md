# Trabalho 1 — Sistemas Operacionais (Yuri)

Simulação de um sistema de atendimento com threads implementadas por troca de contexto e escalonador próprios.

## Requisitos

- Linux x86-64
- `gcc` e `make`

## Compilação

Na raiz do projeto:

```bash
make
```

Gera os executáveis `atendimento`, `analista` e `cliente` na raiz do projeto.

## Execução

Execute a partir da raiz do projeto: o `atendimento` inicia `./analista` e `./cliente` pelo caminho
relativo, e os processos trocam informações por arquivos no diretório corrente.

```bash
./atendimento N X
```

- `N`: número de clientes a serem gerados (`0` = infinito).
- `X`: paciência máxima, em ms. Clientes de prioridade alta esperam até `X/2` ms; de prioridade baixa, até `X` ms.

Exemplos:

```bash
./atendimento 100 50
```

```bash
./atendimento 0 50
```

No segundo exemplo, os clientes são gerados até que a tecla `s` seja pressionada.

Durante a execução, o Analista imprime os PIDs dos clientes atendidos, em linhas `Analista: ...`
de até 10 PIDs. Ao final, o programa informa a taxa de satisfação e o tempo total de execução:

```
Taxa de satisfacao: 0.9800 (98/100)
Tempo total: 61 ms
```

`Ctrl+C` interrompe a execução e remove os processos, semáforos e arquivos criados.

## Testes

```bash
make test
```

O teste `teste_macro` contém uma falha proposital, que verifica se a macro de testes a detecta;
ela aparece na saída como `(falha esperada a seguir)`.

## Limpeza

```bash
make clean
```
