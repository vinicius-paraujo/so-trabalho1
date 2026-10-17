# Trabalho 1 — Sistemas Operacionais (Yuri)

Simulação de um sistema de atendimento com threads implementadas por troca de contexto e escalonador próprios.

## Requisitos

- Linux x86-64
- `gcc` e `make`

## Compilação

```bash
make
```

Gera os executáveis `atendimento`, `analista` e `cliente` na raiz do projeto.

## Execução

```bash
./atendimento N X
```

- `N`: número de clientes a serem gerados (`0` = infinito; encerra com a tecla `s`).
- `X`: paciência máxima, em ms. Clientes de prioridade alta esperam até `X/2` ms; de prioridade baixa, até `X` ms.

Ao final, o programa informa a taxa de satisfação e o tempo total de execução.

## Testes

```bash
make test
```

## Limpeza

```bash
make clean
```
