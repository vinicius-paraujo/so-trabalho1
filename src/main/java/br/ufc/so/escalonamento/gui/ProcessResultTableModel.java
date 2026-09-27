package br.ufc.so.escalonamento.gui;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.metrics.MetricsCalculator;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** Resultado individual de cada processo em um algoritmo. */
@SuppressWarnings("serial") // Componentes Swing deste projeto não são serializados.
public final class ProcessResultTableModel extends AbstractTableModel {
    private static final String[] COLUMN_NAMES = {
        "Processo", "Criação", "Duração", "Prioridade", "Conclusão", "Turnaround", "Espera"
    };

    private final MetricsCalculator metricsCalculator = new MetricsCalculator();
    private List<ProcessControlBlock> processes = List.of();

    public void setProcesses(List<ProcessControlBlock> processes) {
        this.processes = List.copyOf(processes);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return processes.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return column == 0 ? String.class : Integer.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int column) {
        ProcessControlBlock process = processes.get(rowIndex);
        return switch (column) {
            case 0 -> "P" + process.id();
            case 1 -> process.arrivalTime();
            case 2 -> process.duration();
            case 3 -> process.staticPriority();
            case 4 -> process.completionTime();
            case 5 -> metricsCalculator.turnaround(process);
            case 6 -> metricsCalculator.waitingTime(process);
            default -> throw new IllegalArgumentException("Coluna inexistente: " + column);
        };
    }
}
