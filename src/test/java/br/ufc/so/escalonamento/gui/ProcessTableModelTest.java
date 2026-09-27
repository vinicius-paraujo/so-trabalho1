package br.ufc.so.escalonamento.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.input.InputValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessTableModelTest {
    private final ProcessTableModel model = new ProcessTableModel();

    @Test
    void deveCarregarOExemploDoEnunciado() {
        model.loadStatementExample();

        List<ProcessControlBlock> processes = model.toProcesses();

        assertEquals(4, processes.size());
        assertEquals("P4", model.getValueAt(3, ProcessTableModel.ID_COLUMN));
        assertEquals(3, processes.get(3).arrivalTime());
        assertEquals(3, processes.get(3).duration());
        assertEquals(4, processes.get(3).staticPriority());
    }

    @Test
    void devePermitirEditarSomenteOsValoresDoProcesso() {
        model.loadStatementExample();

        model.setValueAt(9, 0, ProcessTableModel.DURATION_COLUMN);

        assertFalse(model.isCellEditable(0, ProcessTableModel.ID_COLUMN));
        assertTrue(model.isCellEditable(0, ProcessTableModel.PRIORITY_COLUMN));
        assertEquals(9, model.toProcesses().getFirst().duration());
    }

    @Test
    void deveAdicionarProcessoValidoAposOUltimo() {
        model.loadStatementExample();

        model.addProcess();

        ProcessControlBlock added = model.toProcesses().getLast();
        assertEquals(5, added.id());
        assertEquals(3, added.arrivalTime());
        assertEquals(1, added.duration());
    }

    @Test
    void deveRenumerarOsProcessosAposRemocao() {
        model.loadStatementExample();

        model.removeProcess(0);

        assertEquals(3, model.getRowCount());
        assertEquals("P1", model.getValueAt(0, ProcessTableModel.ID_COLUMN));
        assertEquals(2, model.toProcesses().getFirst().duration());
    }

    @Test
    void deveIgnorarRemocaoSemSelecao() {
        model.loadStatementExample();

        model.removeProcess(-1);

        assertEquals(4, model.getRowCount());
    }

    @Test
    void deveValidarComAsRegrasDaEntradaPadrao() {
        model.loadStatementExample();
        model.setValueAt(0, 1, ProcessTableModel.DURATION_COLUMN);

        InputValidationException exception = assertThrows(InputValidationException.class, model::toProcesses);

        assertTrue(exception.getMessage().startsWith("Linha 2"), exception.getMessage());
    }

    @Test
    void deveRejeitarTabelaVazia() {
        model.clear();

        assertThrows(InputValidationException.class, model::toProcesses);
    }

    @Test
    void deveCarregarProcessosImportados() {
        model.setProcesses(List.of(new ProcessControlBlock(1, 7, 2, 5)));

        assertEquals(1, model.getRowCount());
        assertEquals(7, model.getValueAt(0, ProcessTableModel.ARRIVAL_COLUMN));
        assertEquals(5, model.getValueAt(0, ProcessTableModel.PRIORITY_COLUMN));
    }
}
