package br.ufc.so.escalonamento.gui;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.input.ConfigurationParser;
import br.ufc.so.escalonamento.input.InputValidationException;
import br.ufc.so.escalonamento.input.ProcessInputParser;
import br.ufc.so.escalonamento.input.SchedulerConfiguration;
import br.ufc.so.escalonamento.simulation.AlgorithmReport;
import br.ufc.so.escalonamento.simulation.SimulationRunner;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

/** Janela principal; delega toda a simulação ao {@link SimulationRunner}. */
@SuppressWarnings("serial")
public final class SchedulerWindow extends JFrame {
    private static final int ALGORITHM_COUNT = 7;

    private final ProcessTableModel processModel = new ProcessTableModel();
    private final JTable processTable = new JTable(processModel);
    private final JSpinner quantumSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 1000, 1));
    private final JSpinner agingSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 1000, 1));
    private final JCheckBox fixedSeedCheckBox = new JCheckBox("Semente fixa para desempates");
    private final JSpinner seedSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    private final JLabel statusLabel = new JLabel(" ");

    private final JTabbedPane tabs = new JTabbedPane();
    private final ComparisonPanel comparisonPanel = new ComparisonPanel();
    private final List<AlgorithmPanel> algorithmPanels = new ArrayList<>();

    public SchedulerWindow() {
        super("Simulador de Escalonamento de Processos — CK0234");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setJMenuBar(menuBar());

        tabs.addTab("Comparação", comparisonPanel);
        for (int index = 0; index < ALGORITHM_COUNT; index++) {
            AlgorithmPanel panel = new AlgorithmPanel();
            algorithmPanels.add(panel);
            tabs.addTab("…", panel);
        }
        tabs.addChangeListener(event -> algorithmPanels.forEach(AlgorithmPanel::stopAnimation));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel(), tabs);
        split.setDividerLocation(340);
        split.setResizeWeight(0);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(split, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        setMinimumSize(new Dimension(1000, 640));
        setSize(1320, 820);
        setLocationRelativeTo(null);

        processModel.loadStatementExample();
        simulate();
    }

    private JMenuBar menuBar() {
        JMenuItem importProcesses = new JMenuItem("Importar processos...");
        importProcesses.addActionListener(event -> importProcesses());
        JMenuItem importConfiguration = new JMenuItem("Importar configuração...");
        importConfiguration.addActionListener(event -> importConfiguration());
        JMenuItem exit = new JMenuItem("Sair");
        exit.addActionListener(event -> dispose());

        JMenu file = new JMenu("Arquivo");
        file.add(importProcesses);
        file.add(importConfiguration);
        file.addSeparator();
        file.add(exit);

        JMenuBar bar = new JMenuBar();
        bar.add(file);
        return bar;
    }

    private JPanel inputPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 6));

        JLabel title = new JLabel("Processos");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        JLabel hint = new JLabel("<html>Edite os valores diretamente na tabela.<br>Maior número = maior prioridade.</html>");
        JPanel header = new JPanel(new BorderLayout(2, 2));
        header.add(title, BorderLayout.NORTH);
        header.add(hint, BorderLayout.CENTER);

        processTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        processTable.setRowHeight(22);
        processTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        JButton addButton = new JButton("Adicionar");
        addButton.addActionListener(event -> {
            stopEditing();
            processModel.addProcess();
        });
        JButton removeButton = new JButton("Remover");
        removeButton.addActionListener(event -> {
            stopEditing();
            processModel.removeProcess(processTable.getSelectedRow());
        });
        JButton exampleButton = new JButton("Exemplo do enunciado");
        exampleButton.addActionListener(event -> {
            stopEditing();
            processModel.loadStatementExample();
            quantumSpinner.setValue(2);
            agingSpinner.setValue(1);
        });
        JButton clearButton = new JButton("Limpar");
        clearButton.addActionListener(event -> {
            stopEditing();
            processModel.clear();
        });

        JPanel tableButtons = new JPanel(new GridLayout(2, 2, 6, 6));
        tableButtons.add(addButton);
        tableButtons.add(removeButton);
        tableButtons.add(exampleButton);
        tableButtons.add(clearButton);

        JPanel configuration = new JPanel(new GridBagLayout());
        configuration.setBorder(BorderFactory.createTitledBorder("Configuração"));
        addRow(configuration, 0, "Quantum", quantumSpinner);
        addRow(configuration, 1, "Aging", agingSpinner);
        seedSpinner.setEnabled(false);
        fixedSeedCheckBox.setToolTipText("Torna reproduzível a escolha aleatória entre processos empatados.");
        fixedSeedCheckBox.addActionListener(event -> seedSpinner.setEnabled(fixedSeedCheckBox.isSelected()));
        GridBagConstraints checkBoxConstraints = constraints(0, 2);
        checkBoxConstraints.gridwidth = 2;
        configuration.add(fixedSeedCheckBox, checkBoxConstraints);
        addRow(configuration, 3, "Semente", seedSpinner);

        JButton simulateButton = new JButton("Simular");
        simulateButton.setFont(simulateButton.getFont().deriveFont(Font.BOLD, 15f));
        simulateButton.addActionListener(event -> simulate());
        getRootPane().setDefaultButton(simulateButton);

        JPanel south = new JPanel(new BorderLayout(6, 8));
        south.add(tableButtons, BorderLayout.NORTH);
        south.add(configuration, BorderLayout.CENTER);
        south.add(simulateButton, BorderLayout.SOUTH);

        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(processTable), BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private void simulate() {
        stopEditing();
        algorithmPanels.forEach(AlgorithmPanel::stopAnimation);

        List<ProcessControlBlock> processes;
        try {
            processes = processModel.toProcesses();
        } catch (InputValidationException exception) {
            showError("Processos inválidos",
                    exception.getMessage() + "\nA linha N da tabela corresponde ao processo PN.");
            return;
        }

        SchedulerConfiguration configuration;
        try {
            configuration = new SchedulerConfiguration(intValue(quantumSpinner), intValue(agingSpinner));
        } catch (IllegalArgumentException exception) {
            showError("Configuração inválida", exception.getMessage());
            return;
        }

        SimulationRunner runner;
        if (fixedSeedCheckBox.isSelected()) {
            long seed = intValue(seedSpinner);
            runner = new SimulationRunner(() -> new Random(seed));
        } else {
            runner = new SimulationRunner();
        }

        List<AlgorithmReport> reports = runner.runAll(processes, configuration);
        comparisonPanel.setReports(reports);
        for (int index = 0; index < reports.size(); index++) {
            algorithmPanels.get(index).setReport(reports.get(index));
            tabs.setTitleAt(index + 1, AlgorithmDescriptions.title(reports.get(index).algorithmName()));
        }

        statusLabel.setText(String.format(
                "Simulação concluída: %d processo(s), quantum %d, aging %d%s.",
                processes.size(),
                configuration.quantum(),
                configuration.agingRate(),
                fixedSeedCheckBox.isSelected() ? ", semente " + intValue(seedSpinner) : ", semente aleatória"));
    }

    private void importProcesses() {
        Path path = choosePath("Importar processos");
        if (path == null) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            processModel.setProcesses(new ProcessInputParser().parse(reader));
            simulate();
        } catch (IOException exception) {
            showError("Erro ao ler arquivo", "Não foi possível ler " + path + ".");
        } catch (InputValidationException exception) {
            showError("Processos inválidos", exception.getMessage());
        }
    }

    private void importConfiguration() {
        Path path = choosePath("Importar configuração");
        if (path == null) {
            return;
        }
        try {
            SchedulerConfiguration configuration = new ConfigurationParser().parse(path);
            quantumSpinner.setValue(configuration.quantum());
            agingSpinner.setValue(configuration.agingRate());
            simulate();
        } catch (IOException exception) {
            showError("Erro ao ler arquivo", "Não foi possível ler " + path + ".");
        } catch (InputValidationException exception) {
            showError("Configuração inválida", exception.getMessage());
        }
    }

    private Path choosePath(String title) {
        JFileChooser chooser = new JFileChooser(Path.of("").toAbsolutePath().toFile());
        chooser.setDialogTitle(title);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return chooser.getSelectedFile().toPath();
    }

    private void stopEditing() {
        if (processTable.isEditing()) {
            processTable.getCellEditor().stopCellEditing();
        }
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private static int intValue(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }

    private static void addRow(JPanel panel, int row, String label, JComponent field) {
        panel.add(new JLabel(label), constraints(0, row));
        GridBagConstraints fieldConstraints = constraints(1, row);
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, fieldConstraints);
    }

    private static GridBagConstraints constraints(int column, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(3, 4, 3, 4);
        return constraints;
    }
}
