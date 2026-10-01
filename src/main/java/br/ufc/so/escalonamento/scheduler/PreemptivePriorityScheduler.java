package br.ufc.so.escalonamento.scheduler;

import static br.ufc.so.escalonamento.scheduler.SchedulingSupport.admitArrivedProcesses;
import static br.ufc.so.escalonamento.scheduler.SchedulingSupport.chooseRandomly;
import static br.ufc.so.escalonamento.scheduler.SchedulingSupport.copyProcesses;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Prioridade estática preemptiva; valores maiores indicam maior prioridade. */
public final class PreemptivePriorityScheduler {
    private final Random random;

    public PreemptivePriorityScheduler() {
        this(new Random());
    }

    public PreemptivePriorityScheduler(Random random) {
        this.random = Objects.requireNonNull(random, "A fonte aleatória é obrigatória.");
    }

    public SchedulingResult schedule(List<ProcessControlBlock> inputProcesses) {
        List<ProcessControlBlock> processes = copyProcesses(inputProcesses);
        List<ProcessControlBlock> readyProcesses = new ArrayList<>();
        List<Integer> timeline = new ArrayList<>();

        ProcessControlBlock runningProcess = null;
        int currentTime = 0;
        int terminatedProcesses = 0;

        while (terminatedProcesses < processes.size()) {
            admitArrivedProcesses(processes, readyProcesses, currentTime);

            // Prioridade igual preserva o processo em execução.
            if (runningProcess != null
                    && highestPriority(readyProcesses) > runningProcess.staticPriority()) {
                runningProcess.markReady();
                readyProcesses.add(runningProcess);
                runningProcess = null;
            }

            if (runningProcess == null) {
                if (readyProcesses.isEmpty()) {
                    timeline.add(SchedulingResult.IDLE);
                    currentTime++;
                    continue;
                }

                runningProcess = selectHighestPriority(readyProcesses);
                readyProcesses.remove(runningProcess);
                runningProcess.markRunning();
            }

            timeline.add(runningProcess.id());
            runningProcess.executeOneSecond();
            currentTime++;

            if (runningProcess.remainingTime() == 0) {
                runningProcess.terminateAt(currentTime);
                terminatedProcesses++;
                runningProcess = null;
            }
        }

        return new SchedulingResult(timeline, processes);
    }

    private int highestPriority(List<ProcessControlBlock> readyProcesses) {
        int highestPriority = Integer.MIN_VALUE;
        for (ProcessControlBlock process : readyProcesses) {
            highestPriority = Math.max(highestPriority, process.staticPriority());
        }
        return highestPriority;
    }

    private ProcessControlBlock selectHighestPriority(List<ProcessControlBlock> readyProcesses) {
        int highestPriority = highestPriority(readyProcesses);

        int shortestRemainingTime = Integer.MAX_VALUE;
        for (ProcessControlBlock process : readyProcesses) {
            if (process.staticPriority() == highestPriority) {
                shortestRemainingTime = Math.min(shortestRemainingTime, process.remainingTime());
            }
        }

        List<ProcessControlBlock> tiedProcesses = new ArrayList<>();
        for (ProcessControlBlock process : readyProcesses) {
            if (process.staticPriority() == highestPriority
                    && process.remainingTime() == shortestRemainingTime) {
                tiedProcesses.add(process);
            }
        }

        return chooseRandomly(tiedProcesses, random);
    }
}
