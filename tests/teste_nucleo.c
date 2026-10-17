#include "teste.h"
#include "nucleo.h"

#include <string.h>

#define PASSOS 5

static char Registro[64];
static int  Posicao = 0;

static void Escreve(void *Argumento)
{
	char Letra = *(char *)Argumento;

	for(int i = 0; i < PASSOS; i++)
	{
		Registro[Posicao++] = Letra;
		CedeProcessador();
	}
}

#define TROCAS 10000

static long Contador = 0;

static void Incrementa(void *Argumento)
{
	(void)Argumento;
	for(int i = 0; i < TROCAS; i++)
	{
		Contador++;
		CedeProcessador();
	}
}

// Variáveis locais vivem em registradores/pilha da thread: se o contexto
// não for restaurado corretamente, os valores divergem após ceder o processador
static int LocaisPreservados = 1;

static void PreservaLocais(void *Argumento)
{
	long Base = (long)Argumento;
	long a = Base + 1, b = Base + 2, c = Base + 3;
	double d = Base * 0.5;

	for(int i = 0; i < 100; i++)
	{
		CedeProcessador();
		if(a != Base + 1 || b != Base + 2 || c != Base + 3 || d != Base * 0.5)
			LocaisPreservados = 0;
	}
}

static void EsperaTodas()
{
	while(ThreadsAtivas() > 0)
		CedeProcessador();
}

int main()
{
	// Duas threads alternam a cada cessão do processador, em Round-Robin
	IniciaNucleo();
	char A = 'A', B = 'B';
	CriaThread(Escreve, &A);
	CriaThread(Escreve, &B);
	EsperaTodas();
	Registro[Posicao] = '\0';
	VERIFICA(strcmp(Registro, "ABABABABAB") == 0);

	// Muitas trocas sem corromper pilha ou contagem
	IniciaNucleo();
	CriaThread(Incrementa, NULL);
	CriaThread(Incrementa, NULL);
	EsperaTodas();
	VERIFICA(Contador == 2 * TROCAS);

	// Contexto (variáveis locais, inclusive ponto flutuante) sobrevive às trocas
	IniciaNucleo();
	CriaThread(PreservaLocais, (void *)100L);
	CriaThread(PreservaLocais, (void *)200L);
	CriaThread(PreservaLocais, (void *)300L);
	EsperaTodas();
	VERIFICA(LocaisPreservados);

	// Tabela cheia: CriaThread recusa a criação
	IniciaNucleo();
	int Criadas = 0;
	while(CriaThread(Incrementa, NULL) != -1)
		Criadas++;
	VERIFICA(Criadas == MAX_THREADS - 1);

	return RESUMO_TESTES();
}
