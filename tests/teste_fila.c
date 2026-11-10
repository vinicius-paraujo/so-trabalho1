#include "teste.h"
#include "fila.h"

#define QUANTUM_US 100
#define TOTAL_CLIENTES 200000
#define X              100

static CLIENTE NovoCliente(pid_t Pid)
{
	CLIENTE Cliente = { Pid, Pid * 10L, PRIORIDADE_BAIXA, 1 };
	return Cliente;
}

static CLIENTE ClienteChegandoEm(pid_t Pid, long HoraChegada, PRIORIDADE Prioridade)
{
	CLIENTE Cliente = { Pid, HoraChegada, Prioridade, 1 };
	return Cliente;
}

static void TestaOperacoesBasicas()
{
	FILA Fila;
	IniciaFila(&Fila, X);
	CLIENTE Retirado;

	VERIFICA(TamanhoFila(&Fila) == 0);
	VERIFICA(RetiraFila(&Fila, &Retirado, 0) == 0);

	InsereFila(&Fila, NovoCliente(1));
	InsereFila(&Fila, NovoCliente(2));
	InsereFila(&Fila, NovoCliente(3));
	VERIFICA(TamanhoFila(&Fila) == 3);

	// Mesma prioridade: o prazo acompanha a chegada, e a ordem é a de chegada, com todos os campos preservados
	RetiraFila(&Fila, &Retirado, 0);
	VERIFICA(Retirado.Pid == 1 && Retirado.HoraChegada == 10);
	RetiraFila(&Fila, &Retirado, 0);
	VERIFICA(Retirado.Pid == 2);
	RetiraFila(&Fila, &Retirado, 0);
	VERIFICA(Retirado.Pid == 3);
	VERIFICA(TamanhoFila(&Fila) == 0);

	for(int i = 0; i < CAPACIDADE_FILA; i++)
		InsereFila(&Fila, NovoCliente(i));
	VERIFICA(InsereFila(&Fila, NovoCliente(999)) == 0);
	VERIFICA(TamanhoFila(&Fila) == CAPACIDADE_FILA);
}

// X = 100: alta tem prazo em chegada + 50; baixa, em chegada + 100
static void TestaOrdemDeRetirada()
{
	FILA Fila;
	IniciaFila(&Fila, X);
	CLIENTE Retirado;

	InsereFila(&Fila, ClienteChegandoEm(1, 0,  PRIORIDADE_BAIXA));   // prazo 100
	InsereFila(&Fila, ClienteChegandoEm(2, 10, PRIORIDADE_ALTA));    // prazo 60
	InsereFila(&Fila, ClienteChegandoEm(3, 20, PRIORIDADE_BAIXA));   // prazo 120

	// Todos ainda podem ser satisfeitos: sai primeiro o de prazo mais próximo, mesmo tendo chegado depois
	RetiraFila(&Fila, &Retirado, 50);
	VERIFICA(Retirado.Pid == 2);

	// O cliente 1 já não pode ser satisfeito se o atendimento terminar em 110: o 3 passa à frente
	RetiraFila(&Fila, &Retirado, 110);
	VERIFICA(Retirado.Pid == 3);

	// O prazo é inclusivo, como na avaliação de satisfação
	InsereFila(&Fila, ClienteChegandoEm(4, 30, PRIORIDADE_ALTA));    // prazo 80
	RetiraFila(&Fila, &Retirado, 80);
	VERIFICA(Retirado.Pid == 4);

	// Ninguém mais pode ser satisfeito: sai o mais antigo
	InsereFila(&Fila, ClienteChegandoEm(5, 40, PRIORIDADE_BAIXA));   // prazo 140
	RetiraFila(&Fila, &Retirado, 500);
	VERIFICA(Retirado.Pid == 1);
	RetiraFila(&Fila, &Retirado, 500);
	VERIFICA(Retirado.Pid == 5);
	VERIFICA(TamanhoFila(&Fila) == 0);
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
		if(!RetiraFila(&FilaCompartilhada, &Cliente, 0))
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
	IniciaFila(&FilaCompartilhada, X);
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
	TestaOrdemDeRetirada();
	TestaProdutorConsumidor();

	return RESUMO_TESTES();
}
