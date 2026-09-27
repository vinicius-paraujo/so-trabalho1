package br.ufc.so.escalonamento.scheduler;

import static br.ufc.so.escalonamento.scheduler.SchedulingAssertions.assertCompletionTimes;
import static br.ufc.so.escalonamento.scheduler.SchedulingAssertions.assertMetrics;
import static br.ufc.so.escalonamento.scheduler.SchedulingAssertions.assertValidSimulation;
import static br.ufc.so.escalonamento.scheduler.SchedulingAssertions.processes;
import static br.ufc.so.escalonamento.scheduler.SchedulingAssertions.referenceProcesses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RoundRobinSchedulerTest {
    @Test
    void deveEscalonarOCasoDeReferencia() {
        SchedulingResult result = new RoundRobinScheduler(2).schedule(referenceProcesses());

        assertEquals(
                List.of(1, 1, 2, 2, 3, 3, 1, 1, 4, 4, 3, 3, 1, 4),
                result.timeline());
        assertCompletionTimes(result, 13, 4, 12, 14);
        assertMetrics(result, 9.75, 6.25, 7);
    }

    @Test
    void deveLiberarACpuQuandoOProcessoTerminaAntesDoQuantum() {
        SchedulingResult result = new RoundRobinScheduler(2).schedule(processes(
                new int[] {0, 1, 1},
                new int[] {0, 3, 1}));

        assertEquals(List.of(1, 2, 2, 2), result.timeline());
        assertCompletionTimes(result, 1, 4);
        assertMetrics(result, 2.50, 0.50, 1);
    }

    @Test
    void deveEnfileirarChegadaNoLimiteDoQuantumAntesDoProcessoPreemptado() {
        SchedulingResult result = new RoundRobinScheduler(2).schedule(processes(
                new int[] {0, 4, 1},
                new int[] {2, 1, 1}));

        assertEquals(List.of(1, 1, 2, 1, 1), result.timeline());
        assertCompletionTimes(result, 5, 3);
        assertMetrics(result, 3.00, 0.50, 2);
    }

    @ParameterizedTest(name = "quantum = {0}")
    @ValueSource(ints = {1, 2, 3})
    void naoDeveContarTrocaParaProcessoUnico(int quantum) {
        SchedulingResult result = new RoundRobinScheduler(quantum).schedule(processes(
                new int[] {0, 2, 1}));

        assertEquals(List.of(1, 1), result.timeline());
        assertCompletionTimes(result, 2);
        assertMetrics(result, 2.00, 0.00, 0);
    }

    @Test
    void deveIgnorarPrioridadeEPreservarAOrdemFifo() {
        SchedulingResult result = new RoundRobinScheduler(2).schedule(processes(
                new int[] {0, 1, 1},
                new int[] {0, 1, 1}));

        assertEquals(List.of(1, 2), result.timeline());
        assertMetrics(result, 1.50, 0.50, 1);
    }

    @Test
    void deveRegistrarCpuOciosa() {
        SchedulingResult result = new RoundRobinScheduler(2).schedule(processes(
                new int[] {2, 1, 1},
                new int[] {4, 1, 1}));

        assertEquals(List.of(0, 0, 1, 0, 2), result.timeline());
        assertMetrics(result, 1.00, 0.00, 0);
    }

    @Test
    void deveRejeitarQuantumNaoPositivo() {
        assertThrows(IllegalArgumentException.class, () -> new RoundRobinScheduler(0));
    }

    @Test
    void deveRespeitarAsInvariantesDaSimulacao() {
        List<ProcessControlBlock> input = referenceProcesses();

        assertValidSimulation(input, new RoundRobinScheduler(2).schedule(input));
    }
}
