package br.ufc.so.escalonamento.simulation;

import static br.ufc.so.escalonamento.scheduler.SchedulingAssertions.assertValidSimulation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.input.SchedulerConfiguration;
import br.ufc.so.escalonamento.scheduler.SchedulingAssertions;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

/**
 * Validação integrada (T14): entradas geradas com semente fixa são submetidas aos sete
 * algoritmos, e cada resultado é confrontado com as invariantes do CT-18 e com o cálculo
 * independente das métricas.
 */
class SimulationInvariantTest {
    private static final int MAX_PROCESSES = 8;
    private static final int MAX_ARRIVAL = 12;
    private static final int MAX_DURATION = 6;
    private static final int MAX_PRIORITY = 5;

    @RepeatedTest(300)
    void deveRespeitarAsInvariantesEmEntradasGeradas(RepetitionInfo repetition) {
        Random generator = new Random(repetition.getCurrentRepetition());
        List<ProcessControlBlock> processes = generateProcesses(generator);
        SchedulerConfiguration configuration = new SchedulerConfiguration(
                1 + generator.nextInt(4),
                generator.nextInt(4));

        List<AlgorithmReport> reports =
                new SimulationRunner(() -> new Random(0)).runAll(processes, configuration);

        assertEquals(7, reports.size());
        for (AlgorithmReport report : reports) {
            assertValidSimulation(processes, report.result());
            assertEquals(
                    SchedulingAssertions.averageTurnaround(report.result()),
                    report.metrics().averageTurnaround(),
                    1e-9,
                    report.algorithmName());
            assertEquals(
                    SchedulingAssertions.averageWaitingTime(report.result()),
                    report.metrics().averageWaitingTime(),
                    1e-9,
                    report.algorithmName());
            assertEquals(
                    SchedulingAssertions.contextSwitches(report.result()),
                    report.metrics().contextSwitches(),
                    report.algorithmName());
        }
    }

    /**
     * O SRTF é o SRPT de tempo discreto, ótimo para o turnaround médio em uma CPU com
     * preempção. Nenhum outro algoritmo pode obter média menor sobre a mesma entrada.
     */
    @RepeatedTest(300)
    void srtfDeveTerOMenorTurnaroundMedio(RepetitionInfo repetition) {
        Random generator = new Random(10_000L + repetition.getCurrentRepetition());
        List<ProcessControlBlock> processes = generateProcesses(generator);

        List<AlgorithmReport> reports = new SimulationRunner(() -> new Random(0))
                .runAll(processes, new SchedulerConfiguration(1 + generator.nextInt(4), generator.nextInt(4)));

        double srtfTurnaround = reports.get(2).metrics().averageTurnaround();
        for (AlgorithmReport report : reports) {
            assertTrue(
                    srtfTurnaround <= report.metrics().averageTurnaround() + 1e-9,
                    report.algorithmName() + " superou o SRTF.");
        }
    }

    @RepeatedTest(50)
    void deveSerDeterministicoComAMesmaSemente(RepetitionInfo repetition) {
        Random generator = new Random(20_000L + repetition.getCurrentRepetition());
        List<ProcessControlBlock> processes = generateProcesses(generator);
        SchedulerConfiguration configuration = new SchedulerConfiguration(2, 1);

        List<AlgorithmReport> first = new SimulationRunner(() -> new Random(7)).runAll(processes, configuration);
        List<AlgorithmReport> second = new SimulationRunner(() -> new Random(7)).runAll(processes, configuration);

        for (int index = 0; index < first.size(); index++) {
            assertEquals(first.get(index).result().timeline(), second.get(index).result().timeline());
        }
    }

    private List<ProcessControlBlock> generateProcesses(Random generator) {
        int count = 1 + generator.nextInt(MAX_PROCESSES);
        List<ProcessControlBlock> processes = new ArrayList<>();
        for (int id = 1; id <= count; id++) {
            processes.add(new ProcessControlBlock(
                    id,
                    generator.nextInt(MAX_ARRIVAL + 1),
                    1 + generator.nextInt(MAX_DURATION),
                    1 + generator.nextInt(MAX_PRIORITY)));
        }
        return processes;
    }
}
