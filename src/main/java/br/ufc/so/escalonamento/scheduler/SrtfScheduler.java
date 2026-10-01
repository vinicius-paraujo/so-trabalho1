package br.ufc.so.escalonamento.scheduler;

import static br.ufc.so.escalonamento.scheduler.SchedulingSupport.admitArrivedProcesses;
import static br.ufc.so.escalonamento.scheduler.SchedulingSupport.chooseRandomly;
import static br.ufc.so.escalonamento.scheduler.SchedulingSupport.copyProcesses;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Shortest Remaining Time First, versão preemptiva do SJF. */
public final class SrtfScheduler {
    private final Random random;

    public SrtfScheduler() {
        this(new Random());
    }

    public SrtfScheduler(Random random) {
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

            // Tempo restante igual preserva o processo em execução.
            if (runningProcess != null
                    && shortestRemainingTime(readyProcesses) < runningProcess.remainingTime()) {
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

                runningProcess = selectShortestRemainingTime(readyProcesses);
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

    private int shortestRemainingTime(List<ProcessControlBlock> readyProcesses) {
        int shortestRemainingTime = Integer.MAX_VALUE;
        for (ProcessControlBlock process : readyProcesses) {
            shortestRemainingTime = Math.min(shortestRemainingTime, process.remainingTime());
        }
        return shortestRemainingTime;
    }

    private ProcessControlBlock selectShortestRemainingTime(List<ProcessControlBlock> readyProcesses) {
        int shortestRemainingTime = shortestRemainingTime(readyProcesses);

        List<ProcessControlBlock> tiedProcesses = new ArrayList<>();
        for (ProcessControlBlock process : readyProcesses) {
            if (process.remainingTime() == shortestRemainingTime) {
                tiedProcesses.add(process);
            }
        }

        return chooseRandomly(tiedProcesses, random);
    }
}
