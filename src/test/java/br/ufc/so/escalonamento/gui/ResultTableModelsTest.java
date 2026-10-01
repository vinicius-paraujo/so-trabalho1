package br.ufc.so.escalonamento.gui;

import static br.ufc.so.escalonamento.scheduler.SchedulingAssertions.referenceProcesses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufc.so.escalonamento.input.SchedulerConfiguration;
import br.ufc.so.escalonamento.simulation.AlgorithmReport;
import br.ufc.so.escalonamento.simulation.SimulationRunner;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ResultTableModelsTest {
    private final List<AlgorithmReport> reports = new SimulationRunner(() -> new Random(0))
            .runAll(referenceProcesses(), new SchedulerConfiguration(2, 1));

    @Test
    void comparacaoDeveExibirAsMetricasDosSeteAlgoritmos() {
        ComparisonTableModel model = new ComparisonTableModel();
        model.setReports(reports);

        assertEquals(7, model.getRowCount());
        assertEquals("FCFS", model.getValueAt(0, ComparisonTableModel.NAME_COLUMN));
        assertEquals(9.75, (double) model.getValueAt(5, ComparisonTableModel.TURNAROUND_COLUMN), 1e-9);
        assertEquals(7, model.getValueAt(5, ComparisonTableModel.SWITCHES_COLUMN));
    }

    @Test
    void comparacaoDeveMarcarTodosOsEmpatadosNoMelhorValor() {
        ComparisonTableModel model = new ComparisonTableModel();
        model.setReports(reports);

        // SJF e SRTF empatam com tt = 6.75 no caso de referência.
        assertTrue(model.isBest(1, ComparisonTableModel.TURNAROUND_COLUMN));
        assertTrue(model.isBest(2, ComparisonTableModel.TURNAROUND_COLUMN));
        assertFalse(model.isBest(0, ComparisonTableModel.TURNAROUND_COLUMN));
        assertFalse(model.isBest(1, ComparisonTableModel.NAME_COLUMN));
        // FCFS, SJF, SRTF e prioridade sem preempção empatam com 3 trocas.
        assertTrue(model.isBest(0, ComparisonTableModel.SWITCHES_COLUMN));
        assertFalse(model.isBest(5, ComparisonTableModel.SWITCHES_COLUMN));
    }

    @Test
    void resultadoPorProcessoDeveExibirConclusaoTurnaroundEEspera() {
        ProcessResultTableModel model = new ProcessResultTableModel();
        model.setProcesses(reports.get(6).result().processes());

        // Round-Robin prioritário: conclusões 12, 2, 14, 7.
        assertEquals(4, model.getRowCount());
        assertEquals("P3", model.getValueAt(2, 0));
        assertEquals(14, model.getValueAt(2, 4));
        assertEquals(13, model.getValueAt(2, 5));
        assertEquals(9, model.getValueAt(2, 6));
    }
}
