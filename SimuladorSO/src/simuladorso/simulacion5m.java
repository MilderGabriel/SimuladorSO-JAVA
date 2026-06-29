
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;


public class simulacion5m extends JPanel {

    private static final int POS_INICIAL_X = 43, POS_INICIAL_Y = 200;
    private static final int POS_LISTO_X = 30, POS_LISTO_Y = 50;
    private static final int POS_EJEC_X = 170, POS_EJEC_Y = 170;
    private static final int POS_BLOQ_X = 220, POS_BLOQ_Y = 10;
    private static final int POS_TERM_X = 330, POS_TERM_Y = 170;
    private static final int POS_SUSL_X = 100, POS_SUSL_Y = 300;
    private static final int POS_SUSB_X = 250, POS_SUSB_Y = 300;

    private static final int RADIO = 15;

    private static final int CENTRO_ESTADO = 20;

    private static final Color COLOR_FONDO = new Color(15, 17, 26);
    private static final Color COLOR_PANEL_CTRL = new Color(25, 28, 40);
    private static final Color COLOR_ACENTO = new Color(99, 179, 237);
    private static final Color COLOR_ESTADO = new Color(72, 199, 142);
    private static final Color COLOR_TEXTO = new Color(100, 220, 120);  // verde suave (tu cambio)
    private static final Color COLOR_SUBTEXTO = new Color(113, 128, 150);
    private static final Color COLOR_BTN_NORMAL = new Color(45, 55, 72);
    private static final Color COLOR_BTN_PELIGRO = new Color(197, 48, 48);


    private static final int EST_FORMADO = -1;
    private static final int EST_LISTO = 0;
    private static final int EST_BLOQUEADO = 1;
    private static final int EST_VUELVE = 2;
    private static final int EST_TERMINADO = 6;
    private static final int EST_SUS_LISTO = 7;
    private static final int EST_SUS_BLOQUEADO = 8;


    private static final int ALGO_FIFO = 0;
    private static final int ALGO_LIFO = 1;
    private static final int ALGO_ROUND_ROBIN = 2;
    private static final int ALGO_PRIORIDAD = 3;
    private static int algoritmoActual = ALGO_ROUND_ROBIN;
    private static final String[] NOMBRES_ALGO = {"FIFO", "LIFO", "Round Robin", "Prioridad"};

    private final GestorMemoria gestorRAM = new GestorMemoria();

    private static ArrayList<Proceso> procesos = new ArrayList<>();
    private static int totalProcesos = (int) (Math.random() * 100 + 1);
    private static int procesosElim = 0;
    private static int contadorTabla = 0;
    private static int memoriaVirtual = 0;
    private static double velocidad = 30;

    private int estadoSimulacion = 0;
    private int limiteRAM = 0;
    private int advertenciaRAM = 0;
    private boolean desbordaVirtual = false;
    private boolean primeraVez = true;

    private javax.swing.Timer timerSimulacion;

    private final StringBuilder bufferLog = new StringBuilder();

    private final File archivoLog = new File("SimulacionLog.txt");
    private final File archivoContador = new File("UltimoRespaldo.txt");


    private DefaultTableModel modeloTabla;
    private JTable tabla;
    private JScrollPane scrollTabla;

    private JLabel lblTeclado, lblRaton, lblMonitor, lblImpresora, lblBocinas;

    private JButton btnIniciar, btnDetener, btnReanudar, btnVelocidad,
            btnProcesos, btnDesfrag, btnTerminar;
    private JTextField txtVelocidad, txtProcesos;
    private JComboBox<String> cmbAlgoritmo;
    private JPanel panelControl;


    public simulacion5m() {
        setLayout(null);
        setBackground(COLOR_FONDO);
        inicializarLog();
        construirUI();
    }

