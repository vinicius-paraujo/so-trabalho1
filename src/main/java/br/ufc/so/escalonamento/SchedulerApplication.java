package br.ufc.so.escalonamento;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.input.ConfigurationParser;
import br.ufc.so.escalonamento.input.InputValidationException;
import br.ufc.so.escalonamento.input.ProcessInputParser;
import br.ufc.so.escalonamento.input.SchedulerConfiguration;
import br.ufc.so.escalonamento.output.ResultFormatter;
import br.ufc.so.escalonamento.simulation.AlgorithmReport;
import br.ufc.so.escalonamento.simulation.SimulationRunner;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Contrato de terminal (ADR 0005): o único argumento é o caminho da configuração e os
 * processos são lidos de stdin. Fluxos e fontes aleatórias são injetados para os testes.
 */
public final class SchedulerApplication {
    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_INVALID_INPUT = 1;
    public static final int EXIT_USAGE = 2;

    private final SimulationRunner simulationRunner;
    private final ConfigurationParser configurationParser = new ConfigurationParser();
    private final ProcessInputParser processInputParser = new ProcessInputParser();
    private final ResultFormatter resultFormatter = new ResultFormatter();

    public SchedulerApplication() {
        this(new SimulationRunner());
    }

    public SchedulerApplication(SimulationRunner simulationRunner) {
        this.simulationRunner = Objects.requireNonNull(
                simulationRunner, "O executor da simulação é obrigatório.");
    }

    public int run(String[] args, Reader processInput, PrintStream out, PrintStream err) {
        if (args.length != 1) {
            err.println("Uso: escalonamento <arquivo-de-configuracao> < processos.txt");
            return EXIT_USAGE;
        }

        SchedulerConfiguration configuration;
        try {
            configuration = configurationParser.parse(Path.of(args[0]));
        } catch (NoSuchFileException | InvalidPathException exception) {
            err.println("Erro na configuração: arquivo não encontrado: " + args[0]);
            return EXIT_INVALID_INPUT;
        } catch (IOException exception) {
            err.println("Erro na configuração: não foi possível ler " + args[0] + ".");
            return EXIT_INVALID_INPUT;
        } catch (InputValidationException exception) {
            err.println("Erro na configuração: " + exception.getMessage());
            return EXIT_INVALID_INPUT;
        }

        List<ProcessControlBlock> processes;
        try {
            processes = processInputParser.parse(processInput);
        } catch (IOException exception) {
            err.println("Erro na entrada: não foi possível ler a entrada padrão.");
            return EXIT_INVALID_INPUT;
        } catch (InputValidationException exception) {
            err.println("Erro na entrada: " + exception.getMessage());
            return EXIT_INVALID_INPUT;
        }

        // A saída é montada por completo antes da escrita, evitando resultado parcial.
        List<AlgorithmReport> reports = simulationRunner.runAll(processes, configuration);
        out.print(resultFormatter.formatAll(reports));
        out.flush();
        return EXIT_SUCCESS;
    }
}
