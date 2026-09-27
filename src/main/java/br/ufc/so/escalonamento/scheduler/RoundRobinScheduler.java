package br.ufc.so.escalonamento.scheduler;

import static br.ufc.so.escalonamento.scheduler.SchedulingSupport.admitArrivedProcesses;
import static br.ufc.so.escalonamento.scheduler.SchedulingSupport.copyProcesses;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Round-Robin sem prioridade. A ordem da fila FIFO é a própria regra de seleção; por isso,
 * não há empate a resolver e nenhuma fonte aleatória é necessária.
 */
public final class RoundRobinScheduler {
    private final int quantum;

    public RoundRobinScheduler(int quantum) {
        if (quantum <= 0) {
            throw new IllegalArgumentException("O quantum deve ser maior que zero.");
        }
        this.quantum = quantum;
    }

    public SchedulingResult schedule(List<ProcessControlBlock> inputProcesses) {
        List<ProcessControlBlock> processes = copyProcesses(inputProcesses);
        Deque<ProcessControlBlock> readyQueue = new ArrayDeque<>();
        List<Integer> timeline = new ArrayList<>();

        int currentTime = 0;
        int terminatedProcesses = 0;

        while (terminatedProcesses < processes.size()) {
            admitArrivedProcesses(processes, readyQueue, currentTime);

            if (readyQueue.isEmpty()) {
                timeline.add(SchedulingResult.IDLE);
                currentTime++;
                continue;
            }

            ProcessControlBlock runningProcess = readyQueue.pollFirst();
            runningProcess.markRunning();

            int usedQuantum = 0;
            while (usedQuantum < quantum && runningProcess.remainingTime() > 0) {
                timeline.add(runningProcess.id());
                runningProcess.executeOneSecond();
                currentTime++;
                usedQuantum++;
                // Chegadas no limite do quantum entram na fila antes da recolocação abaixo.
                admitArrivedProcesses(processes, readyQueue, currentTime);
            }

            if (runningProcess.remainingTime() == 0) {
                runningProcess.terminateAt(currentTime);
                terminatedProcesses++;
            } else {
                runningProcess.markReady();
                readyQueue.addLast(runningProcess);
            }
        }

        return new SchedulingResult(timeline, processes);
    }
}
