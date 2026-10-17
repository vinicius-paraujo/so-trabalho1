#include "recepcao.h"
#include "comum.h"
#include "relogio.h"

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <semaphore.h>
#include <sys/wait.h>

// Remove semáforos que tenham sobrado de uma execução anterior interrompida, que poderiam
// estar com valor 0, e os cria de novo livres (valor 1)
static void CriaSemaforos()
{
	sem_unlink(SEM_ATEND);
	sem_unlink(SEM_BLOCK);

	sem_t *SemAtend = sem_open(SEM_ATEND, O_CREAT | O_EXCL, 0644, 1);
	sem_t *SemBlock = sem_open(SEM_BLOCK, O_CREAT | O_EXCL, 0644, 1);

	sem_close(SemAtend);
	sem_close(SemBlock);
}

static pid_t CriaCliente()
{
	DesabilitaInterrupcoes();
	pid_t Pid = fork();
	HabilitaInterrupcoes();

	if(Pid == 0)
	{
		execl("./cliente", "cliente", NULL);
		_exit(1);
	}

	return Pid;
}

// Espera o cliente parar (SIGSTOP), o que garante que ele já escreveu sua demanda.
// waitpid bloqueante pararia todas as threads do processo, por isso a consulta usa WNOHANG.
static int EsperaClienteDormir(pid_t Pid)
{
	int Status = 0;
	pid_t Resultado;

	while((Resultado = waitpid(Pid, &Status, WUNTRACED | WNOHANG)) == 0)
		CedeProcessador();

	return Resultado == Pid && WIFSTOPPED(Status);
}

static int LeDemanda()
{
	int Demanda = 0;

	DesabilitaInterrupcoes();

	FILE *Arquivo = fopen(ARQUIVO_DEMANDA, "r");
	if(Arquivo != NULL)
	{
		fscanf(Arquivo, "%d", &Demanda);
		fclose(Arquivo);
	}

	// Apagado após a leitura: um arquivo esquecido nunca é confundido com a demanda do próximo cliente
	remove(ARQUIVO_DEMANDA);

	HabilitaInterrupcoes();

	return Demanda;
}

void Recepcao(void *Argumento)
{
	RECEPCAO *Dados = Argumento;

	DesabilitaInterrupcoes();
	CriaSemaforos();
	HabilitaInterrupcoes();
	Dados->SemaforosCriados = 1;

	while(!Dados->Encerrar && (Dados->N == 0 || Dados->Criados < Dados->N))
	{
		// Fila cheia: pausa a criação até o Atendente liberar espaço.
		// Esperar antes de criar evita contar como espera do cliente o tempo em que a fila estava cheia.
		while(TamanhoFila(Dados->Fila) >= CAPACIDADE_FILA && !Dados->Encerrar)
			CedeProcessador();

		if(Dados->Encerrar) break;

		CLIENTE Cliente;
		Cliente.HoraChegada = TempoDecorridoMs();
		Cliente.Pid = CriaCliente();
		Dados->PidEmCriacao = Cliente.Pid;

		// Os clientes são criados um de cada vez porque todos escrevem no mesmo arquivo de demanda
		// Se o cliente terminou sem parar, o execl falhou: insistir só criaria processos inúteis
		if(!EsperaClienteDormir(Cliente.Pid))
		{
			DesabilitaInterrupcoes();
			fprintf(stderr, "Recepcao: nao foi possivel executar ./cliente\n");
			HabilitaInterrupcoes();

			Dados->Encerrar = 1;
			break;
		}

		Cliente.Demanda = LeDemanda();
		DesabilitaInterrupcoes();
		Cliente.Prioridade = (rand() % 2 == 0) ? PRIORIDADE_ALTA : PRIORIDADE_BAIXA;
		HabilitaInterrupcoes();

		InsereFila(Dados->Fila, Cliente);
		Dados->PidEmCriacao = 0;
		Dados->Criados++;
	}
}
