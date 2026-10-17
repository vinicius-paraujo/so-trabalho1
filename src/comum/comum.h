#ifndef COMUM_H
#define COMUM_H

// Nomes compartilhados entre os processos Atendimento, Analista e Cliente

#define ARQUIVO_DEMANDA       "demanda.txt"       // fixado pelo cliente fornecido
#define ARQUIVO_LNG           "lng.txt"           // Lista de Números Gerados: PIDs já atendidos
#define ARQUIVO_LNG_TEMP      "lng.tmp"
#define ARQUIVO_PID_ANALISTA  "pid_analista.txt"

#define SEM_ATEND "/sem_atend"                    // fixado pelo cliente fornecido
#define SEM_BLOCK "/sem_block"                    // protege o arquivo LNG

#define MAX_IMPRESSOS 10

#endif
