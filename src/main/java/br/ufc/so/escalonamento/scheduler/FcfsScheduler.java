package br.ufc.so.escalonamento.scheduler;

import static br.ufc.so.escalonamento.scheduler.SchedulingSupport.admitArrivedProcesses;
import static br.ufc.so.escalonamento.scheduler.SchedulingSupport.chooseRandomly;
import static br.ufc.so.escalonamento.scheduler.SchedulingSupport.copyProcesses;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * First Come, First Served: executa o pronto de criação mais antiga até o fim, sem preempção.
 * Empates de criação seguem os critérios do enunciado: menor tempo restante e, por fim, sorteio.
 */
public final class FcfsScheduler {
    private final Random random;

    public FcfsScheduler() {
        this(new Random());
    }

    public FcfsScheduler(Random random) {
        this.random = Objects.requireNonNull(random, "A fonte aleatória é obrigatória.");
    }

    public SchedulingResult schedule(List<ProcessControlBlock> inputProcesses) {
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

        return chooseRandomly(tiedProcesses, random);
    }
}
