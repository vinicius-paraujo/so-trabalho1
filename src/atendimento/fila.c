#include "fila.h"

void IniciaFila(FILA *Fila, long X)
{
	Fila->Tamanho = 0;
	Fila->X = X;
	Fila->Trava = 0;
}

long PacienciaCliente(CLIENTE *Cliente, long X)
{
	return (Cliente->Prioridade == PRIORIDADE_ALTA) ? X / 2 : X;
}

// Último instante em que o atendimento pode terminar com o cliente ainda satisfeito
static long PrazoCliente(CLIENTE *Cliente, long X)
{
	return Cliente->HoraChegada + PacienciaCliente(Cliente, X);
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

// Entre os clientes que ainda ficarão satisfeitos se o atendimento terminar em PrevisaoTerminoMs,
// escolhe o de prazo mais próximo. Atender quem já não pode ser satisfeito só atrasaria os demais:
// esses ficam para quando não houver mais ninguém a salvar, e então sai o mais antigo (posição 0).
static int EscolheCliente(FILA *Fila, long PrevisaoTerminoMs)
{
	int  Escolhido = 0;
	long MenorPrazo = -1;

	for(int i = 0; i < Fila->Tamanho; i++)
	{
		long Prazo = PrazoCliente(&Fila->Clientes[i], Fila->X);

		if(Prazo >= PrevisaoTerminoMs && (MenorPrazo < 0 || Prazo < MenorPrazo))
		{
			Escolhido = i;
			MenorPrazo = Prazo;
		}
	}

	return Escolhido;
}

int RetiraFila(FILA *Fila, CLIENTE *Cliente, long PrevisaoTerminoMs)
{
	int Retirou = 0;

	EntraRegiaoCritica(&Fila->Trava);

	if(Fila->Tamanho > 0)
	{
		int Escolhido = EscolheCliente(Fila, PrevisaoTerminoMs);
		*Cliente = Fila->Clientes[Escolhido];

		for(int i = Escolhido + 1; i < Fila->Tamanho; i++)
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
