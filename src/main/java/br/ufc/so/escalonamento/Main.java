package br.ufc.so.escalonamento;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** Ponto de entrada da aplicação de terminal. */
public final class Main {
    private Main() {
    }

    /** Fixa UTF-8 também para fluxos redirecionados no Windows. */
    public static void main(String[] args) {
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8);

        int exitCode = new SchedulerApplication().run(
                args,
                new InputStreamReader(System.in, StandardCharsets.UTF_8),
                out,
                err);
        System.exit(exitCode);
    }
}
