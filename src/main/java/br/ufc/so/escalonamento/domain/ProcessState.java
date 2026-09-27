package br.ufc.so.escalonamento.domain;

/**
 * Estados do processo simulado. Não há estado de bloqueio, pois o enunciado não modela
 * operações de entrada e saída.
 */
public enum ProcessState {
    NEW,
    READY,
    RUNNING,
    TERMINATED
}

