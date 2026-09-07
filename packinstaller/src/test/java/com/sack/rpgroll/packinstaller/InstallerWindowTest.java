package com.sack.rpgroll.packinstaller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Que la ventana se monte entera sin reventar.
 *
 * <p>Es el fallo de interfaz más caro y el que ninguna otra prueba ve: una
 * excepción al construirla no aparece al compilar, solo al abrirla — y para
 * entonces ya está en manos de un comprador.
 *
 * <p>De paso deja un PNG en {@code build/ventana.png}. Mirar el resultado es la
 * única forma de encontrar lo que el compilador no puede: un texto cortado, un
 * color perdido, un botón que se salió de su sitio.
 */
class InstallerWindowTest {

    @Test
    void la_ventana_se_construye_y_se_puede_dibujar() throws Exception {
        // Sin pantalla no hay nada que montar. Se omite en vez de fallar: en un
        // servidor de integración esto no es un error del código.
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "hace falta un escritorio para montar la ventana");

        AtomicReference<JFrame> marco = new AtomicReference<>();
        AtomicReference<Exception> fallo = new AtomicReference<>();

        // Swing exige que sus componentes se creen en su propio hilo.
        SwingUtilities.invokeAndWait(() -> {
            try {
                marco.set(new InstallerWindow().construir());
            } catch (Exception error) {
                fallo.set(error);
            }
        });

        if (fallo.get() != null) {
            throw fallo.get();
        }

        JFrame ventana = marco.get();

        assertNotNull(ventana, "construir() tiene que devolver la ventana");
        assertTrue(ventana.getWidth() > 400 && ventana.getHeight() > 300,
                "la ventana quedó demasiado pequeña para leer el informe");

        AtomicReference<BufferedImage> imagen = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            ventana.addNotify();
            ventana.validate();

            BufferedImage lienzo = new BufferedImage(ventana.getWidth(), ventana.getHeight(),
                    BufferedImage.TYPE_INT_RGB);

            // printAll y no paint: paint dibuja el componente sin terminar de
            // resolver las vistas de texto con formato, y el HTML sale con las
            // palabras pegadas donde hay un cambio de estilo.
            ventana.getContentPane().printAll(lienzo.getGraphics());
            imagen.set(lienzo);

            ventana.dispose();
        });

        Path destino = Path.of("build", "ventana.png");
        Files.createDirectories(destino.getParent());

        File archivo = destino.toFile();
        assertTrue(ImageIO.write(imagen.get(), "png", archivo), "no se pudo guardar el PNG");
        assertTrue(archivo.length() > 0, "el PNG salió vacío");
    }

    /**
     * El informe de un plan, dibujado aparte.
     *
     * <p>Es la pantalla que de verdad se mira, y la que usa HTML con color. Se
     * renderiza sin la ventana para poder revisarla sin montar media interfaz.
     */
    @Test
    void el_informe_de_un_plan_se_dibuja() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "hace falta un escritorio");

        Path raiz = Files.createTempDirectory("informe");
        Path origen = raiz.resolve("reino-no-muerto");
        Path plugins = raiz.resolve("servidor/plugins");

        Files.createDirectories(plugins.resolve("RPGRoll-Mobs"));
        Files.createDirectories(plugins.resolve("RPGRoll-Magic"));
        Files.writeString(plugins.resolve("paper.jar"), "x");

        crear(origen, "LEEME.md");
        crear(origen, "RPGRoll-Mobs/mobs/bosses/liche_del_sepulcro.yml");
        crear(origen, "RPGRoll-Mobs/mobs/normal/siervo_sepulcro.yml");
        crear(origen, "RPGRoll-Magic/spells/drenar_vida.yml");
        crear(origen, "RPGRoll-Traps/traps/pinchos.yml");

        // Uno que ya existe y cambio, para que salga tambien un reemplazo.
        Path existente = plugins.resolve("RPGRoll-Magic/spells/drenar_vida.yml");
        Files.createDirectories(existente.getParent());
        Files.writeString(existente, "otra cosa");

        String html;

        try (PackSource pack = PackSource.desde(origen)) {
            html = Informe.delPlan(new Planner(pack, plugins).planificar());
        }

        AtomicReference<BufferedImage> imagen = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            JEditorPane panel = new JEditorPane();
            panel.setContentType("text/html");
            panel.setEditable(false);
            panel.setBackground(Estilo.PANEL);
            panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 16, 14, 16));
            panel.setText(html);
            panel.setSize(820, 620);
            panel.addNotify();
            panel.validate();

            BufferedImage lienzo = new BufferedImage(820, 620, BufferedImage.TYPE_INT_RGB);
            panel.printAll(lienzo.getGraphics());
            imagen.set(lienzo);
        });

        File archivo = Path.of("build", "informe.png").toFile();
        assertTrue(ImageIO.write(imagen.get(), "png", archivo));
        assertTrue(archivo.length() > 0);
    }

    private static void crear(Path raiz, String relativa) throws IOException {
        Path archivo = raiz.resolve(relativa);
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, "clave: " + relativa);
    }
}
