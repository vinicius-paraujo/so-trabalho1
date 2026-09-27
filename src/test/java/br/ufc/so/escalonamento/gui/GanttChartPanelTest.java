package br.ufc.so.escalonamento.gui;

import static br.ufc.so.escalonamento.scheduler.SchedulingAssertions.referenceProcesses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufc.so.escalonamento.input.SchedulerConfiguration;
import br.ufc.so.escalonamento.simulation.AlgorithmReport;
import br.ufc.so.escalonamento.simulation.SimulationRunner;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Desenha o diagrama em memória e confere as cores das células de cada processo. */
class GanttChartPanelTest {
    private static final int PREEMPTIVE_PRIORITY = 4;

    private final List<AlgorithmReport> reports = new SimulationRunner(() -> new Random(0))
            .runAll(referenceProcesses(), new SchedulerConfiguration(2, 1));

    @Test
    void deveDesenharExecucaoEsperaECelulasVazias() {
        GanttChartPanel chart = chartFor(reports.get(PREEMPTIVE_PRIORITY));
        // Prioridade preemptiva: [0,2) P2; [2,3) P1; [3,6) P4; [6,10) P1; [10,14) P3.

        BufferedImage image = render(chart);

        assertEquals(GanttChartPanel.colorFor(1).getRGB(), cellColor(image, chart, 0, 2));
        assertEquals(GanttChartPanel.colorFor(4).getRGB(), cellColor(image, chart, 3, 4));
        assertEquals(new Color(0xE3E6EA).getRGB(), cellColor(image, chart, 0, 3), "P1 aguarda em t = 3");
        assertEquals(Color.WHITE.getRGB(), cellColor(image, chart, 3, 1), "P4 ainda não foi criado em t = 1");
        assertEquals(Color.WHITE.getRGB(), cellColor(image, chart, 1, 5), "P2 já terminou em t = 5");
    }

    @Test
    void deveOcultarOsSegundosAindaNaoRevelados() {
        GanttChartPanel chart = chartFor(reports.get(PREEMPTIVE_PRIORITY));
        chart.setVisibleSeconds(3);

        BufferedImage image = render(chart);

        assertEquals(GanttChartPanel.colorFor(1).getRGB(), cellColor(image, chart, 0, 2));
        assertEquals(Color.WHITE.getRGB(), cellColor(image, chart, 3, 4), "t = 4 ainda não foi revelado");
    }

    @Test
    void deveLimitarOsSegundosVisiveisAoTotal() {
        GanttChartPanel chart = chartFor(reports.get(0));

        chart.setVisibleSeconds(99);
        assertEquals(14, chart.visibleSeconds());
        chart.setVisibleSeconds(-3);
        assertEquals(0, chart.visibleSeconds());
    }

    @Test
    void deveDimensionarODiagramaPelaLinhaDoTempoEPelosProcessos() {
        GanttChartPanel chart = chartFor(reports.get(0));

        Dimension size = chart.getPreferredSize();

        assertEquals(chart.xOf(14) + GanttChartPanel.PADDING, size.width);
        assertTrue(size.height > 5 * GanttChartPanel.ROW_HEIGHT);
    }

    @Test
    void deveDesenharSemResultado() {
        GanttChartPanel chart = new GanttChartPanel();
        chart.setSize(chart.getPreferredSize());

        render(chart);

        assertEquals(0, chart.totalSeconds());
    }

    @Test
    void deveEscolherTextoLegivelSobreCadaCor() {
        assertEquals(Color.WHITE, GanttChartPanel.textColorOn(GanttChartPanel.colorFor(1)));
        assertEquals(Color.BLACK, GanttChartPanel.textColorOn(Color.YELLOW));
        assertNotEquals(GanttChartPanel.colorFor(1), GanttChartPanel.colorFor(2));
    }

    @Test
    void painelsDasAbasDevemAceitarResultadosEDesenhar() {
        AlgorithmPanel algorithmPanel = new AlgorithmPanel();
        algorithmPanel.setReport(reports.get(6));
        algorithmPanel.setSize(1000, 700);
        algorithmPanel.doLayout();

        ComparisonPanel comparisonPanel = new ComparisonPanel();
        comparisonPanel.setReports(reports);
        comparisonPanel.setSize(1000, 700);
        comparisonPanel.doLayout();

        render(algorithmPanel);
        render(comparisonPanel);
    }

    private GanttChartPanel chartFor(AlgorithmReport report) {
        GanttChartPanel chart = new GanttChartPanel();
        chart.setReport(report);
        chart.setSize(chart.getPreferredSize());
        return chart;
    }

    private BufferedImage render(javax.swing.JComponent component) {
        BufferedImage image = new BufferedImage(
                Math.max(1, component.getWidth()), Math.max(1, component.getHeight()), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        component.printAll(g);
        g.dispose();
        return image;
    }

    private int cellColor(BufferedImage image, GanttChartPanel chart, int row, int instant) {
        int x = chart.xOf(instant) + GanttChartPanel.CELL_WIDTH / 2;
        int y = GanttChartPanel.PADDING + GanttChartPanel.HEADER_HEIGHT
                + row * GanttChartPanel.ROW_HEIGHT + GanttChartPanel.ROW_HEIGHT / 2;
        return image.getRGB(x, y);
    }
}
