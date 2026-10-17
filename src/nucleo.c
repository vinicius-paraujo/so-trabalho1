#include "nucleo.h"

#include <stddef.h>
#include <signal.h>
#include <sys/time.h>

THREAD Thread[MAX_THREADS];
volatile int ThreadAtual = THREAD_PRINCIPAL;

// Pilhas reservadas estaticamente: uma por thread, alinhadas a 16 bytes como exige a ABI x86-64
static uint8_t Pilhas[MAX_THREADS][TAMANHO_PILHA] __attribute__((aligned(16)));

// Equivalentes a cli/sti: o SIGALRM faz o papel da interrupção do temporizador,
// então bloqueá-lo impede que o escalonador interrompa o trecho atual
void DesabilitaInterrupcoes()
{
	sigset_t Temporizador;
	sigemptyset(&Temporizador);
	sigaddset(&Temporizador, SIGALRM);
	sigprocmask(SIG_BLOCK, &Temporizador, NULL);
}

void HabilitaInterrupcoes()
{
	sigset_t Temporizador;
	sigemptyset(&Temporizador);
	sigaddset(&Temporizador, SIGALRM);
	sigprocmask(SIG_UNBLOCK, &Temporizador, NULL);
}

void IniciaNucleo()
{
	DesabilitaInterrupcoes();

	for(int i = 0; i < MAX_THREADS; i++)
		Thread[i].Estado = LIVRE;

	// O main já está em execução, na pilha do próprio processo; seu Rsp é salvo na primeira troca
	Thread[THREAD_PRINCIPAL].Estado = EXECUTANDO;
	ThreadAtual = THREAD_PRINCIPAL;

	HabilitaInterrupcoes();
}

// Round-Robin: recebe o rsp de quem parou e devolve o rsp da próxima thread pronta
__attribute__((used))
uint64_t Escalonador(uint64_t RspAtual)
{
	Thread[ThreadAtual].Rsp = RspAtual;

	if(Thread[ThreadAtual].Estado == EXECUTANDO)
		Thread[ThreadAtual].Estado = PRONTA;

	// Sempre termina: o main nunca fica TERMINADA, então há ao menos uma thread PRONTA
	int Proxima = ThreadAtual;
	do
	{
		Proxima = (Proxima + 1) % MAX_THREADS;
	}
	while(Thread[Proxima].Estado != PRONTA);

	ThreadAtual = Proxima;
	Thread[Proxima].Estado = EXECUTANDO;

	return Thread[Proxima].Rsp;
}

