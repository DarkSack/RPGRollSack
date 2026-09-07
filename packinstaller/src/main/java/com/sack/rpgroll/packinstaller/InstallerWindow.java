package com.sack.rpgroll.packinstaller;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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

    private final JFrame ventana = new JFrame("Instalador de packs · RPGRoll");
    private final JTextField campoPack = new JTextField();
    private final JTextField campoPlugins = new JTextField();
    private final JEditorPane informe = new JEditorPane();
    private final Estilo.Boton botonRevisar = new Estilo.Boton("Revisar", false);
    private final Estilo.Boton botonInstalar = new Estilo.Boton("Instalar", true);
    private final JLabel estado = new JLabel(" ");

    private Plan planActual;

    /**
     * El pack con el que se construyó {@link #planActual}.
     *
     * <p>Se instala desde acá y no releyendo el campo de texto: si no, escribir
     * otra ruta después de revisar instalaría algo que nadie ha visto.
     */
    private Path packRevisado;

    static void mostrar() {
        SwingUtilities.invokeLater(() -> new InstallerWindow().construir().setVisible(true));
    }

    /**
     * Monta la ventana, sin mostrarla.
     *
     * <p>Quien la muestra es {@link #mostrar()}. Separarlo permite además
     * comprobar en una prueba que se construye entera sin reventar, que es el
     * fallo de interfaz más caro: se ve al abrir, no al compilar.
     */
    JFrame construir() {
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setMinimumSize(new Dimension(780, 600));

        JPanel raiz = new JPanel(new BorderLayout(0, 14));
        raiz.setBorder(BorderFactory.createEmptyBorder(20, 20, 18, 20));
        raiz.setBackground(Estilo.FONDO);

        raiz.add(cabecera(), BorderLayout.NORTH);
        raiz.add(centro(), BorderLayout.CENTER);
        raiz.add(pie(), BorderLayout.SOUTH);

        // Elegir con el botón ya invalida el plan, pero escribir la ruta a mano
        // también tiene que hacerlo: si no, revisar un pack y teclear otro
        // instalaría algo que nadie ha visto.
        vigilar(campoPack);
        vigilar(campoPlugins);

        permitirSoltarArchivos(raiz);

        ventana.setContentPane(raiz);
        ventana.pack();
        ventana.setSize(860, 660);
        ventana.setLocationRelativeTo(null);

        return ventana;
    }

    // ------------------------------------------------------------------
    // Montaje
    // ------------------------------------------------------------------

    private JPanel cabecera() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(Estilo.FONDO);

        JPanel titulos = new JPanel();
        titulos.setLayout(new javax.swing.BoxLayout(titulos, javax.swing.BoxLayout.Y_AXIS));
        titulos.setBackground(Estilo.FONDO);

        JLabel titulo = new JLabel("Instalador de packs");
        titulo.setFont(Estilo.fuente(Font.BOLD, 21));
        titulo.setForeground(Estilo.TEXTO);
        titulo.setAlignmentX(0f);
        // Unos píxeles de aire: el ancho preferido de un JLabel queda justo al
        // píxel y la última letra se corta según cómo se dibuje el texto.
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        JLabel bajada = new JLabel(
                "Reparte el contenido en los plugins que tengas instalados. El resto se omite.");
        bajada.setFont(Estilo.fuente(Font.PLAIN, 12));
        bajada.setForeground(Estilo.TENUE);
        bajada.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 8));
        bajada.setAlignmentX(0f);

        titulos.add(titulo);
        titulos.add(bajada);

        panel.add(titulos, BorderLayout.NORTH);
        panel.add(rutas(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel rutas() {
        Estilo.Tarjeta tarjeta = new Estilo.Tarjeta(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 0, 5, 10);
        c.anchor = GridBagConstraints.WEST;

        fila(tarjeta, c, 0, "Pack", campoPack, this::elegirPack);
        fila(tarjeta, c, 1, "Carpeta plugins", campoPlugins, this::elegirPlugins);

        JLabel pista = new JLabel("Arrastra aquí el .zip del pack, o púlsalo en Elegir");
        pista.setFont(Estilo.fuente(Font.PLAIN, 11));
        pista.setForeground(Estilo.TENUE);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 3;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 0, 0);
        tarjeta.add(pista, c);

        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setBackground(Estilo.FONDO);
        envoltorio.add(tarjeta, BorderLayout.CENTER);

        return envoltorio;
    }

    private void fila(JPanel panel, GridBagConstraints c, int y, String etiqueta,
                      JTextField campo, Runnable accion) {
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;

        JLabel titulo = new JLabel(etiqueta);
        titulo.setFont(Estilo.fuente(Font.PLAIN, 12));
        titulo.setForeground(Estilo.TENUE);
        titulo.setPreferredSize(new Dimension(110, 20));
        panel.add(titulo, c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        estilizar(campo);
        panel.add(campo, c);

        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;

        Estilo.Boton boton = new Estilo.Boton("Elegir", false);
        boton.addActionListener(evento -> accion.run());
        panel.add(boton, c);
    }

    private JPanel centro() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Estilo.FONDO);

        JLabel titulo = new JLabel("QUÉ VA A PASAR");
        titulo.setFont(Estilo.fuente(Font.BOLD, 10));
        titulo.setForeground(Estilo.TENUE);
        panel.add(titulo, BorderLayout.NORTH);

        informe.setEditable(false);
        informe.setContentType("text/html");
        informe.setFont(Estilo.fuente(Font.PLAIN, 12));
        informe.setBackground(Estilo.PANEL);
        informe.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        informe.setText(Informe.bienvenida());

        JScrollPane scroll = new JScrollPane(informe);
        Estilo.oscurecerBarras(scroll);

        Estilo.Tarjeta tarjeta = new Estilo.Tarjeta(new BorderLayout());
        tarjeta.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        tarjeta.add(scroll, BorderLayout.CENTER);

        panel.add(tarjeta, BorderLayout.CENTER);

        return panel;
    }

    private JPanel pie() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Estilo.FONDO);

        estado.setFont(Estilo.fuente(Font.PLAIN, 12));
        estado.setForeground(Estilo.TENUE);
        panel.add(estado, BorderLayout.WEST);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botones.setBackground(Estilo.FONDO);

        botonRevisar.addActionListener(evento -> revisar());

        botonInstalar.setEnabled(false);
        botonInstalar.addActionListener(evento -> instalar());

        botones.add(botonRevisar);
        botones.add(botonInstalar);
        panel.add(botones, BorderLayout.EAST);

        return panel;
    }

    private static void estilizar(JTextField campo) {
        campo.setFont(Estilo.monoespaciada(12));
        campo.setBackground(Estilo.PANEL_2);
        campo.setForeground(Estilo.TEXTO);
        campo.setCaretColor(Estilo.ACENTO);
        campo.setSelectionColor(Estilo.BORDE);
        campo.setSelectedTextColor(Estilo.TEXTO);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        campo.setPreferredSize(new Dimension(400, 34));
    }

    private void vigilar(JTextField campo) {
        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent evento) {
                invalidarPlan();
            }

            @Override
            public void removeUpdate(DocumentEvent evento) {
                invalidarPlan();
            }

            @Override
            public void changedUpdate(DocumentEvent evento) {
                invalidarPlan();
            }
        });
    }

    /**
     * Soltar archivos sobre la ventana.
     *
     * <p>Es el gesto natural con un zip recién descargado, y ahorra el diálogo
     * de archivos entero. Se decide por lo que se suelta: una carpeta llamada
     * {@code plugins} es el destino; cualquier otra cosa, el pack.
     */
    private void permitirSoltarArchivos(JPanel raiz) {
        raiz.setTransferHandler(new TransferHandler() {

            @Override
            public boolean canImport(TransferSupport soporte) {
                return soporte.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport soporte) {
                if (!canImport(soporte)) {
                    return false;
                }

                try {
                    Object datos = soporte.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);

                    if (!(datos instanceof List<?> lista) || lista.isEmpty()) {
                        return false;
                    }

                    for (Object elemento : lista) {
                        if (elemento instanceof File archivo) {
                            colocar(archivo);
                        }
                    }

                    return true;
                } catch (Exception error) {
                    return false;
                }
            }

            private void colocar(File archivo) {
                boolean esCarpetaPlugins = archivo.isDirectory()
                        && archivo.getName().equalsIgnoreCase("plugins");

                if (esCarpetaPlugins) {
                    campoPlugins.setText(archivo.getAbsolutePath());
                } else {
                    campoPack.setText(archivo.getAbsolutePath());
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // Acciones
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
        }
    }

    private void elegirPlugins() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Elige la carpeta 'plugins' del servidor");
        selector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (selector.showOpenDialog(ventana) == JFileChooser.APPROVE_OPTION) {
            campoPlugins.setText(selector.getSelectedFile().getAbsolutePath());
        }
    }

    /** Cambiar cualquier ruta invalida el plan: instalar uno viejo sería mentir. */
    private void invalidarPlan() {
        planActual = null;
        packRevisado = null;
        botonInstalar.setEnabled(false);
    }

    private void revisar() {
        invalidarPlan();

        Path origen = rutaDe(campoPack, "Falta elegir el pack.");
        Path plugins = rutaDe(campoPlugins, "Falta elegir la carpeta 'plugins'.");

        if (origen == null || plugins == null) {
            return;
        }

        try (PackSource pack = PackSource.desde(origen)) {
            Plan plan = new Planner(pack, plugins).planificar();

            mostrarHtml(Informe.delPlan(plan));

            planActual = plan;
            packRevisado = origen;

            boolean hayTrabajo = !plan.aInstalar().isEmpty();
            botonInstalar.setEnabled(!plan.tieneErrores() && hayTrabajo);

            if (plan.tieneErrores()) {
                anunciar(Estilo.MAL, "Hay errores que resolver.");
            } else if (!hayTrabajo) {
                anunciar(Estilo.TENUE, "Todo está ya en su sitio.");
            } else {
                anunciar(Estilo.ACENTO, Formato.archivos(plan.aInstalar().size()) + " por instalar.");
            }
        } catch (IOException error) {
            mostrarHtml(Informe.error(String.valueOf(error.getMessage())));
            anunciar(Estilo.MAL, "No se pudo leer el pack.");
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
        Path origen = packRevisado;

        if (plan == null || origen == null) {
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
        anunciar(Estilo.TENUE, "Instalando…");

        int total = plan.aInstalar().size();

        // En un hilo aparte: copiando desde el hilo de la interfaz, la ventana
        // se congela y Windows la marca como "no responde" a mitad del trabajo.
        new SwingWorker<Installer.Resultado, String>() {

            private int hechos;

            @Override
            protected Installer.Resultado doInBackground() throws IOException {
                try (PackSource pack = PackSource.desde(origen)) {
                    return new Installer(pack).instalar(plan, this::publish);
                }
            }

            @Override
            protected void process(List<String> copiados) {
                // Se cuenta por lote y se pinta una sola vez: con 116 archivos,
                // repintar por cada uno cuesta más que copiarlos.
                hechos += copiados.size();
                anunciar(Estilo.TENUE, "Instalando… " + hechos + " de " + total);
            }

            @Override
            protected void done() {
                botonRevisar.setEnabled(true);

                try {
                    mostrarResultado(get());
                } catch (Exception error) {
                    mostrarHtml(Informe.error(String.valueOf(error.getMessage())));
                    anunciar(Estilo.MAL, "Falló.");
                }
            }
        }.execute();
    }

    private void mostrarResultado(Installer.Resultado resultado) {
        mostrarHtml(Informe.resultado(resultado));

        if (resultado.todoBien()) {
            anunciar(Estilo.BIEN, "Listo.");
        } else {
            anunciar(Estilo.MAL, "Terminó con fallos.");
        }

        // No se vuelve a habilitar Instalar: el plan ya se aplicó y repetirlo
        // sin revisar de nuevo solo puede confundir sobre qué queda por hacer.
        planActual = null;
        packRevisado = null;
    }

    private void mostrarHtml(String html) {
        informe.setText(html);
        informe.setCaretPosition(0);
    }

    private void anunciar(java.awt.Color tinta, String texto) {
        estado.setForeground(tinta);
        estado.setText(texto);
    }
}
