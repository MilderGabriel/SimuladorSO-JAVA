import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame("Simulación de Procesos del SO");
            ventana.setSize(1200, 720);
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.setResizable(false);
            ventana.setLocationRelativeTo(null);

            simulacion5m panel = new simulacion5m();
            ventana.getContentPane().add(panel, BorderLayout.CENTER);

            JOptionPane.showMessageDialog(null,
                    "Simulador de Procesos de CPU\n\n" +
                            "Se iniciarán " + simulacion5m.totalProcesos + " procesos por defecto.\n" +
                            "Puedes modificar la cantidad (1–100) antes de presionar  Iniciar.\n\n" +
                            "Algoritmos disponibles: FIFO, LIFO, Round Robin, Prioridad.",
                    "Bienvenido", JOptionPane.INFORMATION_MESSAGE);

            ventana.setVisible(true);
        });
    }
}
