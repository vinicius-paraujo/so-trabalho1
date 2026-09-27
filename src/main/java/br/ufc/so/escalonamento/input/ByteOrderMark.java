package br.ufc.so.escalonamento.input;

/**
 * O Windows PowerShell 5.1 insere um BOM UTF-8 ao encaminhar texto para programas externos,
 * e editores como o Bloco de Notas podem salvá-lo nos arquivos. Sem esta remoção, a primeira
 * linha de uma entrada válida seria rejeitada.
 */
final class ByteOrderMark {
    private static final char BOM = '﻿';

    private ByteOrderMark() {
    }

    static String strip(String firstLine) {
        if (!firstLine.isEmpty() && firstLine.charAt(0) == BOM) {
            return firstLine.substring(1);
        }
        return firstLine;
    }
}
