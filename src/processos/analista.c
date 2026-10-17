#include <stdio.h>
#include <unistd.h>
#include <signal.h>
#include <semaphore.h>
#include <fcntl.h>

#include "comum.h"

// Imprime até MAX_IMPRESSOS PIDs do início do LNG e os remove do arquivo
static void ImprimePrimeiros()
{
	FILE *Lng = fopen(ARQUIVO_LNG, "r");
	if(Lng == NULL) return;

	int Pid;
	int Impressos = 0;
	while(Impressos < MAX_IMPRESSOS && fscanf(Lng, "%d", &Pid) == 1)
	{
		if(Impressos == 0) printf("Analista:");
		printf(" %d", Pid);
		Impressos++;
	}

	if(Impressos > 0) printf("\n");

	// A saída pode ser um arquivo ou pipe, em que o printf só escreve quando o buffer enche:
	// sem esvaziá-lo, o texto ficaria retido enquanto o processo dorme
	fflush(stdout);

	// O que não foi impresso é copiado para um arquivo novo, que então substitui o LNG
	FILE *Restante = fopen(ARQUIVO_LNG_TEMP, "w");
	int Caractere;
	while((Caractere = fgetc(Lng)) != EOF)
		fputc(Caractere, Restante);

	fclose(Lng);
	fclose(Restante);
	rename(ARQUIVO_LNG_TEMP, ARQUIVO_LNG);
}

int main()
{
	FILE *ArquivoPid = fopen(ARQUIVO_PID_ANALISTA, "w");
	fprintf(ArquivoPid, "%d", getpid());
	fclose(ArquivoPid);

	// Sem O_EXCL: abre o semáforo se o Atendimento já o criou, ou cria com valor 1 (livre)
	sem_t *SemBlock = sem_open(SEM_BLOCK, O_CREAT, 0644, 1);

	while(1)
	{
		// Nasce dormindo e volta a dormir após cada impressão, até o Atendente enviar SIGCONT
		raise(SIGSTOP);

		sem_wait(SemBlock);
		//*** Início da região crítica: o Atendente também escreve no LNG ***
		ImprimePrimeiros();
		//*** Fim da região crítica ***
		sem_post(SemBlock);
	}

	return 0;
}
