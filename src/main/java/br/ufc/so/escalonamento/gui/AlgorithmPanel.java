package br.ufc.so.escalonamento.gui;

import br.ufc.so.escalonamento.simulation.AlgorithmReport;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.Timer;

/** Exibe métricas e revela progressivamente uma linha do tempo já calculada. */
@SuppressWarnings("serial")
final class AlgorithmPanel extends JPanel {
    private static final int MIN_DELAY_MS = 80;
    private static final int MAX_DELAY_MS = 1500;
    private static final int DEFAULT_DELAY_MS = 500;

    private final GanttChartPanel chart = new GanttChartPanel();
    private final JScrollPane chartScroll = new JScrollPane(chart);
    private final ProcessResultTableModel resultModel = new ProcessResultTableModel();

    private final JLabel titleLabel = new JLabel();
    private final JLabel descriptionLabel = new JLabel();
    private final JLabel turnaroundLabel = metricLabel();
    private final JLabel waitingLabel = metricLabel();
    private final JLabel switchesLabel = metricLabel();
    private final JLabel durationLabel = metricLabel();
    private final JLabel timeLabel = new JLabel();

    private final JButton playButton = new JButton("Reproduzir");
    private final JButton stepButton = new JButton("Passo");
    private final JButton restartButton = new JButton("Reiniciar");
    private final JButton showAllButton = new JButton("Mostrar tudo");
    private final JSlider speedSlider = new JSlider(MIN_DELAY_MS, MAX_DELAY_MS, DEFAULT_DELAY_MS);
    private final Timer timer = new Timer(DEFAULT_DELAY_MS, event -> advance());

    AlgorithmPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        JPanel header = new JPanel(new BorderLayout(4, 4));
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(descriptionLabel, BorderLayout.CENTER);
        header.add(metricsPanel(), BorderLayout.SOUTH);

        JPanel chartPanel = new JPanel(new BorderLayout(4, 4));
        chartPanel.add(controlsPanel(), BorderLayout.NORTH);
        chartScroll.getHorizontalScrollBar().setUnitIncrement(GanttChartPanel.CELL_WIDTH);
        chartPanel.add(chartScroll, BorderLayout.CENTER);

        JTable resultTable = new JTable(resultModel);
        resultTable.setFillsViewportHeight(true);
        JScrollPane resultScroll = new JScrollPane(resultTable);
        resultScroll.setBorder(BorderFactory.createTitledBorder("Resultado por processo"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartPanel, resultScroll);
        split.setResizeWeight(0.65);
        split.setBorder(null);

        add(header, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        playButton.addActionListener(event -> togglePlay());
        stepButton.addActionListener(event -> step());
        restartButton.addActionListener(event -> restart());
        showAllButton.addActionListener(event -> showAll());
        speedSlider.addChangeListener(event -> timer.setDelay(speedSlider.getValue()));
        updateControls();
    }

    void setReport(AlgorithmReport report) {
        timer.stop();
        chart.setReport(report);
        resultModel.setProcesses(report.result().processes());

        titleLabel.setText(AlgorithmDescriptions.title(report.algorithmName()));
        descriptionLabel.setText(AlgorithmDescriptions.description(report.algorithmName()));
        turnaroundLabel.setText(format(report.metrics().averageTurnaround()));
        waitingLabel.setText(format(report.metrics().averageWaitingTime()));
        switchesLabel.setText(Integer.toString(report.metrics().contextSwitches()));
        durationLabel.setText(chart.totalSeconds() + " s");
        updateControls();
    }

    void stopAnimation() {
        timer.stop();
        updateControls();
    }

    private void togglePlay() {
        if (timer.isRunning()) {
            timer.stop();
        } else {
            if (chart.visibleSeconds() >= chart.totalSeconds()) {
                chart.setVisibleSeconds(0);
            }
            timer.start();
        }
        updateControls();
    }

    private void advance() {
        chart.setVisibleSeconds(chart.visibleSeconds() + 1);
        if (chart.visibleSeconds() >= chart.totalSeconds()) {
            timer.stop();
        }
        followCursor();
        updateControls();
    }

    private void step() {
        timer.stop();
        if (chart.visibleSeconds() >= chart.totalSeconds()) {
            chart.setVisibleSeconds(0);
        }
        advance();
    }

    private void restart() {
        timer.stop();
        chart.setVisibleSeconds(0);
        followCursor();
        updateControls();
    }

    private void showAll() {
        timer.stop();
        chart.setVisibleSeconds(chart.totalSeconds());
        updateControls();
    }

    private void followCursor() {
        int x = chart.xOf(chart.visibleSeconds());
        chart.scrollRectToVisible(new Rectangle(
                Math.max(0, x - GanttChartPanel.CELL_WIDTH * 3), 0, GanttChartPanel.CELL_WIDTH * 6, 1));
    }

    private void updateControls() {
        boolean hasReport = chart.totalSeconds() > 0;
        playButton.setText(timer.isRunning() ? "Pausar" : "Reproduzir");
        playButton.setEnabled(hasReport);
        stepButton.setEnabled(hasReport);
        restartButton.setEnabled(hasReport);
        showAllButton.setEnabled(hasReport);
        timeLabel.setText("t = " + chart.visibleSeconds() + " / " + chart.totalSeconds() + " s");
    }

    private JPanel controlsPanel() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controls.add(playButton);
        controls.add(stepButton);
        controls.add(restartButton);
        controls.add(showAllButton);
        controls.add(new JLabel("   Velocidade:"));
        // O valor do controle representa atraso; por isso, a escala visual é invertida.
        speedSlider.setInverted(true);
        speedSlider.setToolTipText("Intervalo entre segundos simulados");
        controls.add(speedSlider);
        controls.add(timeLabel);
        return controls;
    }

    private JPanel metricsPanel() {
        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.add(metricCard("Turnaround médio (tt)", turnaroundLabel));
        metrics.add(metricCard("Espera média (tw)", waitingLabel));
        metrics.add(metricCard("Trocas de contexto", switchesLabel));
        metrics.add(metricCard("Tempo total", durationLabel));
        return metrics;
    }

    private static JPanel metricCard(String caption, JLabel value) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        card.add(new JLabel(caption), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private static JLabel metricLabel() {
        JLabel label = new JLabel("—");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 20f));
        return label;
    }

    static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
