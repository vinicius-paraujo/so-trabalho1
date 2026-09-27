package br.ufc.so.escalonamento.metrics;

public record SchedulingMetrics(
        double averageTurnaround,
        double averageWaitingTime,
        int contextSwitches) {

    public SchedulingMetrics {
        if (averageTurnaround < 0) {
            throw new IllegalArgumentException("O turnaround médio não pode ser negativo.");
        }
        if (averageWaitingTime < 0) {
            throw new IllegalArgumentException("A espera média não pode ser negativa.");
        }
        if (contextSwitches < 0) {
            throw new IllegalArgumentException("O número de trocas de contexto não pode ser negativo.");
        }
    }
}
