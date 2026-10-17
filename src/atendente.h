#ifndef ATENDENTE_H
#define ATENDENTE_H

#include "fila.h"
#include "recepcao.h"

typedef struct
{
	long      N;                 // clientes a atender; 0 = infinitos
	long      X;                 // paciência, em ms, dos clientes de prioridade baixa
	FILA     *Fila;
	RECEPCAO *Recepcao;
	pid_t     PidAnalista;

	volatile int  Encerrar;      // pedido externo de parada (modo N = 0)
	volatile long Atendidos;
	volatile long Satisfeitos;

} ATENDENTE;

void Atendente(void *Argumento);
int  ClienteSatisfeito(CLIENTE *Cliente, long FimMs, long X);
void EsvaziaLng(ATENDENTE *Dados);

#endif
