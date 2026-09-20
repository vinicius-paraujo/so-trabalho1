package br.ufc.so.escalonamento.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProcessControlBlockTest {
    @Test
    void deveInicializarOControleDoProcessoComEstadoExplicito() {
        ProcessControlBlock process = new ProcessControlBlock(1, 3, 5, 2);

        assertEquals(1, process.id());
        assertEquals(3, process.arrivalTime());
        assertEquals(5, process.duration());
        assertEquals(5, process.remainingTime());
        assertEquals(2, process.staticPriority());
        assertEquals(2, process.dynamicPriority());
        assertEquals(ProcessState.NEW, process.state());
    }

    @Test
    void deveRepresentarAsTransicoesDoProcesso() {
        ProcessControlBlock process = new ProcessControlBlock(1, 0, 1, 2);

        process.markReady();
        assertEquals(ProcessState.READY, process.state());

        process.markRunning();
        process.executeOneSecond();
        process.terminateAt(1);

        assertEquals(0, process.remainingTime());
        assertEquals(1, process.completionTime());
        assertEquals(ProcessState.TERMINATED, process.state());
    }

    @Test
    void deveRepresentarPreempcaoComoRetornoAoEstadoPronto() {
        ProcessControlBlock process = new ProcessControlBlock(1, 0, 2, 2);

        process.markReady();
        process.markRunning();
        process.executeOneSecond();
        process.markReady();

        assertEquals(1, process.remainingTime());
        assertEquals(ProcessState.READY, process.state());
    }

    @Test
    void deveAplicarERestaurarPrioridadeDinamica() {
        ProcessControlBlock process = new ProcessControlBlock(1, 0, 1, 2);

        process.markReady();
        process.applyAging(3);
        assertEquals(5, process.dynamicPriority());

        process.markRunning();
        process.resetDynamicPriority();
        assertEquals(2, process.dynamicPriority());
    }

    @Test
    void deveRejeitarExecucaoForaDoEstadoRunning() {
        ProcessControlBlock process = new ProcessControlBlock(1, 0, 1, 2);

        assertThrows(IllegalStateException.class, process::executeOneSecond);
    }

    @Test
    void deveRejeitarAgingForaDoEstadoReady() {
        ProcessControlBlock process = new ProcessControlBlock(1, 0, 1, 2);

        assertThrows(IllegalStateException.class, () -> process.applyAging(1));
    }
}
