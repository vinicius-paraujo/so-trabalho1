package br.ufc.so.escalonamento.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.scheduler.SchedulingResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class MetricsCalculatorTest {
    private final MetricsCalculator calculator = new MetricsCalculator();

    @Test
    void deveCalcularAsMetricasDoFcfsNoCasoDeReferencia() {
        List<ProcessControlBlock> processes = List.of(
                terminated(1, 0, 5, 7),
                terminated(2, 0, 2, 2),
                terminated(3, 1, 4, 11),
                terminated(4, 3, 3, 14));
        List<Integer> timeline = List.of(2, 2, 1, 1, 1, 1, 1, 3, 3, 3, 3, 4, 4, 4);

        SchedulingMetrics metrics = calculator.calculate(new SchedulingResult(timeline, processes));

        assertEquals(7.50, metrics.averageTurnaround(), 1e-9);
        assertEquals(4.00, metrics.averageWaitingTime(), 1e-9);
        assertEquals(3, metrics.contextSwitches());
    }

    @Test
    void naoDeveArredondarAsMedias() {
        List<ProcessControlBlock> processes = List.of(
                terminated(1, 5, 1, 6),
                terminated(2, 0, 2, 2),
                terminated(3, 1, 1, 3));

        assertEquals(5.0 / 3.0, calculator.averageTurnaround(processes), 1e-12);
        assertEquals(1.0 / 3.0, calculator.averageWaitingTime(processes), 1e-12);
    }

    @Test
    void naoDeveContarCargaInicialNemContinuidadeComoTroca() {
        assertEquals(0, calculator.contextSwitches(List.of(1, 1, 1)));
    }

    @Test
    void naoDeveContarTransicoesEnvolvendoCpuOciosa() {
        int idle = SchedulingResult.IDLE;

        assertEquals(0, calculator.contextSwitches(List.of(idle, idle, 1, idle, 2)));
    }

    @Test
    void deveContarCadaSubstituicaoDiretaDeProcesso() {
        assertEquals(2, calculator.contextSwitches(List.of(1, 1, 2, 1, 1, 1)));
    }

    @Test
    void deveContarRetornoAoMesmoProcessoAposOutro() {
        assertEquals(7, calculator.contextSwitches(
                List.of(1, 1, 2, 2, 3, 3, 1, 1, 4, 4, 3, 3, 1, 4)));
    }

    @Test
    void deveRejeitarProcessoNaoTerminado() {
        ProcessControlBlock running = new ProcessControlBlock(1, 0, 2, 1);

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.averageTurnaround(List.of(running)));
    }

    @Test
    void deveRejeitarListaVazia() {
        assertThrows(IllegalArgumentException.class, () -> calculator.averageWaitingTime(List.of()));
    }

    private ProcessControlBlock terminated(int id, int arrival, int duration, int completion) {
        ProcessControlBlock process = new ProcessControlBlock(id, arrival, duration, 1);
        process.markReady();
        process.markRunning();
        for (int second = 0; second < duration; second++) {
            process.executeOneSecond();
        }
        process.terminateAt(completion);
        return process;
    }
}
