#include "relogio.h"

#include <time.h>

static struct timespec Inicio;

// Relógio monotônico: não volta nem salta se a hora do sistema for ajustada durante a execução
void IniciaRelogio()
{
	clock_gettime(CLOCK_MONOTONIC, &Inicio);
}

long TempoDecorridoMs()
{
	struct timespec Agora;
	clock_gettime(CLOCK_MONOTONIC, &Agora);

	return (Agora.tv_sec - Inicio.tv_sec) * 1000L + (Agora.tv_nsec - Inicio.tv_nsec) / 1000000L;
}
