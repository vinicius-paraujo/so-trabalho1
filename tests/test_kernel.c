#include "teste.h"
#include "kernel.h"

#include <string.h>

#define PASSOS 5

static char Registro[64];
static int  Posicao = 0;

static void Escreve(void *Arg)
{
	char Letra = *(char *)Arg;

	for(int i = 0; i < PASSOS; i++)
	{
		Registro[Posicao++] = Letra;
		Yield();
	}
}

#define TROCAS 10000

static long Contador = 0;

static void Incrementa(void *Arg)
{
	(void)Arg;
	for(int i = 0; i < TROCAS; i++)
	{
		Contador++;
		Yield();
	}
}

// Variáveis locais vivem em registradores/pilha da thread: se o contexto
// não for restaurado corretamente, os valores divergem após o Yield
static int LocaisPreservados = 1;

static void PreservaLocais(void *Arg)
{
	long Base = (long)Arg;
	long a = Base + 1, b = Base + 2, c = Base + 3;
	double d = Base * 0.5;

	for(int i = 0; i < 100; i++)
	{
		Yield();
		if(a != Base + 1 || b != Base + 2 || c != Base + 3 || d != Base * 0.5)
			LocaisPreservados = 0;
	}
}

static void EsperaTodas()
{
	while(ThreadsAtivas() > 0)
		Yield();
}

int main()
{
	// Duas threads alternam a cada Yield, em Round-Robin
	InitKernel();
	char A = 'A', B = 'B';
	InitThread(Escreve, &A);
	InitThread(Escreve, &B);
	EsperaTodas();
	Registro[Posicao] = '\0';
	VERIFICA(strcmp(Registro, "ABABABABAB") == 0);

	// Muitas trocas sem corromper pilha ou contagem
	InitKernel();
	InitThread(Incrementa, NULL);
	InitThread(Incrementa, NULL);
	EsperaTodas();
	VERIFICA(Contador == 2 * TROCAS);

	// Contexto (variáveis locais, inclusive ponto flutuante) sobrevive às trocas
	InitKernel();
	InitThread(PreservaLocais, (void *)100L);
	InitThread(PreservaLocais, (void *)200L);
	InitThread(PreservaLocais, (void *)300L);
	EsperaTodas();
	VERIFICA(LocaisPreservados);

	// Tabela cheia: InitThread recusa a criação
	InitKernel();
	int Criadas = 0;
	while(InitThread(Incrementa, NULL) != -1)
		Criadas++;
	VERIFICA(Criadas == MAX_THREADS - 1);

	return RESUMO_TESTES();
}
