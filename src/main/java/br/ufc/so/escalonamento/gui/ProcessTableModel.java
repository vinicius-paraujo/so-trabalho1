package br.ufc.so.escalonamento.gui;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.input.ProcessInputParser;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * Processos editados na interface. A conversão passa pelo mesmo {@link ProcessInputParser} da
 * aplicação de terminal, de modo que as regras de validação do ADR 0005 não são duplicadas.
 */
@SuppressWarnings("serial") // Componentes Swing deste projeto não são serializados.
public final class ProcessTableModel extends AbstractTableModel {
    static final int ID_COLUMN = 0;
    static final int ARRIVAL_COLUMN = 1;
    static final int DURATION_COLUMN = 2;
    static final int PRIORITY_COLUMN = 3;

    private static final String[] COLUMN_NAMES = {"Processo", "Criação", "Duração", "Prioridade"};
    private static final int[][] STATEMENT_EXAMPLE = {
        {0, 5, 2},
        {0, 2, 3},
        {1, 4, 1},
        {3, 3, 4},
    };

    private final List<int[]> rows = new ArrayList<>();

    public void loadStatementExample() {
        rows.clear();
        for (int[] row : STATEMENT_EXAMPLE) {
            rows.add(row.clone());
        }
        fireTableDataChanged();
    }

    public void setProcesses(List<ProcessControlBlock> processes) {
        rows.clear();
        for (ProcessControlBlock process : processes) {
            rows.add(new int[] {process.arrivalTime(), process.duration(), process.staticPriority()});
        }
        fireTableDataChanged();
    }

    /** Novo processo chegando logo após o último, com valores mínimos válidos. */
    public void addProcess() {
        int arrival = rows.isEmpty() ? 0 : rows.getLast()[0];
        rows.add(new int[] {arrival, 1, 1});
        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
    }

    public void removeProcess(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return;
        }
        rows.remove(rowIndex);
        // Os identificadores seguem a posição da linha e mudam para as linhas seguintes.
        fireTableDataChanged();
    }

    public void clear() {
        rows.clear();
        fireTableDataChanged();
    }

    /** @throws br.ufc.so.escalonamento.input.InputValidationException se algum valor for inválido. */
    public List<ProcessControlBlock> toProcesses() {
        StringBuilder input = new StringBuilder();
        for (int[] row : rows) {
            input.append(row[0]).append(' ').append(row[1]).append(' ').append(row[2]).append('\n');
        }

        try {
            return new ProcessInputParser().parse(new StringReader(input.toString()));
        } catch (IOException exception) {
            throw new UncheckedIOException("Falha inesperada ao ler texto em memória.", exception);
        }
    }

    @Override
    public int getRowCount() {
        return rows.size();
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
        return column == ID_COLUMN ? String.class : Integer.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int column) {
        return column != ID_COLUMN;
    }

    @Override
    public Object getValueAt(int rowIndex, int column) {
        if (column == ID_COLUMN) {
            return "P" + (rowIndex + 1);
        }
        return rows.get(rowIndex)[column - 1];
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int column) {
        if (column == ID_COLUMN || !(value instanceof Integer number)) {
            return;
        }
        rows.get(rowIndex)[column - 1] = number;
        fireTableCellUpdated(rowIndex, column);
    }
}
