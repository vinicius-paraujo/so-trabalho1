package br.ufc.so.escalonamento.gui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/** Ponto de entrada da interface gráfica; a aplicação de terminal permanece em {@code Main}. */
public final class GuiMain {
    private GuiMain() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ReflectiveOperationException | UnsupportedLookAndFeelException exception) {
                // A aparência padrão do Swing continua funcional.
            }
            new SchedulerWindow().setVisible(true);
        });
    }
}
