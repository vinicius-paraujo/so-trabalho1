package br.ufc.so.escalonamento.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConfigurationParserTest {
    private final ConfigurationParser parser = new ConfigurationParser();

    @TempDir
    Path temporaryDirectory;

    @Test
    void deveAceitarOrdemLivreEEspacos() throws IOException {
        SchedulerConfiguration configuration = parser.parse(
                new StringReader(" aging : 1 \n quantum : 2 "));

        assertEquals(2, configuration.quantum());
        assertEquals(1, configuration.agingRate());
    }

    @Test
    void deveAceitarAgingZero() throws IOException {
        SchedulerConfiguration configuration = parser.parse(
                new StringReader("quantum:1\naging:0"));

        assertEquals(0, configuration.agingRate());
    }

    @Test
    void deveLerConfiguracaoDeArquivo() throws IOException {
        Path configurationFile = temporaryDirectory.resolve("config.txt");
        Files.writeString(configurationFile, "quantum:2\naging:1");

        SchedulerConfiguration configuration = parser.parse(configurationFile);

        assertEquals(2, configuration.quantum());
        assertEquals(1, configuration.agingRate());
    }

    @Test
    void deveFalharQuandoArquivoNaoExiste() {
        Path missingFile = temporaryDirectory.resolve("inexistente.txt");

        assertThrows(IOException.class, () -> parser.parse(missingFile));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidConfigurations")
    void deveRejeitarConfiguracoesInvalidas(String description, String input) {
        assertThrows(
                InputValidationException.class,
                () -> parser.parse(new StringReader(input)),
                description);
    }

    private static Stream<Arguments> invalidConfigurations() {
        return Stream.of(
                Arguments.of("arquivo vazio", ""),
                Arguments.of("linha vazia", "quantum:2\n\naging:1"),
                Arguments.of("quantum ausente", "aging:1"),
                Arguments.of("aging ausente", "quantum:2"),
                Arguments.of("chave desconhecida", "quantum:2\nidade:1"),
                Arguments.of("chave duplicada", "quantum:2\nquantum:3\naging:1"),
                Arguments.of("separador ausente", "quantum=2\naging:1"),
                Arguments.of("separador duplicado", "quantum::2\naging:1"),
                Arguments.of("valor ausente", "quantum:\naging:1"),
                Arguments.of("valor não inteiro", "quantum:dois\naging:1"),
                Arguments.of("quantum zero", "quantum:0\naging:1"),
                Arguments.of("quantum negativo", "quantum:-1\naging:1"),
                Arguments.of("aging negativo", "quantum:2\naging:-1"));
    }
}
