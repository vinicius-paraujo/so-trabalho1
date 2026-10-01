package br.ufc.so.escalonamento.gui;

import br.ufc.so.escalonamento.simulation.AlgorithmReport;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/** Compara métricas em tabela e gráfico com escala comum. */
@SuppressWarnings("serial")
final class ComparisonPanel extends JPanel {
    private final ComparisonTableModel tableModel = new ComparisonTableModel();
    private final MetricsBarChart barChart = new MetricsBarChart();

    ComparisonPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Comparação entre os algoritmos");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        JTable table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Double.class, new BestValueRenderer());
        table.setDefaultRenderer(Integer.class, new BestValueRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(260);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(600, 7 * 24 + 32));

        JLabel note = new JLabel("Em negrito, o menor valor de cada coluna.");

        JPanel top = new JPanel(new BorderLayout(4, 4));
        top.add(title, BorderLayout.NORTH);
        top.add(tableScroll, BorderLayout.CENTER);
        top.add(note, BorderLayout.SOUTH);

        barChart.setBorder(BorderFactory.createTitledBorder("Turnaround médio e espera média"));
        add(top, BorderLayout.NORTH);
        add(barChart, BorderLayout.CENTER);
    }

    void setReports(List<AlgorithmReport> reports) {
        tableModel.setReports(reports);
        barChart.setReports(reports);
    }

    private final class BestValueRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            Object text = value instanceof Double number ? AlgorithmPanel.format(number) : value;
            Component component = super.getTableCellRendererComponent(table, text, selected, focused, row, column);
            setHorizontalAlignment(SwingConstants.RIGHT);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 12));
            int modelRow = table.convertRowIndexToModel(row);
            int modelColumn = table.convertColumnIndexToModel(column);
            component.setFont(tableModel.isBest(modelRow, modelColumn)
                    ? component.getFont().deriveFont(Font.BOLD)
                    : component.getFont().deriveFont(Font.PLAIN));
            return component;
        }
    }

    private static final class MetricsBarChart extends JPanel {
        private static final Color TURNAROUND_COLOR = new Color(0x1F77B4);
        private static final Color WAITING_COLOR = new Color(0xE37B0E);
        private static final int LABEL_WIDTH = 240;
        private static final int BAR_HEIGHT = 12;
        private static final int GROUP_HEIGHT = 34;

        private transient List<AlgorithmReport> reports = List.of();

        MetricsBarChart() {
            setBackground(Color.WHITE);
        }

        void setReports(List<AlgorithmReport> reports) {
            this.reports = List.copyOf(reports);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (reports.isEmpty()) {
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                paintBars(g);
            } finally {
                g.dispose();
            }
        }

        private void paintBars(Graphics2D g) {
            FontMetrics metrics = g.getFontMetrics();
            int left = getInsets().left + 12;
            int top = getInsets().top + 12;
            int barsLeft = left + LABEL_WIDTH;
            int availableWidth = Math.max(50, getWidth() - getInsets().right - barsLeft - 60);

            double maximum = 0;
            for (AlgorithmReport report : reports) {
                maximum = Math.max(maximum, report.metrics().averageTurnaround());
            }
            double scale = maximum == 0 ? 0 : availableWidth / maximum;

            for (int index = 0; index < reports.size(); index++) {
                AlgorithmReport report = reports.get(index);
                int y = top + index * GROUP_HEIGHT;
                g.setColor(Color.DARK_GRAY);
                g.drawString(AlgorithmDescriptions.title(report.algorithmName()), left, y + BAR_HEIGHT + 4);
                paintBar(g, metrics, barsLeft, y, report.metrics().averageTurnaround(), scale, TURNAROUND_COLOR);
                paintBar(g, metrics, barsLeft, y + BAR_HEIGHT + 2,
                        report.metrics().averageWaitingTime(), scale, WAITING_COLOR);
            }

            int legendY = top + reports.size() * GROUP_HEIGHT + 8;
            g.setColor(TURNAROUND_COLOR);
            g.fillRect(barsLeft, legendY, 14, 10);
            g.setColor(Color.DARK_GRAY);
            g.drawString("tt (turnaround médio)", barsLeft + 20, legendY + 10);
            int second = barsLeft + 40 + metrics.stringWidth("tt (turnaround médio)");
            g.setColor(WAITING_COLOR);
            g.fillRect(second, legendY, 14, 10);
            g.setColor(Color.DARK_GRAY);
            g.drawString("tw (espera média)", second + 20, legendY + 10);
        }

        private void paintBar(
                Graphics2D g, FontMetrics metrics, int x, int y, double value, double scale, Color color) {
            int width = (int) Math.round(value * scale);
            g.setColor(color);
            g.fillRect(x, y, width, BAR_HEIGHT);
            g.setColor(Color.DARK_GRAY);
            g.drawString(AlgorithmPanel.format(value), x + width + 6, y + metrics.getAscent() - 1);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(600, 7 * GROUP_HEIGHT + 60);
        }
    }
}
