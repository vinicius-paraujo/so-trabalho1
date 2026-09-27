package br.ufc.so.escalonamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufc.so.escalonamento.simulation.SimulationRunner;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SchedulerApplicationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void deveReproduzirASaidaCompletaDoCasoDeReferencia() throws IOException, URISyntaxException {
        Path caseDirectory = casesDirectory().resolve("reference");

        Execution execution = run(caseDirectory.resolve("config.txt"), read(caseDirectory.resolve("processes.txt")));

        assertEquals(SchedulerApplication.EXIT_SUCCESS, execution.exitCode());
        assertEquals("", execution.err());
        assertEquals(read(caseDirectory.resolve("expected-output.txt")), execution.out());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("caseDirectories")
    void deveExecutarTodosOsCasosVersionados(String caseName) throws IOException, URISyntaxException {
        Path caseDirectory = casesDirectory().resolve(caseName);
        String processes = read(caseDirectory.resolve("processes.txt"));
        int processCount = (int) processes.lines().count();

        Execution execution = run(caseDirectory.resolve("config.txt"), processes);

        assertEquals(SchedulerApplication.EXIT_SUCCESS, execution.exitCode(), execution.err());
        assertEquals("", execution.err());

        List<String> sections = List.of(execution.out().split("\n\n"));
        assertEquals(7, sections.size());
        for (String section : sections) {
            List<String> lines = section.lines().toList();
            assertTrue(lines.get(0).startsWith("algoritmo:"));
            assertTrue(lines.get(1).matches("tt:\\d+\\.\\d{2}"), lines.get(1));
            assertTrue(lines.get(2).matches("tw:\\d+\\.\\d{2}"), lines.get(2));
            assertTrue(lines.get(3).matches("trocas_contexto:\\d+"), lines.get(3));
            assertEquals(processCount + 1, lines.get(4).split(" ").length);
            for (int row = 5; row < lines.size(); row++) {
                assertEquals((row - 5) + "-" + (row - 4), lines.get(row).split(" ")[0]);
            }
        }
    }

    @Test
    void deveApresentarCpuOciosaEmTodosOsAlgoritmos() throws IOException, URISyntaxException {
        Path caseDirectory = casesDirectory().resolve("idle");

        Execution execution = run(caseDirectory.resolve("config.txt"), read(caseDirectory.resolve("processes.txt")));

        for (String section : execution.out().split("\n\n")) {
            // A divisão por linha vazia remove a quebra final das seções intermediárias.
            assertTrue(section.stripTrailing().endsWith("""
                    trocas_contexto:0
                    tempo P1 P2
                    0-1 -- --
                    1-2 -- --
                    2-3 ## --
                    3-4 -- --
                    4-5 -- ##"""), section);
        }
    }

    @Test
    void deveExigirExatamenteUmArgumento() {
        Execution withoutArguments = run(new String[0], "0 1 1");
        Execution withTwoArguments = run(new String[] {"a", "b"}, "0 1 1");

        assertEquals(SchedulerApplication.EXIT_USAGE, withoutArguments.exitCode());
        assertEquals(SchedulerApplication.EXIT_USAGE, withTwoArguments.exitCode());
        assertEquals("", withoutArguments.out());
        assertTrue(withoutArguments.err().startsWith("Uso:"));
    }

    @Test
    void deveRejeitarArquivoDeConfiguracaoInexistente() {
        Execution execution = run(temporaryDirectory.resolve("ausente.txt"), "0 1 1");

        assertEquals(SchedulerApplication.EXIT_INVALID_INPUT, execution.exitCode());
        assertEquals("", execution.out());
        assertTrue(execution.err().contains("arquivo não encontrado"), execution.err());
    }

    @Test
    void deveRejeitarConfiguracaoInvalidaSemResultadoParcial() throws IOException {
        Path configuration = write("quantum:0\naging:1");

        Execution execution = run(configuration, "0 1 1");

        assertEquals(SchedulerApplication.EXIT_INVALID_INPUT, execution.exitCode());
        assertEquals("", execution.out());
        assertTrue(execution.err().startsWith("Erro na configuração:"), execution.err());
    }

    @Test
    void deveRejeitarEntradaDeProcessosInvalidaSemResultadoParcial() throws IOException {
        Path configuration = write("quantum:2\naging:1");

        Execution execution = run(configuration, "0 5 2\n0 x 3");

        assertEquals(SchedulerApplication.EXIT_INVALID_INPUT, execution.exitCode());
        assertEquals("", execution.out());
        assertTrue(execution.err().contains("Linha 2"), execution.err());
    }

    @Test
    void deveRejeitarEntradaVazia() throws IOException {
        Path configuration = write("quantum:2\naging:1");

        Execution execution = run(configuration, "");

        assertEquals(SchedulerApplication.EXIT_INVALID_INPUT, execution.exitCode());
        assertEquals("", execution.out());
    }

    static Stream<String> caseDirectories() throws IOException, URISyntaxException {
        try (Stream<Path> directories = Files.list(casesDirectory())) {
            return directories.map(path -> path.getFileName().toString()).sorted().toList().stream();
        }
    }

    private static Path casesDirectory() throws URISyntaxException {
        return Path.of(SchedulerApplicationTest.class.getResource("/cases").toURI());
    }

    /** Normaliza quebras de linha, pois o checkout no Windows pode convertê-las para CRLF. */
    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private Path write(String configuration) throws IOException {
        Path path = temporaryDirectory.resolve("config.txt");
        Files.writeString(path, configuration);
        return path;
    }

    private Execution run(Path configuration, String processes) {
        return run(new String[] {configuration.toString()}, processes);
    }

    private Execution run(String[] args, String processes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        SchedulerApplication application =
                new SchedulerApplication(new SimulationRunner(() -> new Random(0)));

        int exitCode = application.run(
                args,
                new StringReader(processes),
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));

        return new Execution(
                exitCode,
                out.toString(StandardCharsets.UTF_8),
                err.toString(StandardCharsets.UTF_8));
    }

    private record Execution(int exitCode, String out, String err) {
    }
}
