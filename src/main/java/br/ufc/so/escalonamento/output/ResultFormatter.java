package br.ufc.so.escalonamento.output;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.simulation.AlgorithmReport;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Formata métricas e linha do tempo com separador {@code '\n'} independente da plataforma. */
public final class ResultFormatter {
    static final String RUNNING = "##";
    static final String NOT_RUNNING = "--";

    public String formatAll(List<AlgorithmReport> reports) {
        Objects.requireNonNull(reports, "Os resultados são obrigatórios.");

        StringBuilder output = new StringBuilder();
        for (int index = 0; index < reports.size(); index++) {
            if (index > 0) {
                output.append('\n');
            }
            output.append(format(reports.get(index)));
        }
        return output.toString();
    }

    public String format(AlgorithmReport report) {
        Objects.requireNonNull(report, "O resultado do algoritmo é obrigatório.");
        List<ProcessControlBlock> processes = report.result().processes();

        StringBuilder section = new StringBuilder();
        section.append("algoritmo:").append(report.algorithmName()).append('\n');
        section.append("tt:").append(formatAverage(report.metrics().averageTurnaround())).append('\n');
        section.append("tw:").append(formatAverage(report.metrics().averageWaitingTime())).append('\n');
        section.append("trocas_contexto:").append(report.metrics().contextSwitches()).append('\n');

        section.append("tempo");
        for (ProcessControlBlock process : processes) {
            section.append(" P").append(process.id());
        }
        section.append('\n');

        List<Integer> timeline = report.result().timeline();
        for (int instant = 0; instant < timeline.size(); instant++) {
            section.append(instant).append('-').append(instant + 1);
            for (ProcessControlBlock process : processes) {
                section.append(' ').append(timeline.get(instant) == process.id() ? RUNNING : NOT_RUNNING);
            }
            section.append('\n');
        }

        return section.toString();
    }

    private String formatAverage(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
