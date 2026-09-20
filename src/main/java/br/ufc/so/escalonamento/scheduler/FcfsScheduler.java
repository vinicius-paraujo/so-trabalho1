package br.ufc.so.escalonamento.scheduler;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.domain.ProcessState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class FcfsScheduler {
    private final Random random;

    public FcfsScheduler() {
        this(new Random());
    }

    public FcfsScheduler(Random random) {
        this.random = Objects.requireNonNull(random, "A fonte aleatória é obrigatória.");
    }

    public SchedulingResult schedule(List<ProcessControlBlock> inputProcesses) {
        Objects.requireNonNull(inputProcesses, "A lista de processos é obrigatória.");
        if (inputProcesses.isEmpty()) {
            throw new IllegalArgumentException("A lista de processos não pode estar vazia.");
        }

        List<ProcessControlBlock> processes = copyProcesses(inputProcesses);
        List<ProcessControlBlock> readyProcesses = new ArrayList<>();
        List<Integer> timeline = new ArrayList<>();

        int currentTime = 0;
        int terminatedProcesses = 0;

        while (terminatedProcesses < processes.size()) {
            admitArrivedProcesses(processes, readyProcesses, currentTime);

            if (readyProcesses.isEmpty()) {
                timeline.add(SchedulingResult.IDLE);
                currentTime++;
                continue;
            }

            ProcessControlBlock runningProcess = selectNextProcess(readyProcesses);
            readyProcesses.remove(runningProcess);
            runningProcess.markRunning();

            while (runningProcess.remainingTime() > 0) {
                timeline.add(runningProcess.id());
                runningProcess.executeOneSecond();
                currentTime++;
                admitArrivedProcesses(processes, readyProcesses, currentTime);
            }

            runningProcess.terminateAt(currentTime);
            terminatedProcesses++;
        }

        return new SchedulingResult(timeline, processes);
    }

    private List<ProcessControlBlock> copyProcesses(List<ProcessControlBlock> inputProcesses) {
        List<ProcessControlBlock> copies = new ArrayList<>();
        for (ProcessControlBlock process : inputProcesses) {
            copies.add(Objects.requireNonNull(process, "Um processo da entrada é nulo.").freshCopy());
        }
        return copies;
    }

    private void admitArrivedProcesses(
            List<ProcessControlBlock> processes,
            List<ProcessControlBlock> readyProcesses,
            int currentTime) {
        for (ProcessControlBlock process : processes) {
            if (process.state() == ProcessState.NEW && process.arrivalTime() <= currentTime) {
                process.markReady();
                readyProcesses.add(process);
            }
        }
    }

    private ProcessControlBlock selectNextProcess(List<ProcessControlBlock> readyProcesses) {
        int earliestArrival = Integer.MAX_VALUE;
        for (ProcessControlBlock process : readyProcesses) {
            earliestArrival = Math.min(earliestArrival, process.arrivalTime());
        }

        int shortestRemainingTime = Integer.MAX_VALUE;
        for (ProcessControlBlock process : readyProcesses) {
            if (process.arrivalTime() == earliestArrival) {
                shortestRemainingTime = Math.min(shortestRemainingTime, process.remainingTime());
            }
        }

        List<ProcessControlBlock> tiedProcesses = new ArrayList<>();
        for (ProcessControlBlock process : readyProcesses) {
            if (process.arrivalTime() == earliestArrival
                    && process.remainingTime() == shortestRemainingTime) {
                tiedProcesses.add(process);
            }
        }

        if (tiedProcesses.size() == 1) {
            return tiedProcesses.getFirst();
        }
        return tiedProcesses.get(random.nextInt(tiedProcesses.size()));
    }
}
