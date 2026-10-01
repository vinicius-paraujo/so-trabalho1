package br.ufc.so.escalonamento.scheduler;

import static br.ufc.so.escalonamento.scheduler.SchedulingSupport.admitArrivedProcesses;
import static br.ufc.so.escalonamento.scheduler.SchedulingSupport.chooseRandomly;
import static br.ufc.so.escalonamento.scheduler.SchedulingSupport.copyProcesses;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Shortest Job First não preemptivo. */
public final class SjfScheduler {
    private final Random random;

    public SjfScheduler() {
        this(new Random());
    }

    public SjfScheduler(Random random) {
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

            ProcessControlBlock runningProcess = selectShortestJob(readyProcesses);
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

    /** Em processos nunca iniciados, duração e tempo restante são iguais. */
    private ProcessControlBlock selectShortestJob(List<ProcessControlBlock> readyProcesses) {
        int shortestDuration = Integer.MAX_VALUE;
        for (ProcessControlBlock process : readyProcesses) {
            shortestDuration = Math.min(shortestDuration, process.duration());
        }

        List<ProcessControlBlock> tiedProcesses = new ArrayList<>();
        for (ProcessControlBlock process : readyProcesses) {
            if (process.duration() == shortestDuration) {
                tiedProcesses.add(process);
            }
        }

        return chooseRandomly(tiedProcesses, random);
    }
}
