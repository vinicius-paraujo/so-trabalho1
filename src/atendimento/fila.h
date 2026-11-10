#ifndef FILA_H
#define FILA_H

#include <sys/types.h>

#include "nucleo.h"

// Com N = 0, a Recepção pausa a criação de clientes quando a fila atinge este tamanho
#define CAPACIDADE_FILA 100

typedef enum
{
	PRIORIDADE_ALTA,   // X/2 ms
	PRIORIDADE_BAIXA 

} PRIORIDADE;

typedef struct
{
	pid_t      Pid;
	long       HoraChegada;
	PRIORIDADE Prioridade;
	int        Demanda;      // tempo para atendimento, lido do arquivo de demanda

} CLIENTE;

typedef struct
{
	CLIENTE Clientes[CAPACIDADE_FILA];
	int     Tamanho;
	long    X;           // paciência dos clientes de prioridade baixa, em ms
	TRAVA   Trava;

} FILA;

void IniciaFila(FILA *Fila, long X);
long PacienciaCliente(CLIENTE *Cliente, long X);
int  InsereFila(FILA *Fila, CLIENTE Cliente);
int  RetiraFila(FILA *Fila, CLIENTE *Cliente, long PrevisaoTerminoMs);
int  TamanhoFila(FILA *Fila);

#endif
