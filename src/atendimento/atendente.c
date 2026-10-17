#include "atendente.h"
#include "comum.h"
#include "relogio.h"

#include <stdio.h>
#include <signal.h>
#include <fcntl.h>
#include <semaphore.h>
#include <sys/wait.h>

static int AnalistaDormindo = 0;
static int PidsPendentes = 0;

static sem_t *SemBlock;

int ClienteSatisfeito(CLIENTE *Cliente, long FimMs, long X)
{
	long Paciencia = (Cliente->Prioridade == PRIORIDADE_ALTA) ? X / 2 : X;

	return (FimMs - Cliente->HoraChegada) <= Paciencia;
}

// waitpid só informa uma vez cada parada; por isso o estado é guardado em AnalistaDormindo
static void AtualizaEstadoAnalista(pid_t PidAnalista)
{
	int Status;

	if(waitpid(PidAnalista, &Status, WUNTRACED | WNOHANG) > 0 && WIFSTOPPED(Status))
		AnalistaDormindo = 1;
}

static void AcordaAnalista(pid_t PidAnalista)
{
	kill(PidAnalista, SIGCONT);
	AnalistaDormindo = 0;

	// O Analista imprime até MAX_IMPRESSOS a cada despertar
	PidsPendentes -= (PidsPendentes < MAX_IMPRESSOS) ? PidsPendentes : MAX_IMPRESSOS;
}

// Acorda o Analista quando há uma leva completa para imprimir e ele já está dormindo
static void TentaAcordarAnalista(pid_t PidAnalista)
{
	AtualizaEstadoAnalista(PidAnalista);

	if(AnalistaDormindo && PidsPendentes >= MAX_IMPRESSOS)
		AcordaAnalista(PidAnalista);
}

// Acorda o Analista quantas vezes forem necessárias para imprimir todo o LNG
void EsvaziaLng(ATENDENTE *Dados)
{
	while(1)
	{
		AtualizaEstadoAnalista(Dados->PidAnalista);

		if(AnalistaDormindo && PidsPendentes == 0) break;

		if(AnalistaDormindo)
			AcordaAnalista(Dados->PidAnalista);
		else
			CedeProcessador();
	}
}

// Espera o cliente terminar. Além de saber quando o atendimento acabou, recolhe o processo
// encerrado, que de outra forma ficaria como zumbi
static void EsperaClienteTerminar(pid_t Pid)
{
	while(waitpid(Pid, NULL, WNOHANG) == 0)
		CedeProcessador();
}

static void EscreveLng(pid_t Pid)
{
	// sem_wait bloquearia o processo inteiro enquanto o Analista usa o LNG
	while(sem_trywait(SemBlock) != 0)
		CedeProcessador();

	//*** Início da região crítica: o Analista também lê e reescreve o LNG ***
	DesabilitaInterrupcoes();
	FILE *Lng = fopen(ARQUIVO_LNG, "a");
	fprintf(Lng, "%d\n", Pid);
	fclose(Lng);
	HabilitaInterrupcoes();
	//*** Fim da região crítica ***

	sem_post(SemBlock);

	PidsPendentes++;
}

static int DeveContinuar(ATENDENTE *Dados)
{
	if(Dados->Encerrar) return 0;

	if(Dados->N > 0 && Dados->Atendidos >= Dados->N) return 0;

	// A Recepção parou por erro: não chegarão mais clientes além dos que já estão na fila
	if(Dados->Recepcao->Encerrar && TamanhoFila(Dados->Fila) == 0) return 0;

	return 1;
}

void Atendente(void *Argumento)
{
	ATENDENTE *Dados = Argumento;

	DesabilitaInterrupcoes();
	SemBlock = sem_open(SEM_BLOCK, O_RDWR);
	HabilitaInterrupcoes();

	while(DeveContinuar(Dados))
	{
		CLIENTE Cliente;
		if(!RetiraFila(Dados->Fila, &Cliente))
		{
			CedeProcessador();
			continue;
		}

		kill(Cliente.Pid, SIGCONT);
		EsperaClienteTerminar(Cliente.Pid);
		long FimMs = TempoDecorridoMs();

		EscreveLng(Cliente.Pid);

		if(ClienteSatisfeito(&Cliente, FimMs, Dados->X))
			Dados->Satisfeitos++;
		Dados->Atendidos++;

		TentaAcordarAnalista(Dados->PidAnalista);
	}

	DesabilitaInterrupcoes();
	sem_close(SemBlock);
	HabilitaInterrupcoes();
}
