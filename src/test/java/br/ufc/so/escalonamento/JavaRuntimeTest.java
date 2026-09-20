package br.ufc.so.escalonamento;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JavaRuntimeTest {
    @Test
    void deveExecutarComJava21() {
        assertEquals(21, Runtime.version().feature());
    }
}

