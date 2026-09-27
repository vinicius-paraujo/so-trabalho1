package br.ufc.so.escalonamento.scheduler;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.domain.ProcessState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Operações comuns aos escalonadores que não envolvem a política de seleção.
 * A escolha do próximo processo permanece em cada algoritmo.
 */
final class SchedulingSupport {
    private SchedulingSupport() {
    }

    static List<ProcessControlBlock> copyProcesses(List<ProcessControlBlock> inputProcesses) {
        Objects.requireNonNull(inputProcesses, "A lista de processos é obrigatória.");
        if (inputProcesses.isEmpty()) {
            throw new IllegalArgumentException("A lista de processos não pode estar vazia.");
        }

        List<ProcessControlBlock> copies = new ArrayList<>();
        for (ProcessControlBlock process : inputProcesses) {
            copies.add(Objects.requireNonNull(process, "Um processo da entrada é nulo.").freshCopy());
        }
        return copies;
    }

    /**
     * Admite na ordem dos identificadores, o que define a ordem FIFO do Round-Robin
     * para chegadas simultâneas.
     */
    static void admitArrivedProcesses(
            List<ProcessControlBlock> processes,
            Collection<ProcessControlBlock> readyProcesses,
            int currentTime) {
        for (ProcessControlBlock process : processes) {
            if (process.state() == ProcessState.NEW && process.arrivalTime() <= currentTime) {
                process.markReady();
                readyProcesses.add(process);
            }
        }
    }

    /**
     * Último critério de desempate. Os candidatos são ordenados por identificador para que
     * a mesma semente produza a mesma escolha, independentemente da ordem da fila.
     */
    static ProcessControlBlock chooseRandomly(List<ProcessControlBlock> tiedProcesses, Random random) {
        if (tiedProcesses.size() == 1) {
            return tiedProcesses.getFirst();
        }

        List<ProcessControlBlock> candidates = new ArrayList<>(tiedProcesses);
        candidates.sort(Comparator.comparingInt(ProcessControlBlock::id));
        return candidates.get(random.nextInt(candidates.size()));
    }
}
