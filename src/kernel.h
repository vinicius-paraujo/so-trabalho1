#ifndef KERNEL_H
#define KERNEL_H

#include <stdint.h>

#define MAX_THREADS 8
#define STACK_SIZE  (64*1024)

// Thread 0 é sempre o fluxo principal (main), registrado por InitKernel
#define MAIN_THREAD 0

typedef enum
{
	LIVRE,       // posição da tabela sem thread
	PRONTA,      // aguardando o processador
	EXECUTANDO,
	TERMINADA

} ESTADO;

// TCB: o contexto (registradores) fica salvo na própria pilha da thread; aqui guarda-se só onde
typedef struct
{
	uint64_t Rsp;
	uint8_t *Stack;
	ESTADO   Estado;
	void   (*Entry)(void *);
	void    *Arg;

} THREAD;

extern THREAD Thread[MAX_THREADS];
extern volatile int CurrentThread;

void InitKernel();
int  InitThread(void (*Entry)(void *), void *Arg);
void Yield();
int  ThreadsAtivas();

#endif
