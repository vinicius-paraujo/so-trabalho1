package br.ufc.so.escalonamento.input;

/** Indica entrada de processos ou configuração inválida. */
public final class InputValidationException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public InputValidationException(String message) {
        super(message);
    }

    public InputValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
