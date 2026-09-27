package br.ufc.so.escalonamento.metrics;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.domain.ProcessState;
import br.ufc.so.escalonamento.scheduler.SchedulingResult;
import java.util.List;
import java.util.Objects;

/**
 * Calcula as métricas exigidas pelo enunciado a partir do resultado de qualquer algoritmo.
 * As médias não são arredondadas; o arredondamento ocorre somente na apresentação.
 */
public final class MetricsCalculator {
    public SchedulingMetrics calculate(SchedulingResult result) {
        Objects.requireNonNull(result, "O resultado da simulação é obrigatório.");
        return new SchedulingMetrics(
                averageTurnaround(result.processes()),
                averageWaitingTime(result.processes()),
                contextSwitches(result.timeline()));
    }

    /** turnaround = conclusão - criação. */
    public double averageTurnaround(List<ProcessControlBlock> processes) {
        requireTerminated(processes);

        long totalTurnaround = 0;
        for (ProcessControlBlock process : processes) {
            totalTurnaround += process.completionTime() - process.arrivalTime();
        }
        return (double) totalTurnaround / processes.size();
    }

    /** espera = turnaround - duração. */
    public double averageWaitingTime(List<ProcessControlBlock> processes) {
        requireTerminated(processes);

        long totalWaitingTime = 0;
        for (ProcessControlBlock process : processes) {
            totalWaitingTime += process.completionTime() - process.arrivalTime() - process.duration();
        }
        return (double) totalWaitingTime / processes.size();
    }

    /**
     * Conta somente a substituição direta de um processo por outro (ADR 0004): a carga inicial
     * e as transições que envolvem CPU ociosa não são trocas de contexto.
     */
    public int contextSwitches(List<Integer> timeline) {
        Objects.requireNonNull(timeline, "A linha do tempo é obrigatória.");

        int switches = 0;
        int previousProcess = SchedulingResult.IDLE;
        for (int currentProcess : timeline) {
            if (previousProcess != SchedulingResult.IDLE
                    && currentProcess != SchedulingResult.IDLE
                    && previousProcess != currentProcess) {
                switches++;
            }
            previousProcess = currentProcess;
        }
        return switches;
    }

    private void requireTerminated(List<ProcessControlBlock> processes) {
        Objects.requireNonNull(processes, "A lista de processos é obrigatória.");
        if (processes.isEmpty()) {
            throw new IllegalArgumentException("A lista de processos não pode estar vazia.");
        }
        for (ProcessControlBlock process : processes) {
            if (process.state() != ProcessState.TERMINATED) {
                throw new IllegalArgumentException(
                        "P" + process.id() + " não terminou; suas métricas não estão definidas.");
            }
        }
    }
}
