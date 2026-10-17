#ifndef NUCLEO_H
#define NUCLEO_H

#include <stdint.h>

#define MAX_THREADS   8
#define TAMANHO_PILHA (64*1024)

// Thread 0 é o fluxo principal (main)
#define THREAD_PRINCIPAL 0

typedef enum
{
	LIVRE,
	PRONTA,
	EXECUTANDO,
	TERMINADA

} ESTADO;

typedef struct
{
	uint64_t Rsp;
	uint8_t *Pilha;
	ESTADO   Estado;
	void   (*Entrada)(void *);
	void    *Argumento;

} THREAD;

extern THREAD Thread[MAX_THREADS];
extern volatile int ThreadAtual;

void IniciaNucleo();
int  CriaThread(void (*Entrada)(void *), void *Argumento);
void CedeProcessador();
int  ThreadsAtivas();

void ConfiguraTemporizador(int QuantumUs);
void DesligaTemporizador();
void DesabilitaInterrupcoes();
void HabilitaInterrupcoes();

// 0 = livre, 1 = ocupada
typedef volatile int TRAVA;

void EntraRegiaoCritica(TRAVA *Trava);
void SaiRegiaoCritica(TRAVA *Trava);

#endif
