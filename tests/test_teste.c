#include "teste.h"

int main()
{
	VERIFICA(1 + 1 == 2);

	int falhas_antes = testes_falhas;
	fprintf(stderr, "  (falha esperada a seguir)\n");
	VERIFICA(1 + 1 == 3);
	int falha_registrada = (testes_falhas == falhas_antes + 1);

	// Desconta a falha provocada acima, para que este executável termine com sucesso.
	testes_falhas--;
	testes_total--;

	VERIFICA(falha_registrada);

	return RESUMO_TESTES();
}
