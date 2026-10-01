package br.ufc.so.escalonamento.gui;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.gui.TimelinePresentation.DisplayState;
import br.ufc.so.escalonamento.gui.TimelinePresentation.Segment;
import br.ufc.so.escalonamento.simulation.AlgorithmReport;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JPanel;

/** Renderiza processos e CPU até o instante visível da animação. */
@SuppressWarnings("serial")
public final class GanttChartPanel extends JPanel {
    static final int PADDING = 12;
    static final int LABEL_WIDTH = 56;
    static final int HEADER_HEIGHT = 24;
    static final int CELL_WIDTH = 34;
    static final int ROW_HEIGHT = 30;
    static final int LEGEND_HEIGHT = 34;

    private static final Color[] PALETTE = {
        new Color(0x1F77B4), new Color(0xD62728), new Color(0x2CA02C), new Color(0x9467BD),
        new Color(0xE37B0E), new Color(0x8C564B), new Color(0xD1449F), new Color(0x138D9B),
        new Color(0x5F6B7A), new Color(0x8A8A12),
    };
    private static final Color READY_COLOR = new Color(0xE3E6EA);
    private static final Color GRID_COLOR = new Color(0xEDEFF2);
    private static final Color AXIS_COLOR = new Color(0x6B7280);
    private static final Color CURSOR_COLOR = new Color(0xDC2626);

    private transient AlgorithmReport report;
    private int visibleSeconds;

    public GanttChartPanel() {
        setBackground(Color.WHITE);
        setOpaque(true);
    }

    public void setReport(AlgorithmReport report) {
        this.report = report;
        this.visibleSeconds = totalSeconds();
        revalidate();
        repaint();
    }

    public int totalSeconds() {
        return report == null ? 0 : report.result().timeline().size();
    }

    public int visibleSeconds() {
        return visibleSeconds;
    }

    public void setVisibleSeconds(int seconds) {
        visibleSeconds = Math.max(0, Math.min(seconds, totalSeconds()));
        repaint();
    }

    public int xOf(int instant) {
        return PADDING + LABEL_WIDTH + instant * CELL_WIDTH;
    }

    static Color colorFor(int processId) {
        return PALETTE[(processId - 1) % PALETTE.length];
    }

    @Override
    public Dimension getPreferredSize() {
        if (report == null) {
            return new Dimension(480, 160);
        }
        int rows = report.result().processes().size() + 1;
        return new Dimension(
                xOf(totalSeconds()) + PADDING,
                PADDING * 2 + HEADER_HEIGHT + rows * ROW_HEIGHT + LEGEND_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (report == null) {
                g.setColor(AXIS_COLOR);
                g.drawString("Execute uma simulação para ver o diagrama.", PADDING, PADDING + HEADER_HEIGHT);
                return;
            }
            paintChart(g);
        } finally {
            g.dispose();
        }
    }

    private void paintChart(Graphics2D g) {
        List<ProcessControlBlock> processes = report.result().processes();
        List<Integer> timeline = report.result().timeline();
        int total = totalSeconds();
        int top = PADDING + HEADER_HEIGHT;
        int cpuRowTop = top + processes.size() * ROW_HEIGHT;
        int bottom = cpuRowTop + ROW_HEIGHT;

        paintTimeAxis(g, total, top, bottom);

        for (int row = 0; row < processes.size(); row++) {
            paintProcessRow(g, processes.get(row), timeline, top + row * ROW_HEIGHT);
        }

        g.setColor(AXIS_COLOR);
        g.drawLine(PADDING, cpuRowTop, xOf(total), cpuRowTop);
        paintCpuRow(g, timeline, cpuRowTop);

        if (visibleSeconds < total) {
            int x = xOf(visibleSeconds);
            g.setColor(CURSOR_COLOR);
            g.setStroke(new BasicStroke(2f));
            g.drawLine(x, top - 6, x, bottom);
            g.setStroke(new BasicStroke(1f));
        }

        paintLegend(g, bottom + 12);
    }

    private void paintTimeAxis(Graphics2D g, int total, int top, int bottom) {
        Font baseFont = g.getFont();
        g.setFont(baseFont.deriveFont(11f));
        FontMetrics metrics = g.getFontMetrics();

        g.setColor(AXIS_COLOR);
        g.drawString("tempo", PADDING, top - 8);
        for (int instant = 0; instant <= total; instant++) {
            int x = xOf(instant);
            g.setColor(GRID_COLOR);
            g.drawLine(x, top, x, bottom);
            g.setColor(AXIS_COLOR);
            String label = Integer.toString(instant);
            g.drawString(label, x - metrics.stringWidth(label) / 2, top - 8);
        }
        g.setFont(baseFont);
    }

