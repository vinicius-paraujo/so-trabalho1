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

class SrtfSchedulerTest {
    @Test
    void deveEscalonarOCasoDeReferencia() {
        SchedulingResult result = scheduler().schedule(referenceProcesses());

        assertEquals(
                List.of(2, 2, 3, 3, 3, 3, 4, 4, 4, 1, 1, 1, 1, 1),
                result.timeline());
        assertCompletionTimes(result, 14, 2, 6, 9);
        assertMetrics(result, 6.75, 3.25, 3);
    }

    @Test
    void devePreemptarQuandoChegaProcessoComMenorTempoRestante() {
        SchedulingResult result = scheduler().schedule(processes(
                new int[] {0, 5, 1},
                new int[] {2, 1, 3}));

        assertEquals(List.of(1, 1, 2, 1, 1, 1), result.timeline());
        assertCompletionTimes(result, 6, 3);
        assertMetrics(result, 3.50, 0.50, 2);
    }

    @Test
    void deveManterProcessoNaCpuEmEmpateDeTempoRestante() {
        SchedulingResult result = scheduler().schedule(processes(
                new int[] {0, 3, 1},
                new int[] {1, 2, 1}));

        assertEquals(List.of(1, 1, 1, 2, 2), result.timeline());
        assertMetrics(result, 3.50, 1.00, 1);
    }

    @Test
    void deveRetomarProcessoPreemptadoPeloTempoRestante() {
        SchedulingResult result = scheduler().schedule(processes(
                new int[] {0, 4, 1},
                new int[] {1, 1, 1},
                new int[] {2, 5, 1}));

        assertEquals(List.of(1, 2, 1, 1, 1, 3, 3, 3, 3, 3), result.timeline());
        assertCompletionTimes(result, 5, 2, 10);
        assertMetrics(result, 14.0 / 3.0, 4.0 / 3.0, 3);
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

    private SrtfScheduler scheduler() {
        return new SrtfScheduler(new Random(0));
    }
}
