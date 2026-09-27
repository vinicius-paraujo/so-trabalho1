package br.ufc.so.escalonamento.gui;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.scheduler.SchedulingResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deriva do resultado já calculado as informações que a interface desenha. Nenhuma decisão de
 * escalonamento é tomada aqui: o estado exibido decorre apenas da criação, da conclusão e da
 * linha do tempo produzidas pelo algoritmo.
 */
public final class TimelinePresentation {
    public enum DisplayState {
        NOT_CREATED,
        READY,
        RUNNING,
        TERMINATED
    }

    /** Intervalo contínuo {@code [start, end)} ocupado pelo mesmo processo ou pela CPU ociosa. */
    public record Segment(int start, int end, int processId) {
        public int length() {
            return end - start;
        }

        public boolean idle() {
            return processId == SchedulingResult.IDLE;
        }
    }

    private TimelinePresentation() {
    }

    public static List<Segment> segments(List<Integer> timeline) {
        Objects.requireNonNull(timeline, "A linha do tempo é obrigatória.");

        List<Segment> segments = new ArrayList<>();
        int start = 0;
        for (int instant = 1; instant <= timeline.size(); instant++) {
            boolean segmentEnds = instant == timeline.size()
                    || !timeline.get(instant).equals(timeline.get(start));
            if (segmentEnds) {
                segments.add(new Segment(start, instant, timeline.get(start)));
                start = instant;
            }
        }
        return segments;
    }

    public static DisplayState stateAt(ProcessControlBlock process, List<Integer> timeline, int instant) {
        if (instant < process.arrivalTime()) {
            return DisplayState.NOT_CREATED;
        }
        if (process.completionTime() != null && instant >= process.completionTime()) {
            return DisplayState.TERMINATED;
        }
        if (instant < timeline.size() && timeline.get(instant) == process.id()) {
            return DisplayState.RUNNING;
        }
        return DisplayState.READY;
    }
}
