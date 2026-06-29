public class Proceso {
    int x, y;
    int indice;
    int estado;
    int cuantum;
    int r, g, b;
    int posicionRAM    = -1;
    int posicionTabla  = -1;
    int memoriaBytes   = 0;
    int memoriaKb      = 0;
    int memoriaVirtual = 0;

    String nombre;
    String recurso     = "—";
    String estadoTexto = "Formado";
    String transicion  = "Formado";
    int    prioridad   = 5;   // 1=alta prioridad, 10=baja prioridad

    Proceso(int x, int y, int indice, int estado, int cuantum,
            String nombre, int r, int g, int b) {
        this.x       = x;
        this.y       = y;
        this.indice  = indice;
        this.estado  = estado;
        this.cuantum = cuantum;
        this.nombre  = nombre;
        this.r = r; this.g = g; this.b = b;
    }

    void moverHacia(int tx, int ty) {
        if (x < tx) x++;
        else if (x > tx) x--;
        if (y < ty) y++;
        else if (y > ty) y--;
    }
}

