package br.ufc.so.escalonamento.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProcessInputParserTest {
    private final ProcessInputParser parser = new ProcessInputParser();

    @Test
    void deveAceitarEspacosTabulacoesEOrdemDeChegadaNaoOrdenada() throws IOException {
        String input = "5\t1 1\n0   2 2\n1 1 3";

        List<ProcessControlBlock> processes = parser.parse(new StringReader(input));

        assertEquals(3, processes.size());
        assertEquals(1, processes.get(0).id());
        assertEquals(5, processes.get(0).arrivalTime());
        assertEquals(2, processes.get(1).id());
        assertEquals(0, processes.get(1).arrivalTime());
        assertEquals(3, processes.get(2).id());
        assertEquals(1, processes.get(2).arrivalTime());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidInputs")
    void deveRejeitarEntradasInvalidas(String description, String input) {
        assertThrows(
                InputValidationException.class,
                () -> parser.parse(new StringReader(input)),
                description);
    }

    private static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of("entrada vazia", ""),
                Arguments.of("linha vazia", "0 1 1\n\n1 1 1"),
                Arguments.of("campo ausente", "0 1"),
                Arguments.of("campo adicional", "0 1 1 4"),
                Arguments.of("campo não inteiro", "zero 1 1"),
                Arguments.of("criação negativa", "-1 1 1"),
                Arguments.of("duração zero", "0 0 1"),
                Arguments.of("duração negativa", "0 -1 1"),
                Arguments.of("prioridade zero", "0 1 0"),
                Arguments.of("prioridade negativa", "0 1 -1"));
    }
}

