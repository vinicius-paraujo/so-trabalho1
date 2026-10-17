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
#include "teclado.h"
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

// Clientes que ficaram na fila ao pressionar "s" nunca serão atendidos: são encerrados e recolhidos
static void EncerraClientesNaFila()
{
	CLIENTE Cliente;

	while(RetiraFila(&Fila, &Cliente))
	{
		kill(Cliente.Pid, SIGKILL);
		waitpid(Cliente.Pid, NULL, 0);
	}
}

static void LiberaRecursos()
{
	// O Analista está parado, fora da região crítica do LNG: pode ser encerrado sem prender /sem_block.
	// Se ele já terminou, o PID já foi recolhido e poderia pertencer a outro processo: não se envia sinal.
	if(AnalistaAtivo())
	{
		kill(DadosAtendente.PidAnalista, SIGKILL);
		waitpid(DadosAtendente.PidAnalista, NULL, 0);
	}

	sem_unlink(SEM_ATEND);
	sem_unlink(SEM_BLOCK);

	remove(ARQUIVO_LNG);
	remove(ARQUIVO_PID_ANALISTA);
}

// Ctrl+C interrompe o programa em qualquer ponto. Sem este tratador, clientes parados e o Analista
// continuariam existindo, e os semáforos e arquivos ficariam no sistema.
static void TratadorInterrupcao(int Sinal)
{
	(void)Sinal;

	for(int i = 0; i < Fila.Tamanho; i++)
		kill(Fila.Clientes[i].Pid, SIGKILL);

	if(DadosAtendente.PidEmAtendimento > 0) kill(DadosAtendente.PidEmAtendimento, SIGKILL);

	// O SIGKILL não é instantâneo: espera o cliente em criação e o Analista morrerem,
	// senão eles poderiam recriar o arquivo de demanda ou o LNG depois da remoção
	if(DadosRecepcao.PidEmCriacao > 0)
	{
		kill(DadosRecepcao.PidEmCriacao, SIGKILL);
		waitpid(DadosRecepcao.PidEmCriacao, NULL, 0);
	}
	if(DadosAtendente.PidAnalista > 0)
	{
		kill(DadosAtendente.PidAnalista, SIGKILL);
		waitpid(DadosAtendente.PidAnalista, NULL, 0);
	}

	sem_unlink(SEM_ATEND);
	sem_unlink(SEM_BLOCK);
	unlink(ARQUIVO_LNG);
	unlink(ARQUIVO_LNG_TEMP);
	unlink(ARQUIVO_PID_ANALISTA);
	unlink(ARQUIVO_DEMANDA);

	RestauraTeclado();

	// Os filhos encerrados são recolhidos pelo init quando este processo termina
	_exit(130);
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

	if(N == 0)
		ConfiguraTeclado();

	signal(SIGINT, TratadorInterrupcao);

	ConfiguraTemporizador(QUANTUM_US);

	CriaThread(Recepcao, &DadosRecepcao);

	// O Analista abre /sem_block pelo nome: precisa nascer depois que a Recepção o recriou
	while(!DadosRecepcao.SemaforosCriados)
		CedeProcessador();
	DadosAtendente.PidAnalista = CriaAnalista();

	CriaThread(Atendente, &DadosAtendente);

	// O main faz o papel da thread de controle: no modo infinito, observa a tecla de parada
	while(ThreadsAtivas() > 0)
	{
		if(N == 0)
		{
			char Tecla = TeclaPressionada();
			if(Tecla == 's' || Tecla == 'S')
			{
				DadosRecepcao.Encerrar = 1;
				DadosAtendente.Encerrar = 1;
			}
		}

		CedeProcessador();
	}

	EncerraClientesNaFila();

	// O enunciado exige que todo PID já atendido seja impresso antes da saída
	EsvaziaLng(&DadosAtendente);

	if(!AnalistaAtivo())
		fprintf(stderr, "Atendimento: o Analista terminou inesperadamente; PIDs podem nao ter sido impressos\n");

	long TempoTotal = TempoDecorridoMs();
	DesligaTemporizador();

	LiberaRecursos();

	if(N == 0)
		RestauraTeclado();

	long Atendidos = DadosAtendente.Atendidos;
	long Satisfeitos = DadosAtendente.Satisfeitos;
	double Taxa = (Atendidos > 0) ? (double)Satisfeitos / Atendidos : 0.0;

	printf("Taxa de satisfacao: %.4f (%ld/%ld)\n", Taxa, Satisfeitos, Atendidos);
	printf("Tempo total: %ld ms\n", TempoTotal);

	return 0;
}
