package com.sack.rpgroll.packinstaller;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

/**
 * La ventana.
 *
 * <p>Un flujo de dos pasos y en este orden: primero <b>Revisar</b>, que no toca
 * nada y explica lo que pasaría, y solo entonces se habilita <b>Instalar</b>.
 *
 * <p>Es deliberado que no haya un botón único de "instalar y ya". Esto escribe
 * en la carpeta de un servidor que puede llevar meses funcionando, y la persona
 * que lo usa casi nunca sabe qué archivos toca cada pack. Ver la lista antes es
 * la diferencia entre una herramienta y una apuesta.
 */
final class InstallerWindow {

    private static final Color FONDO = new Color(0x0E, 0x11, 0x16);
    private static final Color PANEL = new Color(0x16, 0x1B, 0x22);
    private static final Color BORDE = new Color(0x26, 0x2D, 0x36);
    private static final Color TEXTO = new Color(0xE6, 0xED, 0xF3);
    private static final Color TENUE = new Color(0x8B, 0x94, 0x9E);
    private static final Color ACENTO = new Color(0x54, 0xDA, 0xF4);
    private static final Color MALO = new Color(0xF8, 0x51, 0x49);

    private final JFrame ventana = new JFrame("Instalador de packs · RPGRoll");
    private final JTextField campoPack = new JTextField();
    private final JTextField campoPlugins = new JTextField();
    private final JTextArea informe = new JTextArea();
    private final JButton botonRevisar = new JButton("Revisar");
    private final JButton botonInstalar = new JButton("Instalar");
    private final JLabel estado = new JLabel(" ");

    private Plan planActual;

    static void mostrar() {
        SwingUtilities.invokeLater(() -> new InstallerWindow().construir().setVisible(true));
    }

