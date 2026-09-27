package br.ufc.so.escalonamento.output;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.metrics.SchedulingMetrics;
import br.ufc.so.escalonamento.scheduler.SchedulingResult;
import br.ufc.so.escalonamento.simulation.AlgorithmReport;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResultFormatterTest {
    private final ResultFormatter formatter = new ResultFormatter();

    @Test
    void deveFormatarUmaSecaoConformeOCt17() {
        AlgorithmReport report = new AlgorithmReport(
                "FCFS",
                new SchedulingResult(List.of(1, 1), List.of(new ProcessControlBlock(1, 0, 2, 1))),
                new SchedulingMetrics(2.0, 0.0, 0));

        assertEquals("""
                algoritmo:FCFS
                tt:2.00
                tw:0.00
                trocas_contexto:0
                tempo P1
                0-1 ##
                1-2 ##
                """, formatter.format(report));
    }

    @Test
    void deveRepresentarCpuOciosaSomenteComTracos() {
        AlgorithmReport report = new AlgorithmReport(
                "SJF",
                new SchedulingResult(
                        List.of(SchedulingResult.IDLE, 1, SchedulingResult.IDLE, 2),
                        List.of(new ProcessControlBlock(1, 1, 1, 1), new ProcessControlBlock(2, 3, 1, 1))),
                new SchedulingMetrics(1.0, 0.0, 0));

        assertEquals("""
                algoritmo:SJF
                tt:1.00
                tw:0.00
                trocas_contexto:0
                tempo P1 P2
                0-1 -- --
                1-2 ## --
                2-3 -- --
                3-4 -- ##
                """, formatter.format(report));
    }

    @Test
    void deveUsarDuasCasasEPontoDecimal() {
        AlgorithmReport report = new AlgorithmReport(
                "SRTF",
                new SchedulingResult(List.of(1), List.of(new ProcessControlBlock(1, 0, 1, 1))),
                new SchedulingMetrics(5.0 / 3.0, 2.0 / 3.0, 12));

        String section = formatter.format(report);

        assertEquals("tt:1.67", section.lines().toList().get(1));
        assertEquals("tw:0.67", section.lines().toList().get(2));
        assertEquals("trocas_contexto:12", section.lines().toList().get(3));
    }

    @Test
    void deveSepararSecoesPorUmaLinhaVazia() {
        SchedulingResult result = new SchedulingResult(
                List.of(1), List.of(new ProcessControlBlock(1, 0, 1, 1)));
        SchedulingMetrics metrics = new SchedulingMetrics(1.0, 0.0, 0);

        String output = formatter.formatAll(List.of(
                new AlgorithmReport("A", result, metrics),
                new AlgorithmReport("B", result, metrics)));

        assertEquals("""
                algoritmo:A
                tt:1.00
                tw:0.00
                trocas_contexto:0
                tempo P1
                0-1 ##

                algoritmo:B
                tt:1.00
                tw:0.00
                trocas_contexto:0
                tempo P1
                0-1 ##
                """, output);
    }
}
