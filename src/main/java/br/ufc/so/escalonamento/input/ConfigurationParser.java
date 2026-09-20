package br.ufc.so.escalonamento.input;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ConfigurationParser {
    public SchedulerConfiguration parse(Path path) throws IOException {
        Objects.requireNonNull(path, "O caminho da configuração é obrigatório.");
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(reader);
        }
    }

    public SchedulerConfiguration parse(Reader source) throws IOException {
        Objects.requireNonNull(source, "A fonte da configuração é obrigatória.");

        Integer quantum = null;
        Integer aging = null;
        BufferedReader reader = new BufferedReader(source);

        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.isBlank()) {
                throw error(lineNumber, "a linha não pode estar vazia");
            }

            int separator = line.indexOf(':');
            if (separator < 0 || separator != line.lastIndexOf(':')) {
                throw error(lineNumber, "a linha deve conter exatamente um separador ':'");
            }

            String key = line.substring(0, separator).trim();
            String rawValue = line.substring(separator + 1).trim();
            if (rawValue.isEmpty()) {
                throw error(lineNumber, "a chave " + key + " exige um valor");
            }

            int value = parseInteger(rawValue, lineNumber, key);
            switch (key) {
                case "quantum" -> {
                    if (quantum != null) {
                        throw error(lineNumber, "chave duplicada: quantum");
                    }
                    quantum = value;
                }
                case "aging" -> {
                    if (aging != null) {
                        throw error(lineNumber, "chave duplicada: aging");
                    }
                    aging = value;
                }
                default -> throw error(lineNumber, "chave desconhecida: " + key);
            }
        }

        if (quantum == null) {
            throw new InputValidationException("Chave obrigatória ausente: quantum.");
        }
        if (aging == null) {
            throw new InputValidationException("Chave obrigatória ausente: aging.");
        }

        try {
            return new SchedulerConfiguration(quantum, aging);
        } catch (IllegalArgumentException exception) {
            throw new InputValidationException(exception.getMessage(), exception);
        }
    }

    private int parseInteger(String value, int lineNumber, String key) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new InputValidationException(
                    "Linha " + lineNumber + ": o valor de " + key + " deve ser um inteiro.",
                    exception);
        }
    }

    private InputValidationException error(int lineNumber, String message) {
        return new InputValidationException("Linha " + lineNumber + ": " + message + ".");
    }
}
