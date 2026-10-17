#include "teste.h"
#include "comum.h"

#include <fcntl.h>
#include <semaphore.h>
#include <signal.h>
#include <stdlib.h>
#include <sys/wait.h>
#include <unistd.h>

#define SAIDA_ANALISTA "saida_analista.txt"

static void EscreveLng(int Primeiro, int Quantidade)
{
	FILE *Lng = fopen(ARQUIVO_LNG, "w");
	for(int i = 0; i < Quantidade; i++)
		fprintf(Lng, "%d\n", Primeiro + i);
	fclose(Lng);
}

// Lê todos os números de um arquivo, ignorando o texto entre eles
static int LeNumeros(const char *Arquivo, int *Numeros, int Maximo)
{
	FILE *Entrada = fopen(Arquivo, "r");
	if(Entrada == NULL) return 0;

	int Lidos = 0;
	while(Lidos < Maximo)
	{
		if(fscanf(Entrada, "%d", &Numeros[Lidos]) == 1)
			Lidos++;
		else if(fgetc(Entrada) == EOF)
			break;
	}

	fclose(Entrada);
	return Lidos;
}

// Pai pode saber se o filho parou: waitpid com WUNTRACED retorna quando ele recebe SIGSTOP
static int EsperaAdormecer(pid_t Pid)
{
	int Status;
	waitpid(Pid, &Status, WUNTRACED);
	return WIFSTOPPED(Status);
}

static int Acorda(pid_t Pid)
{
	kill(Pid, SIGCONT);
	return EsperaAdormecer(Pid);
}

static int SequenciaCorreta(int *Numeros, int Inicio, int Fim, int PrimeiroValor)
{
	for(int i = Inicio; i < Fim; i++)
		if(Numeros[i] != PrimeiroValor + (i - Inicio))
			return 0;
	return 1;
}

int main()
{
	int Numeros[64];

	sem_unlink(SEM_BLOCK);
	sem_t *SemBlock = sem_open(SEM_BLOCK, O_CREAT, 0644, 1);
	remove(ARQUIVO_LNG);

	pid_t Analista = fork();
	if(Analista == 0)
	{
		int Saida = open(SAIDA_ANALISTA, O_WRONLY | O_CREAT | O_TRUNC, 0644);
		dup2(Saida, STDOUT_FILENO);
		execl("./analista", "analista", NULL);
		_exit(1);
	}

	// Nasce dormindo e publica o próprio PID
	VERIFICA(EsperaAdormecer(Analista));
	VERIFICA(LeNumeros(ARQUIVO_PID_ANALISTA, Numeros, 1) == 1 && Numeros[0] == Analista);

	// 25 PIDs: três despertares imprimem 10, 10 e 5, sempre do início do arquivo
	EscreveLng(1001, 25);

	VERIFICA(Acorda(Analista));
	VERIFICA(LeNumeros(SAIDA_ANALISTA, Numeros, 64) == 10 && SequenciaCorreta(Numeros, 0, 10, 1001));
	VERIFICA(LeNumeros(ARQUIVO_LNG, Numeros, 64) == 15 && SequenciaCorreta(Numeros, 0, 15, 1011));

	VERIFICA(Acorda(Analista));
	VERIFICA(LeNumeros(ARQUIVO_LNG, Numeros, 64) == 5);

	VERIFICA(Acorda(Analista));
	VERIFICA(LeNumeros(ARQUIVO_LNG, Numeros, 64) == 0);
	VERIFICA(LeNumeros(SAIDA_ANALISTA, Numeros, 64) == 25 && SequenciaCorreta(Numeros, 0, 25, 1001));

	// Acordado com o LNG vazio, não imprime nada e volta a dormir
	VERIFICA(Acorda(Analista));
	VERIFICA(LeNumeros(SAIDA_ANALISTA, Numeros, 64) == 25);

	// Com /sem_block ocupado, o Analista acorda mas espera; o LNG só muda depois da liberação
	EscreveLng(2001, 3);
	sem_wait(SemBlock);
	kill(Analista, SIGCONT);
	usleep(100000);
	VERIFICA(LeNumeros(ARQUIVO_LNG, Numeros, 64) == 3);
	sem_post(SemBlock);
	VERIFICA(EsperaAdormecer(Analista));
	VERIFICA(LeNumeros(ARQUIVO_LNG, Numeros, 64) == 0);

	kill(Analista, SIGKILL);
	waitpid(Analista, NULL, 0);
	sem_close(SemBlock);
	sem_unlink(SEM_BLOCK);
	remove(ARQUIVO_LNG);
	remove(ARQUIVO_PID_ANALISTA);
	remove(SAIDA_ANALISTA);

	return RESUMO_TESTES();
}