    /**
     * Monta la ventana sin mostrarla.
     *
     * <p>Separado de {@link #mostrar()} para poder dibujarla en una imagen y
     * revisar el aspecto sin abrir nada en el escritorio de nadie.
     */
    JFrame construir() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorado) {
            // El aspecto por defecto sirve igual; no vale la pena no abrir por esto.
        }

        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setMinimumSize(new Dimension(720, 560));

        JPanel raiz = new JPanel(new BorderLayout(0, 12));
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        raiz.setBackground(FONDO);

        raiz.add(cabecera(), BorderLayout.NORTH);
        raiz.add(centro(), BorderLayout.CENTER);
        raiz.add(pie(), BorderLayout.SOUTH);

        ventana.setContentPane(raiz);
        ventana.pack();
        ventana.setLocationRelativeTo(null);

        return ventana;
    }

    private JPanel cabecera() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(FONDO);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 0, 4, 8);
        c.anchor = GridBagConstraints.WEST;

        fila(panel, c, 0, "Pack", campoPack, "Elegir…", this::elegirPack);
        fila(panel, c, 1, "Carpeta plugins", campoPlugins, "Elegir…", this::elegirPlugins);

        return panel;
    }

    private void fila(JPanel panel, GridBagConstraints c, int y, String etiqueta,
                      JTextField campo, String textoBoton, Runnable accion) {
        c.gridx = 0;
        c.gridy = y;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        JLabel titulo = new JLabel(etiqueta);
        titulo.setForeground(TENUE);
        panel.add(titulo, c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        estilizar(campo);
        panel.add(campo, c);

        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        JButton boton = new JButton(textoBoton);
        boton.addActionListener(evento -> accion.run());
        panel.add(boton, c);
    }

    private JPanel centro() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(FONDO);

        JLabel titulo = new JLabel("Qué va a pasar");
        titulo.setForeground(TENUE);
        panel.add(titulo, BorderLayout.NORTH);

        informe.setEditable(false);
        informe.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        informe.setBackground(PANEL);
        informe.setForeground(TEXTO);
        informe.setCaretColor(TEXTO);
        informe.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        informe.setText("Elige un pack y la carpeta 'plugins' del servidor, y pulsa Revisar.\n\n"
                + "Revisar no toca nada: solo mira y cuenta.");

        JScrollPane scroll = new JScrollPane(informe);
        scroll.setBorder(BorderFactory.createLineBorder(BORDE));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel pie() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(FONDO);

        estado.setForeground(TENUE);
        panel.add(estado, BorderLayout.WEST);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setBackground(FONDO);

        botonRevisar.addActionListener(evento -> revisar());

        botonInstalar.setEnabled(false);
        botonInstalar.addActionListener(evento -> instalar());

        botones.add(botonRevisar);
        botones.add(botonInstalar);
        panel.add(botones, BorderLayout.EAST);

        return panel;
    }

    private static void estilizar(JTextField campo) {
        campo.setBackground(PANEL);
        campo.setForeground(TEXTO);
        campo.setCaretColor(TEXTO);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        campo.setPreferredSize(new Dimension(360, 30));
    }

    /** Rellena la ventana con un caso de ejemplo, solo para el retrato. */
    void rellenarParaRetrato(Path pack, Path plugins, String informeDeEjemplo) {
        campoPack.setText(pack.toString());
        campoPlugins.setText(plugins.toString());
        informe.setText(informeDeEjemplo);
        botonInstalar.setEnabled(true);
        estado.setForeground(ACENTO);
        estado.setText("15 archivos por instalar.");
    }

    // ------------------------------------------------------------------

    private void elegirPack() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Elige el pack (carpeta o .zip)");

        // Los dos formatos en el mismo diálogo: obligar a saber de antemano si
        // el pack está comprimido es una pregunta que el programa puede
        // responder solo mirando lo que le den.
        selector.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        selector.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File archivo) {
                return archivo.isDirectory() || archivo.getName().toLowerCase().endsWith(".zip");
            }

            @Override
            public String getDescription() {
                return "Carpetas o packs .zip";
            }
        });

        if (selector.showOpenDialog(ventana) == JFileChooser.APPROVE_OPTION) {
            campoPack.setText(selector.getSelectedFile().getAbsolutePath());
            invalidarPlan();
        }
    }

    private void elegirPlugins() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Elige la carpeta 'plugins' del servidor");
        selector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (selector.showOpenDialog(ventana) == JFileChooser.APPROVE_OPTION) {
            campoPlugins.setText(selector.getSelectedFile().getAbsolutePath());
            invalidarPlan();
        }
    }

    /** Cambiar cualquier ruta invalida el plan: instalar uno viejo sería mentir. */
    private void invalidarPlan() {
        planActual = null;
        botonInstalar.setEnabled(false);
    }

    private void revisar() {
        invalidarPlan();

        Path origen = rutaDe(campoPack, "Falta elegir el pack.");
        Path plugins = rutaDe(campoPlugins, "Falta elegir la carpeta 'plugins'.");

        if (origen == null || plugins == null) {
            return;
        }

        estado.setForeground(TENUE);
        estado.setText("Revisando…");

        try (PackSource pack = PackSource.desde(origen)) {
            Plan plan = new Planner(pack, plugins).planificar();
            informe.setText(describir(plan));
            informe.setCaretPosition(0);

            planActual = plan;

            boolean hayTrabajo = !plan.aInstalar().isEmpty();
            botonInstalar.setEnabled(!plan.tieneErrores() && hayTrabajo);

            if (plan.tieneErrores()) {
                estado.setForeground(MALO);
                estado.setText("Hay errores que resolver.");
            } else if (!hayTrabajo) {
                estado.setForeground(TENUE);
                estado.setText("Todo está ya en su sitio.");
            } else {
                estado.setForeground(ACENTO);
                estado.setText(Formato.archivos(plan.aInstalar().size()) + " por instalar.");
            }
        } catch (IOException error) {
            informe.setText("No se pudo leer el pack:\n\n" + error.getMessage());
            estado.setForeground(MALO);
            estado.setText("No se pudo leer el pack.");
        }
    }

    private Path rutaDe(JTextField campo, String siFalta) {
        String texto = campo.getText().trim();

        if (texto.isEmpty()) {
            JOptionPane.showMessageDialog(ventana, siFalta, "Falta un dato",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }

        Path ruta = Path.of(texto);

        if (!Files.exists(ruta)) {
            JOptionPane.showMessageDialog(ventana, "No existe:\n" + ruta, "Ruta incorrecta",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }

        return ruta;
    }

    private void instalar() {
        Plan plan = planActual;

        if (plan == null) {
            return;
        }

        int reemplazos = (int) plan.aInstalar().stream()
                .filter(entrada -> entrada.accion() == Plan.Accion.REEMPLAZA)
                .count();

        String pregunta = "Se van a instalar " + Formato.archivos(plan.aInstalar().size())
                + " en:\n" + plan.carpetaPlugins() + "\n\n"
                + (reemplazos > 0
                ? Formato.archivos(reemplazos) + " ya existen y se reemplazarán.\n"
                + "Se guarda una copia de los originales antes de tocarlos.\n\n"
                : "")
                + "¿Continuar?";

        if (JOptionPane.showConfirmDialog(ventana, pregunta, "Confirmar instalación",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }

        botonRevisar.setEnabled(false);
        botonInstalar.setEnabled(false);
        estado.setForeground(TENUE);
        estado.setText("Instalando…");

        // En un hilo aparte: copiando desde el hilo de la interfaz, la ventana
        // se congela y Windows la marca como "no responde" a mitad del trabajo.
        new SwingWorker<Installer.Resultado, Void>() {
            @Override
            protected Installer.Resultado doInBackground() throws IOException {
                try (PackSource pack = PackSource.desde(Path.of(campoPack.getText().trim()))) {
                    return new Installer(pack).instalar(plan, ruta -> {
                    });
                }
            }

            @Override
            protected void done() {
                botonRevisar.setEnabled(true);

                try {
                    mostrarResultado(get());
                } catch (Exception error) {
                    informe.setText("La instalación falló:\n\n" + error.getMessage());
                    estado.setForeground(MALO);
                    estado.setText("Falló.");
                }
            }
        }.execute();
    }

    private void mostrarResultado(Installer.Resultado resultado) {
        StringBuilder texto = new StringBuilder();
        texto.append("Instalación terminada\n\n");
        texto.append("  Copiados:     ").append(resultado.copiados()).append('\n');
        texto.append("  Reemplazados: ").append(resultado.reemplazados()).append('\n');
        texto.append("  Omitidos:     ").append(resultado.omitidos()).append('\n');

        if (resultado.copiaDeSeguridad() != null) {
            texto.append("\n  Copia de los originales en:\n  ")
                    .append(resultado.copiaDeSeguridad()).append('\n');
        }

        if (!resultado.todoBien()) {
            texto.append("\nNo se pudieron copiar ")
                    .append(Formato.archivos(resultado.fallos().size())).append(":\n");
            resultado.fallos().forEach(fallo -> texto.append("  - ").append(fallo).append('\n'));

            estado.setForeground(MALO);
            estado.setText("Terminó con fallos.");
        } else {
            texto.append("\nReinicia el servidor para que los plugins lean el contenido nuevo.\n");
            estado.setForeground(ACENTO);
            estado.setText("Listo.");
        }

        informe.setText(texto.toString());
        informe.setCaretPosition(0);

        // No se vuelve a habilitar Instalar: el plan ya se aplicó y repetirlo
        // sin revisar de nuevo solo puede confundir sobre qué queda por hacer.
        planActual = null;
    }

    // ------------------------------------------------------------------

    private static String describir(Plan plan) {
        StringBuilder texto = new StringBuilder();

        texto.append("Pack:    ").append(plan.nombrePack()).append('\n');
        texto.append("Destino: ").append(plan.carpetaPlugins()).append("\n\n");

        for (Plan.Aviso aviso : plan.avisos()) {
            String prefijo = switch (aviso.nivel()) {
                case ERROR -> "ERROR  ";
                case ADVERTENCIA -> "AVISO  ";
                case NOTA -> "nota   ";
            };
            texto.append(prefijo).append(aviso.mensaje()).append('\n');
        }

        if (!plan.avisos().isEmpty()) {
            texto.append('\n');
        }

        Map<Plan.Accion, Integer> resumen = plan.resumen();

        for (Plan.Accion accion : Plan.Accion.values()) {
            Integer cuantos = resumen.get(accion);

            if (cuantos != null) {
                texto.append(String.format("  %-16s %s%n", accion.etiqueta(),
                        Formato.archivos(cuantos)));
            }
        }

        if (!plan.pluginsAfectados().isEmpty()) {
            texto.append("\nSe instala en:\n");
            plan.pluginsAfectados().forEach(plugin ->
                    texto.append("  + ").append(plugin).append('\n'));
        }

        if (!plan.pluginsOmitidos().isEmpty()) {
            texto.append("\nSe omite (no instalado en este servidor):\n");
            plan.pluginsOmitidos().forEach(plugin ->
                    texto.append("  - ").append(plugin).append('\n'));
        }

        if (!plan.aInstalar().isEmpty()) {
            texto.append("\nArchivos (").append(Formato.tamano(plan.bytesAEscribir())).append("):\n");

            for (Plan.Entrada entrada : plan.aInstalar()) {
                texto.append("  ")
                        .append(entrada.accion() == Plan.Accion.REEMPLAZA ? "~ " : "+ ")
                        .append(entrada.archivo().rutaRelativa())
                        .append('\n');
            }
        }

        return texto.toString();
    }
}
