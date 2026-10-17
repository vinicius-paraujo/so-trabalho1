#include <stdio.h>
#include <stdlib.h>

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

	printf("Atendimento: N = %ld, X = %ld ms\n", N, X);

	return 0;
}
