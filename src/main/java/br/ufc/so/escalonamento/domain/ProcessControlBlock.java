package br.ufc.so.escalonamento.domain;

public final class ProcessControlBlock {
    private final int id;
    private final int arrivalTime;
    private final int duration;
    private final int staticPriority;

    private int remainingTime;
    private int dynamicPriority;
    private ProcessState state;
    private Integer completionTime;

    public ProcessControlBlock(int id, int arrivalTime, int duration, int staticPriority) {
        if (id <= 0) {
            throw new IllegalArgumentException("O identificador deve ser maior que zero.");
        }
        if (arrivalTime < 0) {
            throw new IllegalArgumentException("O instante de criação não pode ser negativo.");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("A duração deve ser maior que zero.");
        }
        if (staticPriority <= 0) {
            throw new IllegalArgumentException("A prioridade estática deve ser maior que zero.");
        }

        this.id = id;
        this.arrivalTime = arrivalTime;
        this.duration = duration;
        this.staticPriority = staticPriority;
        this.remainingTime = duration;
        this.dynamicPriority = staticPriority;
        this.state = ProcessState.NEW;
    }

    public int id() {
        return id;
    }

    public int arrivalTime() {
        return arrivalTime;
    }

    public int duration() {
        return duration;
    }

    public int staticPriority() {
        return staticPriority;
    }

    public int remainingTime() {
        return remainingTime;
    }

    public int dynamicPriority() {
        return dynamicPriority;
    }

    public ProcessState state() {
        return state;
    }

    public Integer completionTime() {
        return completionTime;
    }

    public ProcessControlBlock freshCopy() {
        return new ProcessControlBlock(id, arrivalTime, duration, staticPriority);
    }

    public void markReady() {
        if (state != ProcessState.NEW && state != ProcessState.RUNNING) {
            throw invalidTransition(ProcessState.READY);
        }
        if (remainingTime == 0) {
            throw new IllegalStateException("Um processo concluído não pode retornar ao estado pronto.");
        }
        state = ProcessState.READY;
    }

    public void markRunning() {
        if (state != ProcessState.READY) {
            throw invalidTransition(ProcessState.RUNNING);
        }
        state = ProcessState.RUNNING;
    }

    public void executeOneSecond() {
        if (state != ProcessState.RUNNING) {
            throw new IllegalStateException("Somente um processo em execução pode consumir CPU.");
        }
        if (remainingTime == 0) {
            throw new IllegalStateException("Um processo sem tempo restante não pode consumir CPU.");
        }
        remainingTime--;
    }

    public void terminateAt(int instant) {
        if (state != ProcessState.RUNNING || remainingTime != 0) {
            throw invalidTransition(ProcessState.TERMINATED);
        }
        if (instant < arrivalTime + duration) {
            throw new IllegalArgumentException("O término não pode anteceder o tempo mínimo de execução.");
        }
        completionTime = instant;
        state = ProcessState.TERMINATED;
    }

    public void applyAging(int rate) {
        if (state != ProcessState.READY) {
            throw new IllegalStateException("Somente um processo pronto pode receber aging.");
        }
        if (rate < 0) {
            throw new IllegalArgumentException("A taxa de aging não pode ser negativa.");
        }
        dynamicPriority = Math.addExact(dynamicPriority, rate);
    }

    public void resetDynamicPriority() {
        if (state != ProcessState.RUNNING) {
            throw new IllegalStateException("Somente o processo selecionado pode restaurar sua prioridade.");
        }
        dynamicPriority = staticPriority;
    }

    private IllegalStateException invalidTransition(ProcessState target) {
        return new IllegalStateException("Transição inválida de " + state + " para " + target + ".");
    }
}
