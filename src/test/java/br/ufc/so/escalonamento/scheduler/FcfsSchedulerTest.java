package br.ufc.so.escalonamento.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.domain.ProcessState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class FcfsSchedulerTest {
    @Test
    void deveEscalonarOCasoDeReferencia() {
        SchedulingResult result = scheduler().schedule(processes(
                new int[] {0, 5, 2},
                new int[] {0, 2, 3},
                new int[] {1, 4, 1},
                new int[] {3, 3, 4}));

        assertEquals(
                List.of(2, 2, 1, 1, 1, 1, 1, 3, 3, 3, 3, 4, 4, 4),
                result.timeline());
        assertCompletionTimes(result, 7, 2, 11, 14);
        assertEquals(7.50, averageTurnaround(result), 1e-9);
        assertEquals(4.00, averageWaitingTime(result), 1e-9);
        assertEquals(3, contextSwitches(result));
    }

    @Test
    void deveRespeitarOrdemDeChegadaMesmoQuandoAEntradaNaoEstaOrdenada() {
        SchedulingResult result = scheduler().schedule(processes(
                new int[] {5, 1, 1},
                new int[] {0, 2, 2},
                new int[] {1, 1, 3}));

        assertEquals(List.of(2, 2, 3, 0, 0, 1), result.timeline());
        assertCompletionTimes(result, 6, 2, 3);
        assertEquals(5.0 / 3.0, averageTurnaround(result), 1e-9);
        assertEquals(1.0 / 3.0, averageWaitingTime(result), 1e-9);
        assertEquals(1, contextSwitches(result));
    }

    @Test
    void naoDevePreemptarQuandoProcessoMaisCurtoChega() {
        SchedulingResult result = scheduler().schedule(processes(
                new int[] {0, 5, 1},
                new int[] {2, 1, 3}));

        assertEquals(List.of(1, 1, 1, 1, 1, 2), result.timeline());
        assertCompletionTimes(result, 5, 6);
    }

    @Test
    void deveAplicarDesempateAleatorioAposOsDemaisCriterios() {
        SchedulingResult result = scheduler().schedule(processes(
                new int[] {0, 1, 1},
                new int[] {0, 1, 1}));

        assertEquals(List.of(2, 1), result.timeline());
    }

    @Test
    void deveTerminarTodosOsProcessos() {
        List<ProcessControlBlock> inputProcesses = processes(new int[] {2, 2, 1});

        SchedulingResult result = scheduler().schedule(inputProcesses);

        assertEquals(List.of(0, 0, 1, 1), result.timeline());
        assertEquals(ProcessState.TERMINATED, result.processes().getFirst().state());
        assertEquals(4, result.processes().getFirst().completionTime());
        assertEquals(ProcessState.NEW, inputProcesses.getFirst().state());
        assertEquals(2, inputProcesses.getFirst().remainingTime());
    }

    private FcfsScheduler scheduler() {
        return new FcfsScheduler(new Random(0));
    }

    private List<ProcessControlBlock> processes(int[]... definitions) {
        List<ProcessControlBlock> processes = new ArrayList<>();
        for (int index = 0; index < definitions.length; index++) {
            int[] definition = definitions[index];
            processes.add(new ProcessControlBlock(
                    index + 1,
                    definition[0],
                    definition[1],
                    definition[2]));
        }
        return processes;
    }

    private void assertCompletionTimes(SchedulingResult result, Integer... expected) {
        assertEquals(
                Arrays.asList(expected),
                result.processes().stream().map(ProcessControlBlock::completionTime).toList());
    }

    private double averageTurnaround(SchedulingResult result) {
        return result.processes().stream()
                .mapToInt(process -> process.completionTime() - process.arrivalTime())
                .average()
                .orElseThrow();
    }

    private double averageWaitingTime(SchedulingResult result) {
        return result.processes().stream()
                .mapToInt(process -> process.completionTime()
                        - process.arrivalTime()
                        - process.duration())
                .average()
                .orElseThrow();
    }

    private int contextSwitches(SchedulingResult result) {
        int switches = 0;
        int previousProcess = SchedulingResult.IDLE;

        for (int process : result.timeline()) {
            if (process == SchedulingResult.IDLE) {
                previousProcess = SchedulingResult.IDLE;
            } else if (previousProcess != SchedulingResult.IDLE && previousProcess != process) {
                switches++;
                previousProcess = process;
            } else {
                previousProcess = process;
            }
        }

        return switches;
    }
}
