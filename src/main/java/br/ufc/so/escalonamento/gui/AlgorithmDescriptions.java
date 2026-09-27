package br.ufc.so.escalonamento.gui;

import br.ufc.so.escalonamento.simulation.SimulationRunner;

/** Textos de apresentação dos algoritmos; não interferem em sua execução. */
final class AlgorithmDescriptions {
    private AlgorithmDescriptions() {
    }

    static String title(String algorithmName) {
        return switch (algorithmName) {
            case SimulationRunner.FCFS -> "FCFS";
            case SimulationRunner.SJF -> "SJF";
            case SimulationRunner.SRTF -> "SRTF";
            case SimulationRunner.NON_PREEMPTIVE_PRIORITY -> "Prioridade (sem preempção)";
            case SimulationRunner.PREEMPTIVE_PRIORITY -> "Prioridade (com preempção)";
            case SimulationRunner.ROUND_ROBIN -> "Round-Robin";
            case SimulationRunner.PRIORITY_ROUND_ROBIN -> "Round-Robin com prioridade e aging";
            default -> algorithmName;
        };
    }

    static String description(String algorithmName) {
        return switch (algorithmName) {
            case SimulationRunner.FCFS ->
                    "Executa por ordem de chegada, sem preempção.";
            case SimulationRunner.SJF ->
                    "Escolhe o pronto de menor duração e o executa até o fim, sem preempção.";
            case SimulationRunner.SRTF ->
                    "A cada segundo, executa o pronto de menor tempo restante; chegadas mais curtas preemptam.";
            case SimulationRunner.NON_PREEMPTIVE_PRIORITY ->
                    "Escolhe o pronto de maior prioridade estática e o executa até o fim.";
            case SimulationRunner.PREEMPTIVE_PRIORITY ->
                    "Chegada de processo com prioridade estritamente maior preempta o atual.";
            case SimulationRunner.ROUND_ROBIN ->
                    "Fila FIFO; cada processo executa por até um quantum e volta ao final da fila.";
            case SimulationRunner.PRIORITY_ROUND_ROBIN ->
                    "Maior prioridade dinâmica a cada quantum; quem aguarda recebe aging ao fim de cada quantum.";
            default -> "";
        };
    }
}
