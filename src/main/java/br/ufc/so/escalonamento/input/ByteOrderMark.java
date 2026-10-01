package br.ufc.so.escalonamento.input;

/** Remove o BOM UTF-8 que pode preceder a primeira linha de arquivos ou fluxos redirecionados. */
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
