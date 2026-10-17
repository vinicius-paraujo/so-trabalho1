#include "teclado.h"

#include <fcntl.h>
#include <termios.h>
#include <unistd.h>

static struct termios TerminalOriginal;
static int EhTerminal = 0;
static int FlagsOriginais;
static int Configurado = 0;

// Por padrão, o terminal só entrega o que foi digitado após o Enter e read() espera até haver
// entrada. Aqui cada tecla passa a ser entregue na hora, sem eco, e read() retorna imediatamente.
void ConfiguraTeclado()
{
	if(tcgetattr(STDIN_FILENO, &TerminalOriginal) == 0)
	{
		EhTerminal = 1;

		struct termios Imediato = TerminalOriginal;
		Imediato.c_lflag &= ~(ICANON | ECHO);
		Imediato.c_cc[VMIN]  = 0;    // read() não espera nenhum caractere
		Imediato.c_cc[VTIME] = 0;    // nem tempo algum
		tcsetattr(STDIN_FILENO, TCSANOW, &Imediato);
	}
	else
	{
		// Entrada redirecionada (pipe ou arquivo)
		FlagsOriginais = fcntl(STDIN_FILENO, F_GETFL);
		fcntl(STDIN_FILENO, F_SETFL, FlagsOriginais | O_NONBLOCK);
	}

	Configurado = 1;
}

void RestauraTeclado()
{
	if(!Configurado) return;

	if(EhTerminal)
		tcsetattr(STDIN_FILENO, TCSANOW, &TerminalOriginal);
	else
		fcntl(STDIN_FILENO, F_SETFL, FlagsOriginais);
}

char TeclaPressionada()
{
	char Tecla;

	if(read(STDIN_FILENO, &Tecla, 1) == 1)
		return Tecla;

	return 0;
}
