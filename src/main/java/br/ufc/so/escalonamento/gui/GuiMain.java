package br.ufc.so.escalonamento.gui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/** Inicializa a interface Swing na thread de eventos. */
public final class GuiMain {
    private GuiMain() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ReflectiveOperationException | UnsupportedLookAndFeelException exception) {
                // Falha ao aplicar o tema não impede a inicialização.
            }
            new SchedulerWindow().setVisible(true);
        });
    }
}
