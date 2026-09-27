package br.ufc.so.escalonamento.simulation;

import static br.ufc.so.escalonamento.scheduler.SchedulingAssertions.referenceProcesses;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.ufc.so.escalonamento.input.SchedulerConfiguration;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SimulationRunnerTest {
    private final SimulationRunner runner = new SimulationRunner(() -> new Random(0));

    @Test
    void deveExecutarOsSeteAlgoritmosNaOrdemDoEnunciado() {
        List<AlgorithmReport> reports = runner.runAll(
                referenceProcesses(), new SchedulerConfiguration(2, 1));

        assertEquals(
                List.of(
                        "FCFS",
                        "SJF",
                        "SRTF",
                        "PRIORIDADE_SEM_PREEMPCAO",
                        "PRIORIDADE_COM_PREEMPCAO",
                        "ROUND_ROBIN",
                        "ROUND_ROBIN_PRIORIDADE_AGING"),
                reports.stream().map(AlgorithmReport::algorithmName).toList());
    }

    @Test
    void deveProduzirAsMetricasDaTabelaDeReferencia() {
        List<AlgorithmReport> reports = runner.runAll(
                referenceProcesses(), new SchedulerConfiguration(2, 1));

        double[][] expected = {
            {7.50, 4.00, 3},
            {6.75, 3.25, 3},
            {6.75, 3.25, 3},
            {7.25, 3.75, 3},
            {7.00, 3.50, 4},
            {9.75, 6.25, 7},
            {7.75, 4.25, 5},
        };
        for (int index = 0; index < expected.length; index++) {
            AlgorithmReport report = reports.get(index);
            assertEquals(expected[index][0], report.metrics().averageTurnaround(), 1e-9, report.algorithmName());
            assertEquals(expected[index][1], report.metrics().averageWaitingTime(), 1e-9, report.algorithmName());
            assertEquals((int) expected[index][2], report.metrics().contextSwitches(), report.algorithmName());
        }
    }

    @Test
    void deveRepassarQuantumEAgingAosRoundRobins() {
        List<AlgorithmReport> reports = runner.runAll(
                referenceProcesses(), new SchedulerConfiguration(20, 0));

        // Com quantum maior que qualquer duração, o Round-Robin se comporta como FCFS em FIFO.
        assertEquals(
                List.of(1, 1, 1, 1, 1, 2, 2, 3, 3, 3, 3, 4, 4, 4),
                reports.get(5).result().timeline());
    }
}
