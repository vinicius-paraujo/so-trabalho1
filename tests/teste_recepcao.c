#include "teste.h"
#include "recepcao.h"
#include "relogio.h"
#include "comum.h"

#include <fcntl.h>
#include <semaphore.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>

#define QUANTUM_US 1000
#define TOTAL      150

static FILA     Fila;
static RECEPCAO Dados;
static CLIENTE  Recebidos[TOTAL];

// Lê o estado do processo em /proc: 'T' indica parado por sinal
static char EstadoProcesso(pid_t Pid)
{
	char Caminho[64];
	snprintf(Caminho, sizeof(Caminho), "/proc/%d/stat", Pid);

	FILE *Arquivo = fopen(Caminho, "r");
	if(Arquivo == NULL) return '?';

	char Estado = '?';
	fscanf(Arquivo, "%*d %*s %c", &Estado);
	fclose(Arquivo);

	return Estado;
}

static void EsperaMs(long Ms)
{
	long Fim = TempoDecorridoMs() + Ms;
	while(TempoDecorridoMs() < Fim)
		CedeProcessador();
}

int main()
{
	srand(time(NULL));
	IniciaRelogio();
	IniciaNucleo();
	IniciaFila(&Fila);
	ConfiguraTemporizador(QUANTUM_US);

	Dados.N = TOTAL;
	Dados.Fila = &Fila;
	CriaThread(Recepcao, &Dados);

	// Sem ninguém retirando, a Recepção deve pausar com a fila cheia, em vez de criar mais clientes
	EsperaMs(3000);
	VERIFICA(TamanhoFila(&Fila) == CAPACIDADE_FILA);
	VERIFICA(Dados.Criados == CAPACIDADE_FILA);

	// O main faz o papel de um Atendente que apenas retira, liberando espaço
	int Total = 0;
	while(Total < TOTAL)
	{
		if(RetiraFila(&Fila, &Recebidos[Total]))
			Total++;
		else
			CedeProcessador();
	}

	while(ThreadsAtivas() > 0)
		CedeProcessador();
	DesligaTemporizador();

	VERIFICA(Dados.Criados == TOTAL);
	VERIFICA(Dados.SemaforosCriados);

	int Parados = 0, DemandasValidas = 0, OrdemChegada = 1, Altas = 0;
	for(int i = 0; i < TOTAL; i++)
	{
		if(EstadoProcesso(Recebidos[i].Pid) == 'T') Parados++;

		int Demanda = Recebidos[i].Demanda;
		if(Demanda == 1 || Demanda == 5 || Demanda == 15) DemandasValidas++;

		if(i > 0 && Recebidos[i].HoraChegada < Recebidos[i - 1].HoraChegada) OrdemChegada = 0;

		if(Recebidos[i].Prioridade == PRIORIDADE_ALTA) Altas++;
	}

	// Todo cliente enfileirado é um processo real, parado à espera do atendimento
	VERIFICA(Parados == TOTAL);
	VERIFICA(DemandasValidas == TOTAL);
	VERIFICA(OrdemChegada);

	// 50% de chance para cada prioridade: com 150 clientes, sair desta faixa é praticamente impossível
	VERIFICA(Altas > 45 && Altas < 105);

	// Os semáforos existem e estão livres
	sem_t *SemAtend = sem_open(SEM_ATEND, O_RDWR);
	int Valor = -1;
	if(SemAtend != SEM_FAILED) sem_getvalue(SemAtend, &Valor);
	VERIFICA(Valor == 1);

	VERIFICA(access(ARQUIVO_DEMANDA, F_OK) != 0);

	for(int i = 0; i < TOTAL; i++)
	{
		kill(Recebidos[i].Pid, SIGKILL);
		waitpid(Recebidos[i].Pid, NULL, 0);
	}
	sem_unlink(SEM_ATEND);
	sem_unlink(SEM_BLOCK);

	return RESUMO_TESTES();
}
