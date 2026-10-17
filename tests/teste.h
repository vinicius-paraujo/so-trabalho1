#ifndef TESTE_H
#define TESTE_H

#include <stdio.h>

// Cada arquivo de teste é um executável próprio, com seus próprios contadores.
static int testes_total  = 0;
static int testes_falhas = 0;

// Diferente de assert(), não aborta na primeira falha: registra e continua,
// para que uma execução mostre todas as falhas de uma vez.
// O do { } while(0) faz a macro valer como um único comando dentro de if/else sem chaves.
#define VERIFICA(expressao)                                                   \
	do {                                                                      \
		testes_total++;                                                       \
		if(!(expressao))                                                      \
		{                                                                     \
			testes_falhas++;                                                  \
			fprintf(stderr, "  FALHA  %s:%d  %s\n",                           \
			        __FILE__, __LINE__, #expressao);                          \
		}                                                                     \
	} while(0)

// O valor resultante é o código de saída do executável; o make interrompe se for diferente de 0.
#define RESUMO_TESTES()                                                       \
	(printf("  %-28s %d/%d verificacoes passaram\n", __FILE__,                \
	        testes_total - testes_falhas, testes_total),                      \
	 testes_falhas == 0 ? 0 : 1)

#endif
