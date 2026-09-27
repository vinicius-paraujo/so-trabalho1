package br.ufc.so.escalonamento.scheduler;

import static br.ufc.so.escalonamento.scheduler.SchedulingAssertions.assertCompletionTimes;
import static br.ufc.so.escalonamento.scheduler.SchedulingAssertions.assertMetrics;
import static br.ufc.so.escalonamento.scheduler.SchedulingAssertions.assertValidSimulation;
import static br.ufc.so.escalonamento.scheduler.SchedulingAssertions.processes;
import static br.ufc.so.escalonamento.scheduler.SchedulingAssertions.referenceProcesses;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PreemptivePrioritySchedulerTest {
    @Test
    void deveEscalonarOCasoDeReferencia() {
        SchedulingResult result = scheduler().schedule(referenceProcesses());

        assertEquals(
                List.of(2, 2, 1, 4, 4, 4, 1, 1, 1, 1, 3, 3, 3, 3),
                result.timeline());
        assertCompletionTimes(result, 10, 2, 14, 6);
        assertMetrics(result, 7.00, 3.50, 4);
    }

    @Test
    void devePreemptarQuandoChegaProcessoMaisPrioritario() {
        SchedulingResult result = scheduler().schedule(processes(
                new int[] {0, 5, 1},
                new int[] {2, 1, 3}));

        assertEquals(List.of(1, 1, 2, 1, 1, 1), result.timeline());
        assertCompletionTimes(result, 6, 3);
        assertMetrics(result, 3.50, 0.50, 2);
    }

    @Test
    void naoDevePreemptarPorPrioridadeIgual() {
        SchedulingResult result = scheduler().schedule(processes(
                new int[] {0, 4, 3},
                new int[] {1, 1, 3}));

        assertEquals(List.of(1, 1, 1, 1, 2), result.timeline());
        assertCompletionTimes(result, 4, 5);
        assertMetrics(result, 4.00, 1.50, 1);
    }

    @Test
    void deveDesempatarPrioridadePeloMenorTempoRestante() {
        SchedulingResult result = scheduler().schedule(processes(
                new int[] {0, 3, 2},
                new int[] {0, 1, 2}));

        assertEquals(List.of(2, 1, 1, 1), result.timeline());
        assertCompletionTimes(result, 4, 1);
    }

    @Test
    void deveRegistrarCpuOciosa() {
        SchedulingResult result = scheduler().schedule(processes(
                new int[] {2, 1, 1},
                new int[] {4, 1, 1}));

        assertEquals(List.of(0, 0, 1, 0, 2), result.timeline());
        assertMetrics(result, 1.00, 0.00, 0);
    }

    @Test
    void deveAplicarDesempateAleatorioQuandoNenhumProcessoOcupaACpu() {
        SchedulingResult result = scheduler().schedule(processes(
                new int[] {0, 1, 1},
                new int[] {0, 1, 1}));

        assertEquals(List.of(2, 1), result.timeline());
        assertMetrics(result, 1.50, 0.50, 1);
    }

    @Test
    void deveRespeitarAsInvariantesDaSimulacao() {
        List<ProcessControlBlock> input = referenceProcesses();

        assertValidSimulation(input, scheduler().schedule(input));
    }

    private PreemptivePriorityScheduler scheduler() {
        return new PreemptivePriorityScheduler(new Random(0));
    }
}
