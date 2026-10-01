package br.ufc.so.escalonamento.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.domain.ProcessState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Oráculos compartilhados pelos testes dos escalonadores. As métricas são recalculadas aqui,
 * de forma independente de {@code MetricsCalculator}, para que um erro no cálculo oficial não
 * seja mascarado pelos próprios testes.
 */
public final class SchedulingAssertions {
    private SchedulingAssertions() {
    }

    public static List<ProcessControlBlock> processes(int[]... definitions) {
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

    public static List<ProcessControlBlock> referenceProcesses() {
        return processes(
                new int[] {0, 5, 2},
                new int[] {0, 2, 3},
                new int[] {1, 4, 1},
                new int[] {3, 3, 4});
    }

    public static void assertCompletionTimes(SchedulingResult result, Integer... expected) {
        assertEquals(
                Arrays.asList(expected),
                result.processes().stream().map(ProcessControlBlock::completionTime).toList());
    }

    public static void assertMetrics(
            SchedulingResult result,
            double expectedTurnaround,
            double expectedWaitingTime,
            int expectedContextSwitches) {
        assertEquals(expectedTurnaround, averageTurnaround(result), 1e-9);
        assertEquals(expectedWaitingTime, averageWaitingTime(result), 1e-9);
        assertEquals(expectedContextSwitches, contextSwitches(result));
    }

    /** Invariantes válidas para qualquer algoritmo e entrada válida. */
    public static void assertValidSimulation(List<ProcessControlBlock> input, SchedulingResult result) {
        assertEquals(input.size(), result.processes().size());

        int busySeconds = 0;
        for (int process : result.timeline()) {
            if (process != SchedulingResult.IDLE) {
                busySeconds++;
            }
        }

        int totalDuration = 0;
        for (ProcessControlBlock process : result.processes()) {
            totalDuration += process.duration();
            assertEquals(ProcessState.TERMINATED, process.state());
            assertEquals(0, process.remainingTime());

            int executedSeconds = 0;
            int lastExecution = -1;
            for (int instant = 0; instant < result.timeline().size(); instant++) {
                if (result.timeline().get(instant) == process.id()) {
                    assertTrue(
                            instant >= process.arrivalTime(),
                            "P" + process.id() + " executou antes de sua criação.");
                    executedSeconds++;
                    lastExecution = instant;
                }
            }

            assertEquals(process.duration(), executedSeconds);
            assertEquals(lastExecution + 1, process.completionTime());
            assertTrue(process.completionTime() - process.arrivalTime() - process.duration() >= 0);
        }
        assertEquals(totalDuration, busySeconds);

        for (ProcessControlBlock process : input) {
            assertEquals(ProcessState.NEW, process.state(), "A entrada original não pode ser alterada.");
            assertEquals(process.duration(), process.remainingTime());
        }
    }

    public static double averageTurnaround(SchedulingResult result) {
        return result.processes().stream()
                .mapToInt(process -> process.completionTime() - process.arrivalTime())
                .average()
                .orElseThrow();
    }

    public static double averageWaitingTime(SchedulingResult result) {
        return result.processes().stream()
                .mapToInt(process -> process.completionTime()
                        - process.arrivalTime()
                        - process.duration())
                .average()
                .orElseThrow();
    }

    public static int contextSwitches(SchedulingResult result) {
        int switches = 0;
        int previousProcess = SchedulingResult.IDLE;

        for (int process : result.timeline()) {
            if (previousProcess != SchedulingResult.IDLE
                    && process != SchedulingResult.IDLE
                    && previousProcess != process) {
                switches++;
            }
            previousProcess = process;
        }

        return switches;
    }
}
