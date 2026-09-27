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
import java.util.Random;
import org.junit.jupiter.api.Test;

class PriorityRoundRobinSchedulerTest {
    @Test
    void deveEscalonarOCasoDeReferencia() {
        SchedulingResult result = scheduler(2, 1).schedule(referenceProcesses());

        assertEquals(
                List.of(2, 2, 1, 1, 4, 4, 4, 3, 3, 1, 1, 1, 3, 3),
                result.timeline());
        assertCompletionTimes(result, 12, 2, 14, 7);
        assertMetrics(result, 7.75, 4.25, 5);
    }

    @Test
    void naoDevePreemptarPorPrioridadeDuranteOQuantum() {
        SchedulingResult result = scheduler(2, 1).schedule(processes(
                new int[] {0, 4, 1},
                new int[] {1, 1, 5}));

        assertEquals(List.of(1, 1, 2, 1, 1), result.timeline());
        assertCompletionTimes(result, 5, 3);
        assertMetrics(result, 3.50, 1.00, 2);
    }

    @Test
    void deveAlterarAEscolhaPorAging() {
        SchedulingResult result = scheduler(2, 2).schedule(processes(
                new int[] {0, 6, 3},
                new int[] {0, 1, 1}));

        assertEquals(List.of(1, 1, 2, 1, 1, 1, 1), result.timeline());
        assertCompletionTimes(result, 7, 3);
        assertMetrics(result, 5.00, 1.50, 2);
    }

    @Test
    void deveAcumularAgingERestaurarPrioridadeAoSelecionar() {
        SchedulingResult result = scheduler(1, 2).schedule(processes(
                new int[] {0, 4, 5},
                new int[] {0, 3, 1}));

        assertEquals(List.of(1, 1, 1, 2, 1, 2, 2), result.timeline());
        assertCompletionTimes(result, 5, 7);
        assertMetrics(result, 6.00, 2.50, 3);
    }

    @Test
    void naoDeveAplicarAgingQuandoProcessoTerminaAntesDoQuantum() {
        SchedulingResult result = scheduler(3, 2).schedule(processes(
                new int[] {0, 1, 5},
                new int[] {0, 1, 1},
                new int[] {1, 1, 2}));

        assertEquals(List.of(1, 3, 2), result.timeline());
        assertCompletionTimes(result, 1, 3, 2);
        assertMetrics(result, 5.0 / 3.0, 2.0 / 3.0, 2);
    }

    @Test
    void deveEnvelhecerProcessoQueChegouDuranteOQuantum() {
        SchedulingResult result = scheduler(2, 1).schedule(processes(
                new int[] {0, 4, 3},
                new int[] {1, 1, 2}));

        assertEquals(List.of(1, 1, 2, 1, 1), result.timeline());
        assertCompletionTimes(result, 5, 3);
        assertMetrics(result, 3.50, 1.00, 2);
    }

    @Test
    void naoDeveEnvelhecerProcessoQueChegaNoLimiteDoQuantum() {
        // Se P2 recebesse aging em t = 2, sua prioridade superaria a de P1 e ele seria escolhido.
        SchedulingResult result = scheduler(2, 1).schedule(processes(
                new int[] {0, 3, 2},
                new int[] {2, 5, 2}));

        assertEquals(List.of(1, 1, 1, 2, 2, 2, 2, 2), result.timeline());
        assertCompletionTimes(result, 3, 8);
    }

    @Test
    void naoDeveFavorecerOProcessoQueEsgotouOQuantumNoDesempate() {
        // Em t = 2, P1 e P2 têm prioridade dinâmica 3; P2 vence pelo menor tempo restante.
        SchedulingResult result = scheduler(2, 2).schedule(processes(
                new int[] {0, 6, 3},
                new int[] {0, 1, 1}));

        assertEquals(2, result.timeline().get(2));
    }

    @Test
    void deveEquivalerAoRoundRobinPorPrioridadeQuandoAgingEZero() {
        SchedulingResult result = scheduler(1, 0).schedule(processes(
                new int[] {0, 2, 1},
                new int[] {0, 2, 3}));

        assertEquals(List.of(2, 2, 1, 1), result.timeline());
        assertMetrics(result, 3.00, 1.00, 1);
    }

    @Test
    void deveRegistrarCpuOciosa() {
        SchedulingResult result = scheduler(2, 1).schedule(processes(
                new int[] {2, 1, 1},
                new int[] {4, 1, 1}));

        assertEquals(List.of(0, 0, 1, 0, 2), result.timeline());
        assertMetrics(result, 1.00, 0.00, 0);
    }

    @Test
    void deveAplicarDesempateAleatorioAposOsDemaisCriterios() {
        SchedulingResult result = scheduler(2, 1).schedule(processes(
                new int[] {0, 1, 1},
                new int[] {0, 1, 1}));

        assertEquals(List.of(2, 1), result.timeline());
        assertMetrics(result, 1.50, 0.50, 1);
    }

    @Test
    void deveRejeitarConfiguracaoInvalida() {
        assertThrows(IllegalArgumentException.class, () -> scheduler(0, 1));
        assertThrows(IllegalArgumentException.class, () -> scheduler(2, -1));
    }

    @Test
    void deveRespeitarAsInvariantesDaSimulacao() {
        List<ProcessControlBlock> input = referenceProcesses();

        assertValidSimulation(input, scheduler(2, 1).schedule(input));
    }

    private PriorityRoundRobinScheduler scheduler(int quantum, int agingRate) {
        return new PriorityRoundRobinScheduler(quantum, agingRate, new Random(0));
    }
}