    private void construirUI() {

        panelControl = new JPanel(null);
        panelControl.setBounds(0, 0, 1200, 40);
        panelControl.setBackground(COLOR_PANEL_CTRL);
        add(panelControl);

        btnIniciar = crearBoton("Iniciar", COLOR_ESTADO, 450, 7, 100, 26);
        btnTerminar = crearBoton("Terminar", COLOR_BTN_PELIGRO, 555, 7, 100, 26);
        btnDetener = crearBoton("Pausar", COLOR_BTN_NORMAL, 725, 7, 95, 26);
        btnReanudar = crearBoton("Reanudar", COLOR_BTN_NORMAL, 660, 7, 62, 26);
        btnVelocidad = crearBoton("Vel.", COLOR_BTN_NORMAL, 870, 7, 70, 26);
        btnProcesos = crearBoton("Procesos", COLOR_BTN_NORMAL, 990, 7, 90, 26);
        btnDesfrag = crearBoton("Desfrag", COLOR_BTN_NORMAL, 1085, 7, 100, 26);

        panelControl.add(btnIniciar);
        panelControl.add(btnTerminar);
        panelControl.add(btnDetener);
        panelControl.add(btnReanudar);
        panelControl.add(btnVelocidad);
        panelControl.add(btnProcesos);
        panelControl.add(btnDesfrag);

        txtVelocidad = crearCampoTexto("" + velocidad, 825, 7, 42);
        txtProcesos = crearCampoTexto("" + totalProcesos, 942, 7, 44);
        panelControl.add(txtVelocidad);
        panelControl.add(txtProcesos);

        panelControl.add(miniLabel("ms/frame", 822, 0));
        panelControl.add(miniLabel("proc (1-100)", 935, 0));

        cmbAlgoritmo = new JComboBox<>(NOMBRES_ALGO);
        cmbAlgoritmo.setSelectedIndex(ALGO_ROUND_ROBIN);
        cmbAlgoritmo.setBounds(100, 7, 130, 26);
        cmbAlgoritmo.setBackground(new Color(35, 38, 55));
        cmbAlgoritmo.setForeground(COLOR_TEXTO);
        cmbAlgoritmo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        cmbAlgoritmo.setFocusable(false);
        cmbAlgoritmo.addActionListener(e -> {
            algoritmoActual = cmbAlgoritmo.getSelectedIndex();
            if (timerSimulacion != null && timerSimulacion.isRunning()) {
                ordenarColaSegunAlgoritmo();
            }
        });
        panelControl.add(cmbAlgoritmo);
        panelControl.add(miniLabel("Planificación", 100, 0));

        btnTerminar.setEnabled(false);
        btnDetener.setEnabled(false);
        btnReanudar.setEnabled(false);
        btnDesfrag.setEnabled(false);

        lblRaton = crearLabelIO("Ratón", POS_BLOQ_X + 70, POS_BLOQ_Y);
        lblTeclado = crearLabelIO("Teclado", POS_BLOQ_X + 70, POS_BLOQ_Y + 16);
        lblMonitor = crearLabelIO("Monitor", POS_BLOQ_X + 70, POS_BLOQ_Y + 32);
        lblImpresora = crearLabelIO("Impresora", POS_BLOQ_X + 70, POS_BLOQ_Y + 48);
        lblBocinas = crearLabelIO("Bocinas", POS_BLOQ_X + 70, POS_BLOQ_Y + 64);

        String[] cols = {"PROCESO", "ESTADO", "TRANSICIÓN", "CUANTUM", "PRIORIDAD", "RECURSO", "MEMORIA"};
        modeloTabla = new DefaultTableModel(null, cols) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tabla = new JTable(modeloTabla);
        estilizarTabla();

        scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBounds(0, 500, 1200, 175);
        scrollTabla.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, COLOR_ACENTO));
        add(scrollTabla);

        btnIniciar.addActionListener(e -> accionIniciar());
        btnTerminar.addActionListener(e -> accionTerminar());
        btnDetener.addActionListener(e -> accionDetener());
        btnReanudar.addActionListener(e -> accionReanudar());
        btnVelocidad.addActionListener(e -> accionVelocidad());
        btnProcesos.addActionListener(e -> accionProcesos());
        btnDesfrag.addActionListener(e -> desfragmentar());
    }

    private JButton crearBoton(String texto, Color fondo, int x, int y, int w, int h) {
        JButton btn = new JButton(texto);
        btn.setBounds(x, y, w, h);
        btn.setBackground(fondo);
        btn.setForeground(COLOR_TEXTO);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            final Color original = fondo;

            public void mouseEntered(MouseEvent e) {
                btn.setBackground(original.brighter());
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(original);
            }
        });
        return btn;
    }

    private JTextField crearCampoTexto(String valor, int x, int y, int w) {
        JTextField tf = new JTextField(valor);
        tf.setBounds(x, y, w, 26);
        tf.setBackground(new Color(35, 38, 55));
        tf.setForeground(COLOR_TEXTO);
        tf.setFont(new Font("Consolas", Font.BOLD, 12));
        tf.setHorizontalAlignment(JTextField.CENTER);
        tf.setBorder(BorderFactory.createLineBorder(COLOR_ACENTO, 1));
        tf.setCaretColor(COLOR_TEXTO);
        return tf;
    }

    private JLabel miniLabel(String texto, int x, int y) {
        JLabel lbl = new JLabel(texto);
        lbl.setBounds(x, y, 90, 10);
        lbl.setForeground(COLOR_SUBTEXTO);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 8));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    private JLabel crearLabelIO(String texto, int x, int y) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(COLOR_SUBTEXTO);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setBounds(x, y + 40, 120, 16);
        add(lbl);
        return lbl;
    }

    private void estilizarTabla() {
        tabla.setBackground(new Color(20, 23, 35));
        tabla.setForeground(COLOR_TEXTO);
        tabla.setFont(new Font("Consolas", Font.PLAIN, 12));
        tabla.setRowHeight(22);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setSelectionBackground(new Color(50, 70, 100));

        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                                                           boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setForeground(COLOR_TEXTO);
                setBackground(row % 2 == 0
                        ? new Color(20, 23, 35)
                        : new Color(28, 32, 48));
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (sel) setBackground(new Color(50, 70, 100));
                return this;
            }
        });

        JTableHeader header = tabla.getTableHeader();
        header.setBackground(new Color(30, 35, 55));
        header.setForeground(COLOR_ACENTO);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_ACENTO));
    }

    private void accionIniciar() {
        crearProcesos();
        ordenarColaSegunAlgoritmo();
        iniciarTimer();
        mostrarMensaje("Simulación iniciada con " + totalProcesos + " procesos.");
        txtProcesos.setEditable(false);
        btnProcesos.setEnabled(false);
        btnIniciar.setEnabled(false);
        btnDetener.setEnabled(true);
        btnDesfrag.setEnabled(true);
        btnTerminar.setEnabled(true);
        btnReanudar.setEnabled(false);
        primeraVez = false;
    }

    private void accionDetener() {
        estadoSimulacion = 1;
        mostrarMensaje("Simulación pausada.");
        btnReanudar.setEnabled(true);
        btnDetener.setEnabled(false);
    }

    private void accionReanudar() {
        estadoSimulacion = 0;
        mostrarMensaje("Simulación reanudada.");
        btnDetener.setEnabled(true);
        btnReanudar.setEnabled(false);
    }

    private void accionVelocidad() {
        try {
            int v = Integer.parseInt(txtVelocidad.getText().trim());
            if (v < 0 || v > 500) {
                mostrarMensaje("Velocidad fuera de rango (0–500 ms).");
                return;
            }
            velocidad = v;
            if (timerSimulacion != null) timerSimulacion.setDelay((int) velocidad);
            mostrarMensaje("Velocidad ajustada a " + velocidad + " ms/frame.");
        } catch (NumberFormatException ex) {
            mostrarMensaje("Ingresa un número válido para la velocidad.");
        }
    }

    private void accionProcesos() {
        try {
            int n = Integer.parseInt(txtProcesos.getText().trim());
            if (n <= 0 || n > 100) {
                mostrarMensaje("Ingresa un valor entre 1 y 100.");
                return;
            }
            totalProcesos = n;
            mostrarMensaje("Número de procesos ajustado a " + totalProcesos + ".");
            txtProcesos.setText("" + totalProcesos);
        } catch (NumberFormatException ex) {
            mostrarMensaje("Ingresa un número válido para los procesos.");
        }
    }

    private void accionTerminar() {
        if (timerSimulacion != null) timerSimulacion.stop();
        procesos.clear();
        reiniciarOCerrar();
    }

    private void iniciarTimer() {
        if (timerSimulacion != null) timerSimulacion.stop();
        timerSimulacion = new javax.swing.Timer((int) velocidad, e -> {
            if (estadoSimulacion == 0) {
                actualizarLogica();
            }
            repaint();
        });
        timerSimulacion.start();
    }


    private void actualizarLogica() {
        try {
            for (int i = 0; i < procesos.size(); i++) {
                Proceso p = procesos.get(i);
                if (p.memoriaBytes <= 0) {
                    p.memoriaBytes = GestorMemoria.generarTamano();
                    p.memoriaKb = GestorMemoria.convertirAKb(p.memoriaBytes);
                }
                moverProceso(i);
            }

            refrescarTabla();
            flushLogBuffer();

            if (limiteRAM > 400 && advertenciaRAM == 0) {
                mostrarMensaje("RAM al límite – desbordando a memoria virtual.");
                advertenciaRAM = 1;
            }

            if (totalProcesos == 0) {
                timerSimulacion.stop();
                reiniciarOCerrar();
            }
        } catch (Exception ignored) {
        }
    }

    // ============================================================
    //  CREACIÓN DE PROCESOS
    // ============================================================
    private static void crearProcesos() {
        procesos.clear();
        for (int i = 0; i < totalProcesos; i++) {
            int cuantum = (int) (Math.random() * 15 + 1);
            int prioridad = (int) (Math.random() * 10 + 1);  // 1=alta, 10=baja
            int r = (int) (Math.random() * 255 + 1);
            int g = (int) (Math.random() * 255 + 1);
            int b = (int) (Math.random() * 255 + 1);
            Proceso nuevo = new Proceso(
                    POS_INICIAL_X, POS_INICIAL_Y,
                    i, EST_FORMADO, cuantum,
                    "" + (i + 1), r, g, b
            );
            nuevo.prioridad = prioridad;
            procesos.add(nuevo);
        }
    }

    /**
     * Reordena la cola de FORMADOS según el algoritmo activo
     */
    private static void ordenarColaSegunAlgoritmo() {
        // Separar procesos formados (cola) de los que ya están en otro estado
        ArrayList<Proceso> enCola = new ArrayList<>();
        ArrayList<Proceso> enCurso = new ArrayList<>();
        for (Proceso p : procesos) {
            if (p.estado == EST_FORMADO) enCola.add(p);
            else enCurso.add(p);
        }
        switch (algoritmoActual) {
            case ALGO_FIFO:
                // FIFO puro: orden de llegada (índice original), sin preempción
                enCola.sort(Comparator.comparingInt(p -> p.indice));
                break;
            case ALGO_LIFO:
                // LIFO puro: último en llegar ejecuta primero, sin preempción
                enCola.sort((a, b) -> Integer.compare(b.indice, a.indice));
                break;
            case ALGO_ROUND_ROBIN:
                // Round Robin: orden FIFO pero con cuantum, rota al final de cola
                enCola.sort(Comparator.comparingInt(p -> p.indice));
                break;
            case ALGO_PRIORIDAD:
                // Prioridad: menor número = mayor urgencia, con cuantum
                enCola.sort(Comparator.comparingInt(p -> p.prioridad));
                break;
        }
        procesos.clear();
        procesos.addAll(enCurso);
        procesos.addAll(enCola);
    }

    // ============================================================
    //  REINICIAR O CERRAR
    // ============================================================
    private void reiniciarOCerrar() {
        int opcion = JOptionPane.showConfirmDialog(
                null,
                "¿Deseas reiniciar la simulación?",
                "Simulación finalizada",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (opcion == JOptionPane.YES_OPTION) {
            totalProcesos = (int) (Math.random() * 100 + 1);
            procesosElim = 0;
            contadorTabla = 0;
            memoriaVirtual = 0;
            estadoSimulacion = 0;
            limiteRAM = 0;
            advertenciaRAM = 0;
            desbordaVirtual = false;
            gestorRAM.eliminarTodos();
            gestorRAM.suma = 0;
            gestorRAM.def = false;
            modeloTabla.setRowCount(0);

            txtProcesos.setText("" + totalProcesos);
            txtProcesos.setEditable(true);
            btnIniciar.setEnabled(true);
            btnProcesos.setEnabled(true);
            btnDetener.setEnabled(false);
            btnReanudar.setEnabled(false);
            btnDesfrag.setEnabled(false);
            btnTerminar.setEnabled(false);
            repaint();
        } else {
            System.exit(0);
        }
    }

    // ============================================================
    //  TABLA
    // ============================================================
    private void agregarFila(int idx) {
        Proceso p = procesos.get(idx);
        modeloTabla.insertRow(modeloTabla.getRowCount(), new Object[]{
                p.nombre, p.estadoTexto, p.transicion,
                p.cuantum, p.prioridad, p.recurso, p.memoriaKb + " Kb"
        });
    }

    private void refrescarTabla() {
        modeloTabla.setRowCount(0);
        boolean usaQuantum = (algoritmoActual == ALGO_ROUND_ROBIN || algoritmoActual == ALGO_PRIORIDAD);
        for (Proceso p : procesos) {
            modeloTabla.addRow(new Object[]{
                    p.nombre, p.estadoTexto, p.transicion,
                    usaQuantum ? p.cuantum : "N/A",
                    p.prioridad, p.recurso, p.memoriaKb + " Kb"
            });
        }
    }

    // ── Centros reales de cada estado (centro del óvalo 40×40 dibujado en ny+40)
    //    La bolita se dibuja desde (p.x, p.y), radio=RADIO, para centrarla restar RADIO/2
    private static final int H = RADIO / 2; // offset de centrado = 7

    private static final int CX_LISTO = POS_LISTO_X + CENTRO_ESTADO - H;
    private static final int CY_LISTO = POS_LISTO_Y + 40 + CENTRO_ESTADO - H;
    private static final int CX_EJEC = POS_EJEC_X + CENTRO_ESTADO - H;
    private static final int CY_EJEC = POS_EJEC_Y + 40 + CENTRO_ESTADO - H;
    private static final int CX_BLOQ = POS_BLOQ_X + CENTRO_ESTADO - H;
    private static final int CY_BLOQ = POS_BLOQ_Y + 40 + CENTRO_ESTADO - H;
    private static final int CX_TERM = POS_TERM_X + CENTRO_ESTADO - H;
    private static final int CY_TERM = POS_TERM_Y + 40 + CENTRO_ESTADO - H;
    private static final int CX_SUSL = POS_SUSL_X + CENTRO_ESTADO - H;
    private static final int CY_SUSL = POS_SUSL_Y + 40 + CENTRO_ESTADO - H;
    private static final int CX_SUSB = POS_SUSB_X + CENTRO_ESTADO - H;
    private static final int CY_SUSB = POS_SUSB_Y + 40 + CENTRO_ESTADO - H;

    // ============================================================
    //  MOTOR DE MOVIMIENTO
    // ============================================================
    private void moverProceso(int j) {
        Proceso p = procesos.get(j);

        if (p.estado == EST_FORMADO) {
            boolean puedeMover = (j == 0)
                    || (procesos.get(j - 1).estado >= EST_LISTO);
            if (puedeMover) {
                p.moverHacia(CX_LISTO, CY_LISTO);
            }
        }

        if (p.x == CX_LISTO && p.y == CY_LISTO
                && p.estado == EST_FORMADO) {
            p.estado = EST_LISTO;
            p.estadoTexto = "Listo";
            p.transicion = "Despachado";
            p.recurso = "—";

            limiteRAM = gestorRAM.totalOcupado();
            boolean hayRAM = (limiteRAM <= 400);

            if (p.posicionRAM == -1 && hayRAM && p.memoriaVirtual == 0) {
                gestorRAM.agregar(p.memoriaBytes);
                p.posicionRAM = gestorRAM.ultimaDireccion;
            } else if (p.posicionRAM == -1 && !hayRAM) {
                p.memoriaVirtual = p.memoriaKb;
                memoriaVirtual += p.memoriaVirtual;
            }
            if (p.posicionTabla == -1) {
                p.posicionTabla = contadorTabla++;
                agregarFila(j);
            }
            registrarLogBuffered(j);
        }

        ejecutar(j);
        bloquear(j);
        formarDenuevo(j);
        eliminar(j);
        suspendidoListo(j);
        suspendidoBloqueado(j);
    }

    // ── EJECUCIÓN ───
    private void ejecutar(int j) {
        Proceso p = procesos.get(j);
        if (p.estado != EST_LISTO) return;

        p.moverHacia(CX_EJEC, CY_EJEC);

        if (p.x == CX_EJEC && p.y == CY_EJEC) {

            boolean usaQuantum = (algoritmoActual == ALGO_ROUND_ROBIN
                    || algoritmoActual == ALGO_PRIORIDAD);

            if (usaQuantum) {
                // ── Round Robin / Prioridad: descontar cuantum, puede ser interrumpido ──
                int dado = (int) (Math.random() * 100 + 1);
                if (dado >= 50) {
                    p.estado = EST_BLOQUEADO;
                    asignarRecursoIO(p);
                    p.estadoTexto = "Ejecución";
                    p.transicion = "Bloqueado";
                } else {
                    p.estado = EST_VUELVE;
                    p.recurso = "Op. matemática";
                    p.estadoTexto = "Ejecución";
                    p.transicion = "Vuelve a cola";
                }
                p.cuantum -= 5;
                if (p.cuantum <= 0) {
                    liberarRecursoIO();
                    p.recurso = "—";
                    p.estado = EST_TERMINADO;
                    p.cuantum = 0;
                    p.estadoTexto = "Ejecución";
                    p.transicion = "Terminado";
                }
            } else {
                // ── FIFO / LIFO: sin preempción, corre hasta terminar o bloquearse ──
                int dado = (int) (Math.random() * 100 + 1);
                if (dado >= 50) {
                    p.estado = EST_BLOQUEADO;
                    asignarRecursoIO(p);
                    p.estadoTexto = "Ejecución";
                    p.transicion = "Bloqueado";
                } else {
                    // No cede CPU — termina directo
                    liberarRecursoIO();
                    p.recurso = "—";
                    p.estado = EST_TERMINADO;
                    p.cuantum = 0;
                    p.estadoTexto = "Ejecución";
                    p.transicion = "Terminado (sin preempción)";
                }
                // El cuantum no se descuenta en FIFO/LIFO (no aplica)
            }
            registrarLogBuffered(j);
        }
    }

    // ── BLOQUEADO ────
    private void bloquear(int j) {
        Proceso p = procesos.get(j);
        if (p.estado != EST_BLOQUEADO) return;

        p.moverHacia(CX_BLOQ, CY_BLOQ);

        if (p.x == CX_BLOQ && p.y == CY_BLOQ) {
            liberarRecursoIO();
            p.recurso = "—";

            int liberar = (int) (Math.random() * 100 + 1);
            int sb = (int) (Math.random() * 100 + 1);
            p.estadoTexto = "Bloqueado";
            registrarLogBuffered(j);

            if (liberar > 50) {
                if (sb > 50) {
                    p.estado = EST_SUS_BLOQUEADO;
                    p.transicion = "Suspendido-bloqueado";
                } else {
                    p.estado = EST_VUELVE;
                    p.transicion = "Despertar";
                }
            }
            registrarLogBuffered(j);
        }
    }

    // ── VUELVE AL INICIO ────
    // En Round Robin y Prioridad: el proceso va al final de la cola (preempción).
    // En FIFO / LIFO: sólo puede llegar aquí si viene de BLOQUEADO (despertar),
    // se reinserta y se reordena según el algoritmo activo.
    private void formarDenuevo(int j) {
        Proceso p = procesos.get(j);
        if (p.estado != EST_VUELVE) return;

        p.moverHacia(POS_INICIAL_X, POS_INICIAL_Y);

        if (p.x == POS_INICIAL_X && p.y == POS_INICIAL_Y) {
            p.recurso = "—";
            p.estado = EST_FORMADO;
            p.estadoTexto = "Formado";
            p.transicion = "En cola";
            // Mover al final de la lista y reordenar
            procesos.remove(j);
            procesos.add(p);
            ordenarColaSegunAlgoritmo();
        }
    }

    // ── TERMINADO ────
    private void eliminar(int j) {
        Proceso p = procesos.get(j);
        if (p.estado != EST_TERMINADO) return;

        p.moverHacia(CX_TERM, CY_TERM);

        if (p.x == CX_TERM && p.y == CY_TERM) {
            totalProcesos--;
            procesosElim++;
            registrarLogBuffered(j);

            if (p.memoriaVirtual == 0) {
                if (gestorRAM.def) {
                    gestorRAM.quitarMemoria(p.memoriaBytes);
                    desfragmentar();
                } else {
                    gestorRAM.quitarSlot(p.posicionRAM);
                    gestorRAM.quitarMemoria(p.memoriaBytes);
                }
            }
            memoriaVirtual -= p.memoriaVirtual;
            procesos.remove(j);
        }
    }

    // ── SUSPENDIDO-LISTO ────
    private void suspendidoListo(int j) {
        Proceso p = procesos.get(j);
        if (p.estado != EST_SUS_LISTO) return;

        p.moverHacia(CX_SUSL, CY_SUSL);

        if (p.x == CX_SUSL && p.y == CY_SUSL) {
            p.estado = EST_VUELVE;
            p.estadoTexto = "Suspendido-Listo";
            p.transicion = "Formado";
            registrarLogBuffered(j);
        }
    }

    // ── SUSPENDIDO-BLOQUEADO ───
    private void suspendidoBloqueado(int j) {
        Proceso p = procesos.get(j);
        if (p.estado != EST_SUS_BLOQUEADO) return;

        p.moverHacia(CX_SUSB, CY_SUSB);

        if (p.x == CX_SUSB && p.y == CY_SUSB) {
            int liberar = (int) (Math.random() * 100 + 1);
            p.estadoTexto = "Suspendido-bloqueado";
            registrarLogBuffered(j);
            if (liberar < 50) {
                p.estado = EST_SUS_LISTO;
                p.transicion = "Suspendido-listo";
            } else {
                p.estado = EST_BLOQUEADO;
                p.transicion = "Bloqueado";
            }
            registrarLogBuffered(j);
        }
    }

    // ============================================================
    //  GESTIÓN DE RECURSOS I/O
    // ============================================================
    private static final String[] NOMBRES_RECURSOS = {"Ratón", "Teclado", "Monitor", "Impresora", "Bocinas"};

    private void asignarRecursoIO(Proceso p) {
        int idx = (int) (Math.random() * 5);
        p.recurso = NOMBRES_RECURSOS[idx];
        Color c = new Color(p.r, p.g, p.b);
        switch (idx) {
            case 0:
                lblRaton.setForeground(c);
                break;
            case 1:
                lblTeclado.setForeground(c);
                break;
            case 2:
                lblMonitor.setForeground(c);
                break;
            case 3:
                lblImpresora.setForeground(c);
                break;
            case 4:
                lblBocinas.setForeground(c);
                break;
        }
    }

    private void liberarRecursoIO() {
        lblRaton.setForeground(COLOR_SUBTEXTO);
        lblTeclado.setForeground(COLOR_SUBTEXTO);
        lblMonitor.setForeground(COLOR_SUBTEXTO);
        lblImpresora.setForeground(COLOR_SUBTEXTO);
        lblBocinas.setForeground(COLOR_SUBTEXTO);
    }

    // ============================================================
    //  DESFRAGMENTACIÓN DE MEMORIA
    // ============================================================
    private void desfragmentar() {
        gestorRAM.eliminarTodos();
        gestorRAM.def = true;
        gestorRAM.p = 0;
        int restante = gestorRAM.suma;
        gestorRAM.suma = 0;
        while (restante > 512) {
            restante -= 512;
            gestorRAM.agregar(512);
        }
        gestorRAM.agregar(restante);
        repaint();
    }

    // ============================================================
    //  PINTADO
    // ============================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        dibujarEscenario(g2);

        for (int i = 0; i < procesos.size(); i++) {
            Proceso p = procesos.get(i);
            dibujarProceso(g2, p, new Color(p.r, p.g, p.b));
        }
    }

    private void dibujarProceso(Graphics2D g2, Proceso p, Color color) {
        // Sombra/glow
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
        g2.fillOval(p.x - 3, p.y - 3, RADIO + 8, RADIO + 8);
        // Relleno
        g2.setColor(color);
        g2.fillOval(p.x, p.y, RADIO, RADIO);
        // Borde blanco
        g2.setColor(new Color(255, 255, 255, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(p.x, p.y, RADIO, RADIO);
        // Nombre
        g2.setColor(COLOR_TEXTO);
        g2.setFont(new Font("Consolas", Font.BOLD, 9));
        g2.drawString(p.nombre, p.x + 2, p.y - 2);
    }

    // ============================================================
    //  ESCENARIO: círculos de estado y etiquetas
    // ============================================================
    private void dibujarEscenario(Graphics2D g2) {
        g2.setColor(new Color(50, 60, 85));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(450, 500, 450, 40);

        int[][] nodos = {
                {POS_LISTO_X, POS_LISTO_Y},
                {POS_EJEC_X, POS_EJEC_Y},
                {POS_BLOQ_X, POS_BLOQ_Y},
                {POS_TERM_X, POS_TERM_Y},
                {POS_SUSL_X, POS_SUSL_Y},
                {POS_SUSB_X, POS_SUSB_Y}
        };
        String[] etiquetas = {"LISTO", "EJECUCIÓN", "BLOQUEADO", "TERMINADO", "SUS-LISTO", "SUS-BLOQUEADO"};

        for (int i = 0; i < nodos.length; i++) {
            int nx = nodos[i][0], ny = nodos[i][1] + 40;
            g2.setColor(new Color(72, 199, 142, 40));
            g2.fillOval(nx - 5, ny - 5, 50, 50);
            g2.setColor(COLOR_ESTADO);
            g2.fillOval(nx, ny, 40, 40);
            g2.setColor(COLOR_ESTADO.brighter());
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(nx, ny, 40, 40);
            g2.setColor(COLOR_TEXTO);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.drawString(etiquetas[i], nx - 5, ny + 54);
        }

        dibujarStats(g2);
        gestorRAM.dibujar(g2);
    }

    private void dibujarStats(Graphics2D g2) {
        int px = 460, py = 60;
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(COLOR_ACENTO);
        g2.drawString("ESTADÍSTICAS", px, py);

        g2.setFont(new Font("Consolas", Font.PLAIN, 12));
        g2.setColor(COLOR_SUBTEXTO);
        g2.drawString("Procesos activos:  ", px, py + 24);
        g2.setColor(COLOR_TEXTO);
        g2.drawString("" + totalProcesos, px + 130, py + 24);

        g2.setColor(COLOR_SUBTEXTO);
        g2.drawString("Procesos terminados:", px, py + 42);
        g2.setColor(COLOR_TEXTO);
        g2.drawString("" + procesosElim, px + 140, py + 42);

        g2.setColor(COLOR_SUBTEXTO);
        g2.drawString("Mem. virtual en uso:", px, py + 60);
        g2.setColor(memoriaVirtual > 0 ? new Color(251, 130, 100) : COLOR_TEXTO);
        g2.drawString(memoriaVirtual + " Mb", px + 140, py + 60);

        g2.setColor(COLOR_SUBTEXTO);
        g2.drawString("Velocidad actual:   ", px, py + 78);
        g2.setColor(COLOR_TEXTO);
        g2.drawString(velocidad + " ms/frame", px + 130, py + 78);

        g2.setColor(COLOR_SUBTEXTO);
        g2.drawString("Algoritmo:          ", px, py + 96);
        g2.setColor(COLOR_ACENTO);
        g2.drawString(NOMBRES_ALGO[algoritmoActual], px + 130, py + 96);

        boolean usaQ = (algoritmoActual == ALGO_ROUND_ROBIN || algoritmoActual == ALGO_PRIORIDAD);
        g2.setColor(COLOR_SUBTEXTO);
        g2.drawString("Cuantum:            ", px, py + 114);
        g2.setColor(usaQ ? COLOR_ESTADO : new Color(180, 80, 80));
        g2.drawString(usaQ ? "Activo (1-15)" : "No aplica (sin preempción)", px + 130, py + 114);
    }

    // ============================================================
    //  LOG CON BUFFER
    // ============================================================
    private void inicializarLog() {
        gestionarRespaldo();
        bufferLog.append("═".repeat(90)).append("\n");
        bufferLog.append("  SIMULACIÓN SO  –  ").append(fechaHora()).append("\n");
        bufferLog.append("═".repeat(90)).append("\n");
        bufferLog.append(String.format("%-20s %-10s %-22s %-22s %-10s %-15s %-10s\n",
                "PROCESO", "CUANTUM", "ESTADO", "TRANSICIÓN", "PRIORIDAD", "RECURSO", "MEMORIA"));
        bufferLog.append("─".repeat(100)).append("\n");
        flushLogBuffer();
    }

    private void registrarLogBuffered(int j) {
        if (j >= procesos.size()) return;
        Proceso p = procesos.get(j);
        bufferLog.append(String.format("%-20s %-10d %-22s %-22s %-10d %-15s %-10s\n",
                p.nombre, p.cuantum, p.estadoTexto, p.transicion,
                p.prioridad, p.recurso, p.memoriaKb + " Kb"));
    }

    private void flushLogBuffer() {
        if (bufferLog.length() == 0) return;
        try (FileWriter fw = new FileWriter(archivoLog, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.print(bufferLog.toString());
            bufferLog.setLength(0);
        } catch (IOException e) {
            System.err.println("Error escribiendo log: " + e.getMessage());
        }
    }

    private void gestionarRespaldo() {
        if (!archivoLog.exists()) {
            escribirContador("0");
            return;
        }
        int num = leerContador();
        num++;
        escribirContador("" + num);
        try {
            File respaldo = new File("SimulacionLog_Respaldo_" + num + ".txt");
            try (FileChannel src = new FileInputStream(archivoLog).getChannel();
                 FileChannel dest = new FileOutputStream(respaldo).getChannel()) {
                src.transferTo(0, archivoLog.length(), dest);
            }
            new FileOutputStream(archivoLog).close();
        } catch (IOException e) {
            System.err.println("Error al respaldar: " + e.getMessage());
        }
    }

    private int leerContador() {
        try (Scanner sc = new Scanner(archivoContador)) {
            if (sc.hasNextLine()) return Integer.parseInt(sc.nextLine().trim());
        } catch (Exception ignored) {
        }
        return 0;
    }

    private void escribirContador(String valor) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivoContador))) {
            pw.println(valor);
        } catch (IOException ignored) {
        }
    }

    private String fechaHora() {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
    }

    private void mostrarMensaje(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Simulación SO", JOptionPane.INFORMATION_MESSAGE);
    }

}


