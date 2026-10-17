#include "teste.h"
#include "nucleo.h"

#include <time.h>

#define QUANTUM_US 1000
#define LIMITE_S   5

static volatile int  Parar = 0;
static volatile int  UltimaThread = -1;
static volatile long Alternancias = 0;
static volatile long Voltas[MAX_THREADS];

// Laço que nunca cede o processador: só sai do processador se for preemptado
static void LacoInfinito(void *Argumento)
{
	long Eu = (long)Argumento;

	while(!Parar)
	{
		Voltas[Eu]++;
		if(UltimaThread != Eu)
		{
			UltimaThread = Eu;
			Alternancias++;
		}
	}
}

// Cálculo longo sem ceder o processador: se a preempção corromper registradores, o resultado diverge da fórmula fechada
#define TERMOS 20000000L

static volatile int SomasCorretas = 1;

static void SomaLonga(void *Argumento)
{
	long Deslocamento = (long)Argumento;
	long Soma = 0;
	double SomaReal = 0.0;

	for(long i = 1; i <= TERMOS; i++)
	{
		Soma += i + Deslocamento;
		SomaReal += 1.0;
	}

	if(Soma != TERMOS * (TERMOS + 1) / 2 + TERMOS * Deslocamento || SomaReal != (double)TERMOS)
		SomasCorretas = 0;
}

static double Segundos()
{
	struct timespec Agora;
	clock_gettime(CLOCK_MONOTONIC, &Agora);
	return Agora.tv_sec + Agora.tv_nsec / 1e9;
}

// O main também não cede o processador: só espera, contando com a preempção para dar vez às outras
static int EsperaTodas()
{
	double Inicio = Segundos();
	while(ThreadsAtivas() > 0)
	{
		if(Segundos() - Inicio > LIMITE_S) return 0;
	}
	return 1;
}

int main()
{
	IniciaNucleo();
	ConfiguraTemporizador(QUANTUM_US);

	CriaThread(LacoInfinito, (void *)1L);
	CriaThread(LacoInfinito, (void *)2L);

	double Inicio = Segundos();
	while(Alternancias < 50 && Segundos() - Inicio < LIMITE_S);

	Parar = 1;
	VERIFICA(EsperaTodas());
	VERIFICA(Alternancias >= 50);
	VERIFICA(Voltas[1] > 0 && Voltas[2] > 0);

	IniciaNucleo();
	CriaThread(SomaLonga, (void *)1L);
	CriaThread(SomaLonga, (void *)2L);
	CriaThread(SomaLonga, (void *)3L);
	VERIFICA(EsperaTodas());
	VERIFICA(SomasCorretas);

	DesligaTemporizador();

	return RESUMO_TESTES();
}
