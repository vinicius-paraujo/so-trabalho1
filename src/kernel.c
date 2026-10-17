#include "kernel.h"

THREAD Thread[MAX_THREADS];
volatile int CurrentThread = MAIN_THREAD;

// Pilhas reservadas estaticamente: uma por thread, alinhadas a 16 bytes como exige a ABI x86-64
static uint8_t Pilhas[MAX_THREADS][STACK_SIZE] __attribute__((aligned(16)));

void InitKernel()
{
	for(int i = 0; i < MAX_THREADS; i++)
		Thread[i].Estado = LIVRE;

	// O main já está em execução, na pilha do próprio processo; seu Rsp é salvo no primeiro Yield
	Thread[MAIN_THREAD].Estado = EXECUTANDO;
	CurrentThread = MAIN_THREAD;
}

// Round-Robin: recebe o rsp de quem parou e devolve o rsp da próxima thread pronta
__attribute__((used))
uint64_t Scheduler(uint64_t CurrentRsp)
{
	Thread[CurrentThread].Rsp = CurrentRsp;

	if(Thread[CurrentThread].Estado == EXECUTANDO)
		Thread[CurrentThread].Estado = PRONTA;

	// Sempre termina: o main nunca fica TERMINADA, então há ao menos uma thread PRONTA
	int Proxima = CurrentThread;
	do
	{
		Proxima = (Proxima + 1) % MAX_THREADS;
	}
	while(Thread[Proxima].Estado != PRONTA);

	CurrentThread = Proxima;
	Thread[Proxima].Estado = EXECUTANDO;

	return Thread[Proxima].Rsp;
}

// Troca de contexto cooperativa: a thread cede o processador chamando Yield().
// "naked": sem prólogo/epílogo do compilador, a pilha fica exatamente como o assembly a deixa.
__attribute__((naked))
void Yield()
{
	__asm__ volatile(
		".intel_syntax noprefix\n"

		// Salva o contexto na pilha da thread atual (o rip de retorno já foi empilhado pelo call)
		"push rax\n"
		"push rbx\n"
		"push rcx\n"
		"push rdx\n"
		"push rsi\n"
		"push rdi\n"
		"push rbp\n"
		"push r8\n"
		"push r9\n"
		"push r10\n"
		"push r11\n"
		"push r12\n"
		"push r13\n"
		"push r14\n"
		"push r15\n"

		"mov rdi, rsp\n"
		"call Scheduler\n"

		// A troca de contexto: passa a usar a pilha da thread escolhida
		"mov rsp, rax\n"

		"pop r15\n"
		"pop r14\n"
		"pop r13\n"
		"pop r12\n"
		"pop r11\n"
		"pop r10\n"
		"pop r9\n"
		"pop r8\n"
		"pop rbp\n"
		"pop rdi\n"
		"pop rsi\n"
		"pop rdx\n"
		"pop rcx\n"
		"pop rbx\n"
		"pop rax\n"

		// Desempilha o rip da nova thread e continua de onde ela parou
		"ret\n"

		".att_syntax prefix\n"
	);
}

__attribute__((used))
void ThreadFinish()
{
	Thread[CurrentThread].Estado = TERMINADA;

	// Uma thread TERMINADA nunca volta a ser escolhida, então este Yield não retorna
	Yield();
}

// Destino do "return" da função de entrada de uma thread.
// Ao chegar aqui a pilha está desalinhada (nenhum call empilhou um retorno), então realinha antes de chamar.
__attribute__((naked))
static void ThreadExit()
{
	__asm__ volatile(
		".intel_syntax noprefix\n"
		"and rsp, -16\n"
		"call ThreadFinish\n"
		".att_syntax prefix\n"
	);
}

// Monta na pilha nova o mesmo quadro que Yield deixaria, para que a primeira
// "retomada" da thread comece em Entry(Arg)
int InitThread(void (*Entry)(void *), void *Arg)
{
	int Id = -1;
	for(int i = 0; i < MAX_THREADS; i++)
	{
		if(i != MAIN_THREAD && Thread[i].Estado == LIVRE)
		{
			Id = i;
			break;
		}
	}

	if(Id == -1) return -1;

	uint8_t  *StackBase = Pilhas[Id];
	uint64_t *rsp = (uint64_t *)(StackBase + STACK_SIZE);

	*(--rsp) = (uint64_t)ThreadExit; // para onde Entry "retorna" ao terminar
	*(--rsp) = (uint64_t)Entry;      // rip consumido pelo ret de Yield

	*(--rsp) = 0;              // rax
	*(--rsp) = 0;              // rbx
	*(--rsp) = 0;              // rcx
	*(--rsp) = 0;              // rdx
	*(--rsp) = 0;              // rsi
	*(--rsp) = (uint64_t)Arg;  // rdi (1º argumento de Entry)
	*(--rsp) = 0;              // rbp
	*(--rsp) = 0;              // r8
	*(--rsp) = 0;              // r9
	*(--rsp) = 0;              // r10
	*(--rsp) = 0;              // r11
	*(--rsp) = 0;              // r12
	*(--rsp) = 0;              // r13
	*(--rsp) = 0;              // r14
	*(--rsp) = 0;              // r15

	Thread[Id].Rsp    = (uint64_t)rsp;
	Thread[Id].Stack  = StackBase;
	Thread[Id].Entry  = Entry;
	Thread[Id].Arg    = Arg;
	Thread[Id].Estado = PRONTA;

	return Id;
}

int ThreadsAtivas()
{
	int Ativas = 0;
	for(int i = 0; i < MAX_THREADS; i++)
	{
		if(i != MAIN_THREAD && (Thread[i].Estado == PRONTA || Thread[i].Estado == EXECUTANDO))
			Ativas++;
	}
	return Ativas;
}
