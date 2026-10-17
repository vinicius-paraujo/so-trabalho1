#include "fila.h"

void IniciaFila(FILA *Fila)
{
	Fila->Tamanho = 0;
	Fila->Trava = 0;
}

// 1 se inseriu, 0 se a fila estava cheia
int InsereFila(FILA *Fila, CLIENTE Cliente)
{
	int Inseriu = 0;

	EntraRegiaoCritica(&Fila->Trava);

	if(Fila->Tamanho < CAPACIDADE_FILA)
	{
		Fila->Clientes[Fila->Tamanho] = Cliente;
		Fila->Tamanho++;
		Inseriu = 1;
	}

	SaiRegiaoCritica(&Fila->Trava);

	return Inseriu;
}

int RetiraFila(FILA *Fila, CLIENTE *Cliente)
{
	int Retirou = 0;

	EntraRegiaoCritica(&Fila->Trava);

	if(Fila->Tamanho > 0)
	{
		*Cliente = Fila->Clientes[0];

		for(int i = 1; i < Fila->Tamanho; i++)
			Fila->Clientes[i - 1] = Fila->Clientes[i];

		Fila->Tamanho--;
		Retirou = 1;
	}

	SaiRegiaoCritica(&Fila->Trava);

	return Retirou;
}

int TamanhoFila(FILA *Fila)
{
	EntraRegiaoCritica(&Fila->Trava);
	int Tamanho = Fila->Tamanho;
	SaiRegiaoCritica(&Fila->Trava);

	return Tamanho;
}
