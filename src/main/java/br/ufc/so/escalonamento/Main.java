package br.ufc.so.escalonamento;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class Main {
    private Main() {
    }

    /**
     * Os fluxos usam UTF-8 explicitamente: no Windows, quando redirecionados, o Java adotaria
     * a codificação legada do sistema e corromperia os acentos das mensagens de erro.
     */
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
