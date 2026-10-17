#include "teste.h"
#include "fila.h"

#define QUANTUM_US 100
#define TOTAL_CLIENTES 200000

static CLIENTE NovoCliente(pid_t Pid)
{
	CLIENTE Cliente = { Pid, Pid * 10L, PRIORIDADE_BAIXA, 1 };
	return Cliente;
}

static void TestaOperacoesBasicas()
{
	FILA Fila;
	IniciaFila(&Fila);
	CLIENTE Retirado;

	VERIFICA(TamanhoFila(&Fila) == 0);
	VERIFICA(RetiraFila(&Fila, &Retirado) == 0);

	InsereFila(&Fila, NovoCliente(1));
	InsereFila(&Fila, NovoCliente(2));
	InsereFila(&Fila, NovoCliente(3));
	VERIFICA(TamanhoFila(&Fila) == 3);

	// Ordem de chegada, com todos os campos preservados
	RetiraFila(&Fila, &Retirado);
	VERIFICA(Retirado.Pid == 1 && Retirado.HoraChegada == 10);
	RetiraFila(&Fila, &Retirado);
	VERIFICA(Retirado.Pid == 2);
	RetiraFila(&Fila, &Retirado);
	VERIFICA(Retirado.Pid == 3);
	VERIFICA(TamanhoFila(&Fila) == 0);

	for(int i = 0; i < CAPACIDADE_FILA; i++)
		InsereFila(&Fila, NovoCliente(i));
	VERIFICA(InsereFila(&Fila, NovoCliente(999)) == 0);
	VERIFICA(TamanhoFila(&Fila) == CAPACIDADE_FILA);
}

// Produtor e consumidor sob preempção, como farão a Recepção e o Atendente
static FILA FilaCompartilhada;
static volatile int ForaDeOrdem = 0;
static volatile int Recebidos = 0;

static void Produtor(void *Argumento)
{
	(void)Argumento;
	for(pid_t Pid = 1; Pid <= TOTAL_CLIENTES; Pid++)
	{
		while(!InsereFila(&FilaCompartilhada, NovoCliente(Pid)))
			CedeProcessador();
	}
}

static void Consumidor(void *Argumento)
{
	(void)Argumento;
	CLIENTE Cliente;
	pid_t Esperado = 1;

	while(Esperado <= TOTAL_CLIENTES)
	{
		if(!RetiraFila(&FilaCompartilhada, &Cliente))
		{
			CedeProcessador();
			continue;
		}

		if(Cliente.Pid != Esperado || Cliente.HoraChegada != Esperado * 10L)
			ForaDeOrdem = 1;

		Esperado++;
		Recebidos++;
	}
}

static void TestaProdutorConsumidor()
{
	IniciaNucleo();
	IniciaFila(&FilaCompartilhada);
	ConfiguraTemporizador(QUANTUM_US);

	CriaThread(Produtor, NULL);
	CriaThread(Consumidor, NULL);
	while(ThreadsAtivas() > 0)
		CedeProcessador();

	DesligaTemporizador();

	// Cada cliente chega exatamente uma vez, na ordem em que foi inserido, sem campos corrompidos
	VERIFICA(Recebidos == TOTAL_CLIENTES);
	VERIFICA(!ForaDeOrdem);
	VERIFICA(TamanhoFila(&FilaCompartilhada) == 0);
}

int main()
{
	IniciaNucleo();

	TestaOperacoesBasicas();
	TestaProdutorConsumidor();

	return RESUMO_TESTES();
}
