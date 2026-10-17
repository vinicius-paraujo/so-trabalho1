#ifndef RECEPCAO_H
#define RECEPCAO_H

#include "fila.h"

typedef struct
{
	long  N;                          // clientes a criar; 0 = infinitos
	FILA *Fila;

	volatile int  SemaforosCriados;   // o Analista só pode ser criado depois disso
	volatile int  Encerrar;           // pedido externo de parada (modo N = 0)
	volatile long Criados;

} RECEPCAO;

void Recepcao(void *Argumento);

#endif
