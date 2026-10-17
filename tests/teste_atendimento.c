#include "teste.h"
#include "atendente.h"
#include "comum.h"

#include <dirent.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

#define MAX_PIDS 1000

static void TestaSatisfacao()
{
	CLIENTE Alta  = { 1, 100, PRIORIDADE_ALTA, 1 };
	CLIENTE Baixa = { 2, 100, PRIORIDADE_BAIXA, 1 };

	// X = 40: alta tolera 20 ms, baixa 40 ms; o limite é inclusivo
	VERIFICA(ClienteSatisfeito(&Alta, 120, 40));
	VERIFICA(!ClienteSatisfeito(&Alta, 121, 40));
	VERIFICA(ClienteSatisfeito(&Baixa, 140, 40));
	VERIFICA(!ClienteSatisfeito(&Baixa, 141, 40));
}

typedef struct
{
	int  Codigo;
	int  Pids[MAX_PIDS];
	int  QuantidadePids;
	long Satisfeitos;
	long Atendidos;
	long TempoMs;
	int  ResumoLido;
	int  CodigoInformado;      // "codigo=N" impresso pelo próprio comando de teste
	int  AvisoAnalista;

} EXECUCAO;

// Executa o programa completo e interpreta sua saída
static EXECUCAO Executa(const char *Comando)
{
	EXECUCAO Execucao;
	memset(&Execucao, 0, sizeof(Execucao));

	FILE *Saida = popen(Comando, "r");
	char Linha[4096];

	while(fgets(Linha, sizeof(Linha), Saida) != NULL)
	{
		if(strncmp(Linha, "Analista:", 9) == 0)
		{
			char *Resto = Linha + 9;
			int Pid, Lidos;
			while(sscanf(Resto, "%d%n", &Pid, &Lidos) == 1 && Execucao.QuantidadePids < MAX_PIDS)
			{
				Execucao.Pids[Execucao.QuantidadePids++] = Pid;
				Resto += Lidos;
			}
		}
		else if(sscanf(Linha, "Taxa de satisfacao: %*f (%ld/%ld)", &Execucao.Satisfeitos, &Execucao.Atendidos) == 2)
			Execucao.ResumoLido++;
		else if(sscanf(Linha, "Tempo total: %ld ms", &Execucao.TempoMs) == 1)
			Execucao.ResumoLido++;
		else if(sscanf(Linha, "codigo=%d", &Execucao.CodigoInformado) == 1)
			;
		else if(strstr(Linha, "Analista terminou") != NULL)
			Execucao.AvisoAnalista = 1;
	}

	Execucao.Codigo = pclose(Saida);
	return Execucao;
}

static int PidsDistintos(EXECUCAO *Execucao)
{
	for(int i = 0; i < Execucao->QuantidadePids; i++)
		for(int j = i + 1; j < Execucao->QuantidadePids; j++)
			if(Execucao->Pids[i] == Execucao->Pids[j])
				return 0;
	return 1;
}

static int SemaforosRemanescentes()
{
	int Encontrados = 0;
	DIR *Diretorio = opendir("/dev/shm");
	struct dirent *Entrada;

	while(Diretorio != NULL && (Entrada = readdir(Diretorio)) != NULL)
		if(strcmp(Entrada->d_name, "sem.sem_atend") == 0 || strcmp(Entrada->d_name, "sem.sem_block") == 0)
			Encontrados++;

	if(Diretorio != NULL) closedir(Diretorio);
	return Encontrados;
}

static void TestaExecucao(const char *Comando, int N)
{
	EXECUCAO Execucao = Executa(Comando);

	VERIFICA(Execucao.Codigo == 0);
	VERIFICA(Execucao.ResumoLido == 2);
	VERIFICA(Execucao.Atendidos == N);
	VERIFICA(Execucao.Satisfeitos >= 0 && Execucao.Satisfeitos <= N);

	// Todo cliente atendido teve o PID impresso pelo Analista, uma única vez
	VERIFICA(Execucao.QuantidadePids == N);
	VERIFICA(PidsDistintos(&Execucao));

	VERIFICA(SemaforosRemanescentes() == 0);
	VERIFICA(access(ARQUIVO_LNG, F_OK) != 0);
	VERIFICA(access(ARQUIVO_DEMANDA, F_OK) != 0);
}