// Salva o contexto da thread atual na sua pilha, pede ao Escalonador a próxima e restaura o contexto dela.
// Deve ser chamada sempre com as interrupções desabilitadas.
// "naked": sem prólogo/epílogo do compilador, a pilha fica exatamente como o assembly a deixa.
__attribute__((naked))
static void TrocaContexto()
{
	__asm__ volatile(
		".intel_syntax noprefix\n"

		// O rip de retorno já foi empilhado pelo call
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
		"call Escalonador\n"

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

// Troca cooperativa: a própria thread cede o processador
void CedeProcessador()
{
	DesabilitaInterrupcoes();
	TrocaContexto();
	HabilitaInterrupcoes();
}

// Troca preemptiva. Ao entregar o sinal, o Linux já salvou na pilha da thread o contexto interrompido
// (como o hardware empilha rip/rflags na interrupção) e mantém o SIGALRM bloqueado durante o tratador.
// Quando esta thread for escolhida de novo, TrocaContexto retorna aqui, o tratador termina e o
// Linux restaura o contexto interrompido e a máscara de sinais anterior.
static void TratadorTemporizador(int Sinal)
{
	(void)Sinal;
	TrocaContexto();
}

void ConfiguraTemporizador(int QuantumUs)
{
	struct sigaction Acao;
	Acao.sa_handler = TratadorTemporizador;
	sigemptyset(&Acao.sa_mask);
	// Chamadas de sistema interrompidas pelo temporizador são retomadas em vez de falharem com EINTR
	Acao.sa_flags = SA_RESTART;
	sigaction(SIGALRM, &Acao, NULL);

	struct itimerval Temporizador;
	Temporizador.it_interval.tv_sec  = 0;
	Temporizador.it_interval.tv_usec = QuantumUs;
	Temporizador.it_value = Temporizador.it_interval;
	setitimer(ITIMER_REAL, &Temporizador, NULL);
}

void DesligaTemporizador()
{
	struct itimerval Desligado = { 0 };
	setitimer(ITIMER_REAL, &Desligado, NULL);
}

static void FinalizaThread()
{
	DesabilitaInterrupcoes();
	Thread[ThreadAtual].Estado = TERMINADA;

	// Uma thread TERMINADA nunca volta a ser escolhida, então esta troca não retorna
	TrocaContexto();
}

// Primeira função executada por toda thread nova
static void InicioThread()
{
	// A thread nasce dentro de uma troca de contexto, isto é, com as interrupções desabilitadas
	HabilitaInterrupcoes();

	Thread[ThreadAtual].Entrada(Thread[ThreadAtual].Argumento);

	FinalizaThread();
}

// Monta na pilha nova o mesmo quadro que TrocaContexto deixaria, para que a primeira
// "retomada" da thread comece em InicioThread
int CriaThread(void (*Entrada)(void *), void *Argumento)
{
	DesabilitaInterrupcoes();

	int Id = -1;
	for(int i = 0; i < MAX_THREADS; i++)
	{
		if(i != THREAD_PRINCIPAL && Thread[i].Estado == LIVRE)
		{
			Id = i;
			break;
		}
	}

	if(Id == -1)
	{
		HabilitaInterrupcoes();
		return -1;
	}

	uint8_t  *BasePilha = Pilhas[Id];
	uint64_t *rsp = (uint64_t *)(BasePilha + TAMANHO_PILHA);

	// Endereço de retorno fictício: InicioThread nunca retorna, mas a ABI exige que, ao entrar
	// numa função, a pilha esteja deslocada 8 bytes do alinhamento, como se um call tivesse ocorrido
	*(--rsp) = 0;
	*(--rsp) = (uint64_t)InicioThread; // rip consumido pelo ret de TrocaContexto

	*(--rsp) = 0;              // rax
	*(--rsp) = 0;              // rbx
	*(--rsp) = 0;              // rcx
	*(--rsp) = 0;              // rdx
	*(--rsp) = 0;              // rsi
	*(--rsp) = 0;              // rdi
	*(--rsp) = 0;              // rbp
	*(--rsp) = 0;              // r8
	*(--rsp) = 0;              // r9
	*(--rsp) = 0;              // r10
	*(--rsp) = 0;              // r11
	*(--rsp) = 0;              // r12
	*(--rsp) = 0;              // r13
	*(--rsp) = 0;              // r14
	*(--rsp) = 0;              // r15

	Thread[Id].Rsp       = (uint64_t)rsp;
	Thread[Id].Pilha     = BasePilha;
	Thread[Id].Entrada   = Entrada;
	Thread[Id].Argumento = Argumento;
	Thread[Id].Estado    = PRONTA;

	HabilitaInterrupcoes();

	return Id;
}

// TSL: testa e ocupa a trava numa única instrução atômica. Como a preempção só ocorre entre
// instruções, nenhuma outra thread consegue ver a trava livre entre o teste e a ocupação.
void EntraRegiaoCritica(TRAVA *Trava)
{
	// Se ocupada, quem a detém só a libera quando voltar a executar:
	// ceder o processador evita gastar o resto do quantum em espera ocupada
	while(__sync_fetch_and_or(Trava, 1))
		CedeProcessador();
}

void SaiRegiaoCritica(TRAVA *Trava)
{
	// Além de zerar a trava, impede o compilador de mover escritas da região crítica para depois dela
	__sync_lock_release(Trava);
}

int ThreadsAtivas()
{
	int Ativas = 0;
	for(int i = 0; i < MAX_THREADS; i++)
	{
		if(i != THREAD_PRINCIPAL && (Thread[i].Estado == PRONTA || Thread[i].Estado == EXECUTANDO))
			Ativas++;
	}
	return Ativas;
}
