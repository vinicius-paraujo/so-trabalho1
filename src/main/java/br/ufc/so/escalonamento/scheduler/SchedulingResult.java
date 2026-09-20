package br.ufc.so.escalonamento.scheduler;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import java.util.List;
import java.util.Objects;

public record SchedulingResult(
        List<Integer> timeline,
        List<ProcessControlBlock> processes) {

    public static final int IDLE = 0;

    public SchedulingResult {
        timeline = List.copyOf(Objects.requireNonNull(timeline, "A linha do tempo é obrigatória."));
        processes = List.copyOf(Objects.requireNonNull(processes, "Os processos são obrigatórios."));
    }
}

