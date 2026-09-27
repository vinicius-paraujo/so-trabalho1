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
 * Round-Robin com prioridade dinâmica e aging. A prioridade é reavaliada somente nos limites
 * do quantum; chegadas mais prioritárias não interrompem o quantum em andamento.
 */
public final class PriorityRoundRobinScheduler {
    private final int quantum;
    private final int agingRate;
    private final Random random;

    public PriorityRoundRobinScheduler(int quantum, int agingRate) {
        this(quantum, agingRate, new Random());
    }

    public PriorityRoundRobinScheduler(int quantum, int agingRate, Random random) {
        if (quantum <= 0) {
            throw new IllegalArgumentException("O quantum deve ser maior que zero.");
        }
        if (agingRate < 0) {
            throw new IllegalArgumentException("A taxa de aging não pode ser negativa.");
        }
        this.quantum = quantum;
        this.agingRate = agingRate;
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

            ProcessControlBlock runningProcess = selectHighestDynamicPriority(readyProcesses);
            readyProcesses.remove(runningProcess);
            runningProcess.markRunning();
            runningProcess.resetDynamicPriority();

            int usedQuantum = 0;
            while (usedQuantum < quantum && runningProcess.remainingTime() > 0) {
                timeline.add(runningProcess.id());
                runningProcess.executeOneSecond();
                currentTime++;
                usedQuantum++;
                if (usedQuantum < quantum) {
                    admitArrivedProcesses(processes, readyProcesses, currentTime);
                }
            }

            // O aging ocorre somente quando o quantum é consumido por completo, inclusive se o
            // processo termina exatamente no limite. Quem chega no próprio limite ainda não
            // aguardou e, por isso, é admitido somente depois do envelhecimento.
            if (usedQuantum == quantum) {
                ageWaitingProcesses(readyProcesses);
            }
            admitArrivedProcesses(processes, readyProcesses, currentTime);

            if (runningProcess.remainingTime() == 0) {
                runningProcess.terminateAt(currentTime);
                terminatedProcesses++;
            } else {
                // Ao fim do quantum o processo deixa a CPU e disputa a próxima seleção como os
                // demais prontos, sem a preferência de quem ainda ocupa a CPU.
                runningProcess.markReady();
                readyProcesses.add(runningProcess);
            }
        }

        return new SchedulingResult(timeline, processes);
    }

    private void ageWaitingProcesses(List<ProcessControlBlock> readyProcesses) {
        for (ProcessControlBlock process : readyProcesses) {
            process.applyAging(agingRate);
        }
    }

    private ProcessControlBlock selectHighestDynamicPriority(List<ProcessControlBlock> readyProcesses) {
        int highestPriority = Integer.MIN_VALUE;
        for (ProcessControlBlock process : readyProcesses) {
            highestPriority = Math.max(highestPriority, process.dynamicPriority());
        }

        int shortestRemainingTime = Integer.MAX_VALUE;
        for (ProcessControlBlock process : readyProcesses) {
            if (process.dynamicPriority() == highestPriority) {
                shortestRemainingTime = Math.min(shortestRemainingTime, process.remainingTime());
            }
        }

        List<ProcessControlBlock> tiedProcesses = new ArrayList<>();
        for (ProcessControlBlock process : readyProcesses) {
            if (process.dynamicPriority() == highestPriority
                    && process.remainingTime() == shortestRemainingTime) {
                tiedProcesses.add(process);
            }
        }

        return chooseRandomly(tiedProcesses, random);
    }
}
