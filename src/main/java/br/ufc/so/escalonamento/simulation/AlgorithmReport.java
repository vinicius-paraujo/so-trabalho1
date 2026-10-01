package br.ufc.so.escalonamento.simulation;

import br.ufc.so.escalonamento.metrics.SchedulingMetrics;
import br.ufc.so.escalonamento.scheduler.SchedulingResult;
import java.util.Objects;

/** Agrupa linha do tempo, PCBs finais e métricas de um algoritmo. */
public record AlgorithmReport(
        String algorithmName,
        SchedulingResult result,
        SchedulingMetrics metrics) {

    public AlgorithmReport {
        Objects.requireNonNull(algorithmName, "O nome do algoritmo é obrigatório.");
        Objects.requireNonNull(result, "O resultado da simulação é obrigatório.");
        Objects.requireNonNull(metrics, "As métricas são obrigatórias.");
    }
}
