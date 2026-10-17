#include "teste.h"
#include "nucleo.h"

// Quantum curto e muitas iterações garantem centenas de preempções durante o teste
#define QUANTUM_US 100
#define ITERACOES  5000000L

static volatile long Contador = 0;
static TRAVA Trava = 0;

static volatile int Dentro = 0;
static volatile int ExclusaoViolada = 0;

// Ler, somar e escrever em passos separados, com um atraso entre a leitura e a escrita:
// quase toda preempção cai nessa janela, tornando a perda de atualizações praticamente certa
static void IncrementaSemTrava(void *Argumento)
{
	(void)Argumento;
	for(long i = 0; i < ITERACOES / 10; i++)
	{
		long Valor = Contador;
		for(volatile int Atraso = 0; Atraso < 50; Atraso++);
		Contador = Valor + 1;
	}
}

static void IncrementaComTrava(void *Argumento)
{
	(void)Argumento;
	for(long i = 0; i < ITERACOES; i++)
	{
		EntraRegiaoCritica(&Trava);
		Contador++;
		SaiRegiaoCritica(&Trava);
	}
}

// Se duas threads estiverem na região crítica ao mesmo tempo, uma delas encontra Dentro != 0
static void VerificaExclusao(void *Argumento)
{
	(void)Argumento;
	for(long i = 0; i < ITERACOES / 10; i++)
	{
		EntraRegiaoCritica(&Trava);

		if(Dentro != 0) ExclusaoViolada = 1;
		Dentro = 1;
		for(volatile int Atraso = 0; Atraso < 20; Atraso++);
		Dentro = 0;

		SaiRegiaoCritica(&Trava);
	}
}

static void EsperaTodas()
{
	while(ThreadsAtivas() > 0)
		CedeProcessador();
}

int main()
{
	IniciaNucleo();
	ConfiguraTemporizador(QUANTUM_US);

	// Sem trava, o incremento é interrompido no meio e atualizações se perdem.
	// Esta verificação mostra que o teste é capaz de detectar a condição de disputa.
	CriaThread(IncrementaSemTrava, NULL);
	CriaThread(IncrementaSemTrava, NULL);
	EsperaTodas();
	VERIFICA(Contador < 2 * (ITERACOES / 10));

	IniciaNucleo();
	Contador = 0;
	CriaThread(IncrementaComTrava, NULL);
	CriaThread(IncrementaComTrava, NULL);
	EsperaTodas();
	VERIFICA(Contador == 2 * ITERACOES);

	IniciaNucleo();
	CriaThread(VerificaExclusao, NULL);
	CriaThread(VerificaExclusao, NULL);
	CriaThread(VerificaExclusao, NULL);
	EsperaTodas();
	VERIFICA(!ExclusaoViolada);
	VERIFICA(Trava == 0);

	DesligaTemporizador();

	return RESUMO_TESTES();
}
