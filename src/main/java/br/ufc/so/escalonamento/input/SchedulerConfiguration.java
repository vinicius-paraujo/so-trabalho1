package br.ufc.so.escalonamento.input;

public record SchedulerConfiguration(int quantum, int agingRate) {
    public SchedulerConfiguration {
        if (quantum <= 0) {
            throw new IllegalArgumentException("O quantum deve ser maior que zero.");
        }
        if (agingRate < 0) {
            throw new IllegalArgumentException("A taxa de aging não pode ser negativa.");
        }
    }
}

