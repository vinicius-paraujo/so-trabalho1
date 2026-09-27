package br.ufc.so.escalonamento.gui;

import br.ufc.so.escalonamento.simulation.AlgorithmReport;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** Métricas dos sete algoritmos lado a lado, com identificação do melhor valor por coluna. */
@SuppressWarnings("serial") // Componentes Swing deste projeto não são serializados.
public final class ComparisonTableModel extends AbstractTableModel {
    static final int NAME_COLUMN = 0;
    static final int TURNAROUND_COLUMN = 1;
    static final int WAITING_COLUMN = 2;
    static final int SWITCHES_COLUMN = 3;

    private static final String[] COLUMN_NAMES = {"Algoritmo", "TT médio", "TW médio", "Trocas de contexto"};
    private static final double TOLERANCE = 1e-9;

    private List<AlgorithmReport> reports = List.of();

    public void setReports(List<AlgorithmReport> reports) {
        this.reports = List.copyOf(reports);
        fireTableDataChanged();
    }

    /** Menor valor da coluna; empates marcam todos os algoritmos empatados. */
    public boolean isBest(int rowIndex, int column) {
        if (column == NAME_COLUMN || reports.isEmpty()) {
            return false;
        }

        double best = Double.MAX_VALUE;
        for (int row = 0; row < reports.size(); row++) {
            best = Math.min(best, numericValue(row, column));
        }
        return numericValue(rowIndex, column) <= best + TOLERANCE;
    }

    @Override
    public int getRowCount() {
        return reports.size();
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
        return switch (column) {
            case NAME_COLUMN -> String.class;
            case SWITCHES_COLUMN -> Integer.class;
            default -> Double.class;
        };
    }

    @Override
    public Object getValueAt(int rowIndex, int column) {
        AlgorithmReport report = reports.get(rowIndex);
        return switch (column) {
            case NAME_COLUMN -> AlgorithmDescriptions.title(report.algorithmName());
            case TURNAROUND_COLUMN -> report.metrics().averageTurnaround();
            case WAITING_COLUMN -> report.metrics().averageWaitingTime();
            case SWITCHES_COLUMN -> report.metrics().contextSwitches();
            default -> throw new IllegalArgumentException("Coluna inexistente: " + column);
        };
    }

    private double numericValue(int rowIndex, int column) {
        return ((Number) getValueAt(rowIndex, column)).doubleValue();
    }
}
