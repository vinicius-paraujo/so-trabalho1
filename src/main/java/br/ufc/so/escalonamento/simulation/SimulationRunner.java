package br.ufc.so.escalonamento.simulation;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.input.SchedulerConfiguration;
import br.ufc.so.escalonamento.metrics.MetricsCalculator;
import br.ufc.so.escalonamento.scheduler.FcfsScheduler;
import br.ufc.so.escalonamento.scheduler.NonPreemptivePriorityScheduler;
import br.ufc.so.escalonamento.scheduler.PreemptivePriorityScheduler;
import br.ufc.so.escalonamento.scheduler.PriorityRoundRobinScheduler;
import br.ufc.so.escalonamento.scheduler.RoundRobinScheduler;
import br.ufc.so.escalonamento.scheduler.SchedulingResult;
import br.ufc.so.escalonamento.scheduler.SjfScheduler;
import br.ufc.so.escalonamento.scheduler.SrtfScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Executa os sete algoritmos, na ordem do enunciado, sobre a mesma entrada. Cada escalonador
 * copia os PCBs, de modo que nenhuma execução interfere nas seguintes.
 */
public final class SimulationRunner {
    public static final String FCFS = "FCFS";
    public static final String SJF = "SJF";
    public static final String SRTF = "SRTF";
    public static final String NON_PREEMPTIVE_PRIORITY = "PRIORIDADE_SEM_PREEMPCAO";
    public static final String PREEMPTIVE_PRIORITY = "PRIORIDADE_COM_PREEMPCAO";
    public static final String ROUND_ROBIN = "ROUND_ROBIN";
    public static final String PRIORITY_ROUND_ROBIN = "ROUND_ROBIN_PRIORIDADE_AGING";

    private final Supplier<Random> randomSupplier;
    private final MetricsCalculator metricsCalculator = new MetricsCalculator();

    public SimulationRunner() {
        this(Random::new);
    }

    /**
     * Cada algoritmo recebe sua própria fonte aleatória. Nos testes, uma semente fixa por
     * algoritmo reproduz exatamente os resultados dos testes unitários de cada escalonador.
     */
    public SimulationRunner(Supplier<Random> randomSupplier) {
        this.randomSupplier = Objects.requireNonNull(
                randomSupplier, "O fornecedor de fontes aleatórias é obrigatório.");
    }

    public List<AlgorithmReport> runAll(
            List<ProcessControlBlock> processes,
            SchedulerConfiguration configuration) {
        Objects.requireNonNull(processes, "A lista de processos é obrigatória.");
        Objects.requireNonNull(configuration, "A configuração é obrigatória.");

        List<AlgorithmReport> reports = new ArrayList<>();
        reports.add(report(FCFS,
                new FcfsScheduler(randomSupplier.get()).schedule(processes)));
        reports.add(report(SJF,
                new SjfScheduler(randomSupplier.get()).schedule(processes)));
        reports.add(report(SRTF,
                new SrtfScheduler(randomSupplier.get()).schedule(processes)));
        reports.add(report(NON_PREEMPTIVE_PRIORITY,
                new NonPreemptivePriorityScheduler(randomSupplier.get()).schedule(processes)));
        reports.add(report(PREEMPTIVE_PRIORITY,
                new PreemptivePriorityScheduler(randomSupplier.get()).schedule(processes)));
        reports.add(report(ROUND_ROBIN,
                new RoundRobinScheduler(configuration.quantum()).schedule(processes)));
        reports.add(report(PRIORITY_ROUND_ROBIN,
                new PriorityRoundRobinScheduler(
                        configuration.quantum(),
                        configuration.agingRate(),
                        randomSupplier.get()).schedule(processes)));
        return List.copyOf(reports);
    }

    private AlgorithmReport report(String algorithmName, SchedulingResult result) {
        return new AlgorithmReport(algorithmName, result, metricsCalculator.calculate(result));
    }
}
