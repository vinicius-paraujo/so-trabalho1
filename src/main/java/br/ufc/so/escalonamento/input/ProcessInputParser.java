package br.ufc.so.escalonamento.input;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ProcessInputParser {
    public List<ProcessControlBlock> parse(Reader source) throws IOException {
        Objects.requireNonNull(source, "A fonte dos processos é obrigatória.");

        List<ProcessControlBlock> processes = new ArrayList<>();
        BufferedReader reader = new BufferedReader(source);

        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.isBlank()) {
                throw error(lineNumber, "a linha não pode estar vazia");
            }

            String[] fields = line.trim().split("\\s+");
            if (fields.length != 3) {
                throw error(lineNumber, "são esperados exatamente três inteiros");
            }

            int arrivalTime = parseInteger(fields[0], lineNumber, "instante de criação");
            int duration = parseInteger(fields[1], lineNumber, "duração");
            int priority = parseInteger(fields[2], lineNumber, "prioridade");

            try {
                processes.add(new ProcessControlBlock(
                        processes.size() + 1,
                        arrivalTime,
                        duration,
                        priority));
            } catch (IllegalArgumentException exception) {
                throw new InputValidationException(
                        "Linha " + lineNumber + ": " + exception.getMessage(),
                        exception);
            }
        }

        if (processes.isEmpty()) {
            throw new InputValidationException("A entrada deve possuir ao menos um processo.");
        }

        return List.copyOf(processes);
    }

    private int parseInteger(String value, int lineNumber, String fieldName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new InputValidationException(
                    "Linha " + lineNumber + ": o campo " + fieldName + " deve ser um inteiro.",
                    exception);
        }
    }

    private InputValidationException error(int lineNumber, String message) {
        return new InputValidationException("Linha " + lineNumber + ": " + message + ".");
    }
}
