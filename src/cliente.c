#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>
#include <signal.h>
#include <semaphore.h>
#include <fcntl.h>

int main()
{

	int tempo;
	srand( (unsigned)time(NULL) );
	int x = rand()%10;

	// Sorteia a demanda: 15 (10% de chance), 5 (30%) ou 1 (60%)
	if(x == 0)
		tempo = 15;

	else if(x > 0 && x <= 3)
		tempo = 5;

	else
		tempo = 1;

	// Informa a demanda ao Atendimento pelo arquivo compartilhado
	FILE* demanda = fopen("demanda.txt", "w+");
	fprintf(demanda, "%d", tempo);
	fclose(demanda);

	// Dorme até o Atendente acordá-lo com SIGCONT
	raise(SIGSTOP);

	// Sem o semáforo criado pelo Atendimento, o cliente é atendido sem exclusão mútua
	sem_t *sem = sem_open("/sem_atend", O_RDWR);

	if(sem != SEM_FAILED) sem_wait(sem);

	// Simula o atendimento; usleep conta em microssegundos
	usleep(tempo);

	if(sem != SEM_FAILED) sem_post(sem);

	return 0;

}