// Conta processos com o nome dado ainda existentes, inclusive parados
static int ProcessosRemanescentes(const char *Programa)
{
	int Encontrados = 0;
	DIR *Diretorio = opendir("/proc");
	struct dirent *Entrada;

	while(Diretorio != NULL && (Entrada = readdir(Diretorio)) != NULL)
	{
		char Caminho[300], Nome[64] = "";
		snprintf(Caminho, sizeof(Caminho), "/proc/%s/comm", Entrada->d_name);

		FILE *Arquivo = fopen(Caminho, "r");
		if(Arquivo == NULL) continue;
		fscanf(Arquivo, "%63s", Nome);
		fclose(Arquivo);

		if(strcmp(Nome, Programa) == 0) Encontrados++;
	}

	if(Diretorio != NULL) closedir(Diretorio);
	return Encontrados;
}

// N = 0: a tecla "s" chega pela entrada padrão depois de um tempo de execução
static void TestaModoInfinito(const char *Comando)
{
	EXECUCAO Execucao = Executa(Comando);

	VERIFICA(Execucao.Codigo == 0);
	VERIFICA(Execucao.ResumoLido == 2);
	VERIFICA(Execucao.Atendidos > 0);

	// O LNG foi esvaziado antes da saída: todo cliente atendido teve o PID impresso
	VERIFICA(Execucao.QuantidadePids == Execucao.Atendidos);
	VERIFICA(PidsDistintos(&Execucao));

	// Clientes que ficaram na fila foram encerrados, sem sobrar processos parados
	VERIFICA(ProcessosRemanescentes("cliente") == 0);
	VERIFICA(SemaforosRemanescentes() == 0);
	VERIFICA(access(ARQUIVO_LNG, F_OK) != 0);
}

// Processos encerrados com SIGKILL somem da tabela logo depois, quando o init os recolhe
static int EsperaSumirem(const char *Programa)
{
	for(int Tentativa = 0; Tentativa < 20 && ProcessosRemanescentes(Programa) > 0; Tentativa++)
		usleep(50000);

	return ProcessosRemanescentes(Programa);
}

static void TestaInterrupcao()
{
	EXECUCAO Execucao = Executa("./atendimento 0 100 & P=$!; sleep 0.7; kill -INT $P; wait $P; echo codigo=$?");

	// 130 = encerrado pelo tratador de Ctrl+C
	VERIFICA(Execucao.CodigoInformado == 130);
	VERIFICA(EsperaSumirem("cliente") == 0);
	VERIFICA(EsperaSumirem("analista") == 0);
	VERIFICA(SemaforosRemanescentes() == 0);
	VERIFICA(access(ARQUIVO_LNG, F_OK) != 0);
	VERIFICA(access(ARQUIVO_DEMANDA, F_OK) != 0);
}

// Se o Analista morrer, o Atendimento não pode esperar por ele para sempre
static void TestaAnalistaMorto()
{
	EXECUCAO Execucao = Executa("timeout 20 ./atendimento 200 50 2>&1 & P=$!; sleep 0.3; pkill -KILL -x analista; wait $P; echo codigo=$?");

	VERIFICA(Execucao.CodigoInformado == 0);
	VERIFICA(Execucao.AvisoAnalista);
	VERIFICA(Execucao.Atendidos == 200);
	VERIFICA(SemaforosRemanescentes() == 0);
}

int main()
{
	TestaSatisfacao();

	// Quantidade que não é múltipla de 10: a última leva do Analista é incompleta
	TestaExecucao("./atendimento 25 1000", 25);

	// Mais clientes que a capacidade da fila e paciência curta
	TestaExecucao("./atendimento 250 50", 250);

	TestaModoInfinito("(sleep 2; printf s) | ./atendimento 0 100");

	// Teclas diferentes de "s" são ignoradas
	TestaModoInfinito("(sleep 1; printf xq; sleep 1; printf s) | ./atendimento 0 20");

	TestaInterrupcao();
	TestaAnalistaMorto();

	return RESUMO_TESTES();
}
