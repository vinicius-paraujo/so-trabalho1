package br.ufc.so.escalonamento.gui;

import static br.ufc.so.escalonamento.scheduler.SchedulingAssertions.referenceProcesses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufc.so.escalonamento.domain.ProcessControlBlock;
import br.ufc.so.escalonamento.gui.TimelinePresentation.DisplayState;
import br.ufc.so.escalonamento.gui.TimelinePresentation.Segment;
import br.ufc.so.escalonamento.scheduler.PreemptivePriorityScheduler;
import br.ufc.so.escalonamento.scheduler.SchedulingResult;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TimelinePresentationTest {
    @Test
    void deveAgruparInstantesConsecutivosDoMesmoProcesso() {
        List<Segment> segments = TimelinePresentation.segments(List.of(2, 2, 1, 4, 4, 4, 1));

        assertEquals(
                List.of(new Segment(0, 2, 2), new Segment(2, 3, 1), new Segment(3, 6, 4), new Segment(6, 7, 1)),
                segments);
    }

    @Test
    void deveRepresentarCpuOciosaComoSegmentoProprio() {
        int idle = SchedulingResult.IDLE;

        List<Segment> segments = TimelinePresentation.segments(List.of(idle, idle, 1, idle, 2));

        assertEquals(4, segments.size());
        assertTrue(segments.get(0).idle());
        assertEquals(2, segments.get(0).length());
        assertTrue(segments.get(2).idle());
    }

    @Test
    void deveRetornarListaVaziaParaLinhaDoTempoVazia() {
        assertTrue(TimelinePresentation.segments(List.of()).isEmpty());
    }

    @Test
    void deveDerivarOEstadoExibidoDeCadaProcesso() {
        SchedulingResult result = new PreemptivePriorityScheduler(new Random(0)).schedule(referenceProcesses());
        List<Integer> timeline = result.timeline();
        // Linha do tempo: [0,2) P2; [2,3) P1; [3,6) P4; [6,10) P1; [10,14) P3.
        ProcessControlBlock p1 = result.processes().get(0);
        ProcessControlBlock p4 = result.processes().get(3);

        assertEquals(DisplayState.READY, TimelinePresentation.stateAt(p1, timeline, 0));
        assertEquals(DisplayState.RUNNING, TimelinePresentation.stateAt(p1, timeline, 2));
        assertEquals(DisplayState.READY, TimelinePresentation.stateAt(p1, timeline, 3));
        assertEquals(DisplayState.TERMINATED, TimelinePresentation.stateAt(p1, timeline, 10));
        assertEquals(DisplayState.NOT_CREATED, TimelinePresentation.stateAt(p4, timeline, 2));
        assertEquals(DisplayState.RUNNING, TimelinePresentation.stateAt(p4, timeline, 3));
        assertEquals(DisplayState.TERMINATED, TimelinePresentation.stateAt(p4, timeline, 6));
    }
}
