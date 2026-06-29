import java.awt.*;
import java.util.Arrays;

public class GestorMemoria {

    static final int SLOTS     = 21;
    static final int ALTO_SLOT = 20;
    static final int RAM_MAX   = 512;

    static float[] slots = new float[SLOTS];
    static int  suma     = 0;
    static int  p        = 0;
    static int  ultimaDireccion = 0;
    static boolean def   = false;

    GestorMemoria() { limpiarSlots(); }

    static void limpiarSlots() { Arrays.fill(slots, 0f); }

    static int generarTamano() {
        return (int)(Math.random() * 251 + 5);
    }

    static int convertirAKb(int mb) {
        float proporcion = ((float) mb * 100f / (float) RAM_MAX) * ALTO_SLOT;
        return (int)(proporcion / 10f);
    }

    void agregar(int mb) {
        suma += mb;
        float porcentaje = (float) mb * 100f / (float) RAM_MAX;
        float altoVisual = (ALTO_SLOT * porcentaje) / 100f;
        agregarSlot(altoVisual);
        p++;
    }

    private void agregarSlot(float valor) {
        try {
            int i = 0;
            while (slots[i] != 0) i++;
            ultimaDireccion = i;
            slots[i] = valor;
        } catch (ArrayIndexOutOfBoundsException e) {
            defragmentar();
        }
    }

    void quitarSlot(int posicion) {
        if (posicion >= 0 && posicion < SLOTS) slots[posicion] = 0;
    }

    void quitarMemoria(int mb) {
        suma = Math.max(0, suma - mb);
    }

    void eliminarTodos() { limpiarSlots(); }

    int totalOcupado() {
        int total = 0;
        for (float s : slots) total += (int) s;
        return total;
    }

    private void defragmentar() {
        limpiarSlots();
        p = 0;
        def = true;
        int aux = suma;
        suma = 0;
        while (aux > RAM_MAX) { agregar(RAM_MAX); aux -= RAM_MAX; }
        agregar(aux);
        suma = aux + (suma - aux);
    }

    void dibujar(Graphics2D g2) {
        int rx = 775, ry = 80;

        g2.setColor(new Color(30, 35, 55));
        g2.fillRoundRect(rx - 5, ry - 25, 220, 20, 6, 6);
        g2.setColor(new Color(99, 179, 237));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.drawString("MEMORIA RAM", rx + 50, ry - 10);

        int u = 0;
        for (int j = 0; j < slots.length; j++) {
            g2.setColor(new Color(30, 35, 55));
            g2.fillRect(rx, ry + u, 200, ALTO_SLOT);
            g2.setColor(new Color(50, 60, 85));
            g2.drawRect(rx, ry + u, 200, ALTO_SLOT);

            if (slots[j] > 0) {
                g2.setColor(new Color(49, 130, 206, 200));
                g2.fillRect(rx + 1, ry + u + 1, (int) slots[j] * 2, ALTO_SLOT - 2);
            }

            g2.setColor(new Color(160, 180, 210));
            g2.setFont(new Font("Consolas", Font.PLAIN, 9));
            g2.drawString(String.format("%4d Mb", (int)(slots[j] * 10)), rx + 110, ry + u + 13);

            u += ALTO_SLOT;
        }

        int uso = Math.min(suma * 200 / (RAM_MAX * SLOTS / 5), 200);
        g2.setColor(new Color(40, 50, 75));
        g2.fillRoundRect(rx, ry + u + 5, 200, 10, 5, 5);
        Color barColor = suma > 400 ? new Color(252, 129, 74) : new Color(72, 199, 142);
        g2.setColor(barColor);
        g2.fillRoundRect(rx, ry + u + 5, uso, 10, 5, 5);
        g2.setColor(new Color(160, 180, 210));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        g2.drawString("Uso total: " + suma + " Mb", rx + 5, ry + u + 25);
    }
}
