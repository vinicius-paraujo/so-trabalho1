#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>
#include <signal.h>
#include <semaphore.h>
#include <sys/wait.h>

#include "nucleo.h"
#include "fila.h"
#include "relogio.h"
#include "recepcao.h"
#include "atendente.h"
#include "comum.h"

#define QUANTUM_US 1000

static FILA      Fila;
static RECEPCAO  DadosRecepcao;
static ATENDENTE DadosAtendente;

static pid_t CriaAnalista()
{
	pid_t Pid = fork();

	if(Pid == 0)
	{
		execl("./analista", "analista", NULL);
		_exit(1);
	}

	return Pid;
}

static void LiberaRecursos()
{
	// O Analista está parado, fora da região crítica do LNG: pode ser encerrado sem prender /sem_block
	kill(DadosAtendente.PidAnalista, SIGKILL);
	waitpid(DadosAtendente.PidAnalista, NULL, 0);

	sem_unlink(SEM_ATEND);
	sem_unlink(SEM_BLOCK);

	remove(ARQUIVO_LNG);
	remove(ARQUIVO_PID_ANALISTA);
}

int main(int argc, char *argv[])
{
	if(argc != 3)
	{
		fprintf(stderr, "Uso: %s N X\n", argv[0]);
		fprintf(stderr, "  N: numero de clientes (0 = infinito)\n");
		fprintf(stderr, "  X: paciencia maxima, em ms\n");
		return 1;
	}

	// N: quantidade de clientes a gerar; X: paciência dos clientes de prioridade baixa (alta usa X/2)
	long N = strtol(argv[1], NULL, 10);
	long X = strtol(argv[2], NULL, 10);

	// N = 0 é válido (clientes infinitos); paciência nula ou negativa não faz sentido
	if(N < 0 || X <= 0)
	{
		fprintf(stderr, "Entradas invalidas: N >= 0 e X > 0.\n");
		return 1;
	}

	srand(time(NULL));
	IniciaRelogio();
	IniciaNucleo();
	IniciaFila(&Fila);
	remove(ARQUIVO_LNG);

	DadosRecepcao.N = N;
	DadosRecepcao.Fila = &Fila;

	DadosAtendente.N = N;
	DadosAtendente.X = X;
	DadosAtendente.Fila = &Fila;
	DadosAtendente.Recepcao = &DadosRecepcao;

	ConfiguraTemporizador(QUANTUM_US);

	CriaThread(Recepcao, &DadosRecepcao);

	// O Analista abre /sem_block pelo nome: precisa nascer depois que a Recepção o recriou
	while(!DadosRecepcao.SemaforosCriados)
		CedeProcessador();
	DadosAtendente.PidAnalista = CriaAnalista();

	CriaThread(Atendente, &DadosAtendente);

	while(ThreadsAtivas() > 0)
		CedeProcessador();

	EsvaziaLng(&DadosAtendente);

	long TempoTotal = TempoDecorridoMs();
	DesligaTemporizador();

	LiberaRecursos();

	long Atendidos = DadosAtendente.Atendidos;
	long Satisfeitos = DadosAtendente.Satisfeitos;
	double Taxa = (Atendidos > 0) ? (double)Satisfeitos / Atendidos : 0.0;

	printf("Taxa de satisfacao: %.4f (%ld/%ld)\n", Taxa, Satisfeitos, Atendidos);
	printf("Tempo total: %ld ms\n", TempoTotal);

	return 0;
}
