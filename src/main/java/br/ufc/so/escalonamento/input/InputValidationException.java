package br.ufc.so.escalonamento.input;

/** Entrada ou configuração fora do contrato do ADR 0005; a mensagem é exibida ao usuário. */
public final class InputValidationException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public InputValidationException(String message) {
        super(message);
    }

    public InputValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