    private void paintProcessRow(Graphics2D g, ProcessControlBlock process, List<Integer> timeline, int rowTop) {
        Color color = colorFor(process.id());
        paintRowLabel(g, "P" + process.id(), rowTop);

        for (int instant = 0; instant < visibleSeconds; instant++) {
            DisplayState state = TimelinePresentation.stateAt(process, timeline, instant);
            if (state == DisplayState.RUNNING) {
                g.setColor(color);
                g.fillRect(xOf(instant), rowTop + 6, CELL_WIDTH, ROW_HEIGHT - 12);
            } else if (state == DisplayState.READY) {
                g.setColor(READY_COLOR);
                g.fillRect(xOf(instant), rowTop + 11, CELL_WIDTH, ROW_HEIGHT - 22);
            }
        }

        if (process.arrivalTime() <= visibleSeconds) {
            paintArrivalMarker(g, xOf(process.arrivalTime()), rowTop + 1, color);
        }
    }

    private void paintCpuRow(Graphics2D g, List<Integer> timeline, int rowTop) {
        paintRowLabel(g, "CPU", rowTop);
        FontMetrics metrics = g.getFontMetrics();

        for (Segment segment : TimelinePresentation.segments(timeline)) {
            if (segment.start() >= visibleSeconds) {
                break;
            }
            int start = xOf(segment.start());
            int width = (Math.min(segment.end(), visibleSeconds) - segment.start()) * CELL_WIDTH;
            int y = rowTop + 4;
            int height = ROW_HEIGHT - 8;

            String label;
            if (segment.idle()) {
                g.setColor(AXIS_COLOR);
                g.drawRect(start, y, width, height);
                label = "ocioso";
            } else {
                Color color = colorFor(segment.processId());
                g.setColor(color);
                g.fillRect(start, y, width, height);
                g.setColor(Color.WHITE);
                g.drawLine(start, y, start, y + height);
                g.setColor(textColorOn(color));
                label = "P" + segment.processId();
            }

            if (metrics.stringWidth(label) + 4 <= width) {
                if (segment.idle()) {
                    g.setColor(AXIS_COLOR);
                }
                g.drawString(
                        label,
                        start + (width - metrics.stringWidth(label)) / 2,
                        y + (height + metrics.getAscent() - metrics.getDescent()) / 2);
            }
        }
    }

    private void paintRowLabel(Graphics2D g, String label, int rowTop) {
        Font baseFont = g.getFont();
        g.setFont(baseFont.deriveFont(Font.BOLD));
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(Color.DARK_GRAY);
        g.drawString(label, PADDING, rowTop + (ROW_HEIGHT + metrics.getAscent() - metrics.getDescent()) / 2);
        g.setFont(baseFont);
    }

    private void paintArrivalMarker(Graphics2D g, int x, int y, Color color) {
        Polygon triangle = new Polygon(new int[] {x - 4, x + 4, x}, new int[] {y, y, y + 5}, 3);
        g.setColor(color);
        g.fillPolygon(triangle);
    }

    private void paintLegend(Graphics2D g, int y) {
        FontMetrics metrics = g.getFontMetrics();
        int x = PADDING;

        g.setColor(PALETTE[0]);
        g.fillRect(x, y, 16, 12);
        x = legendText(g, metrics, "Executando", x + 22, y);

        g.setColor(READY_COLOR);
        g.fillRect(x, y + 3, 16, 6);
        x = legendText(g, metrics, "Pronto (aguardando)", x + 22, y);

        paintArrivalMarker(g, x + 8, y + 3, PALETTE[0]);
        x = legendText(g, metrics, "Criação", x + 22, y);

        g.setColor(AXIS_COLOR);
        g.drawRect(x, y, 16, 12);
        legendText(g, metrics, "CPU ociosa", x + 22, y);
    }

    private int legendText(Graphics2D g, FontMetrics metrics, String text, int x, int y) {
        g.setColor(Color.DARK_GRAY);
        g.drawString(text, x, y + 11);
        return x + metrics.stringWidth(text) + 20;
    }

    /** Escolhe texto claro ou escuro conforme a luminância da cor de fundo. */
    static Color textColorOn(Color background) {
        double luminance = (0.299 * background.getRed()
                + 0.587 * background.getGreen()
                + 0.114 * background.getBlue()) / 255;
        return luminance > 0.6 ? Color.BLACK : Color.WHITE;
    }
}
