package com.sack.rpgroll.packinstaller;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * Colores, tipografía y los tres componentes que Swing no trae con el aspecto
 * que hace falta.
 *
 * <p>La paleta es la misma de la tienda, para que la herramienta no parezca de
 * otro producto. El aspecto del sistema pinta los botones en gris claro, que
 * sobre un fondo oscuro se ve como un error y no como una decisión, así que los
 * botones y las barras de desplazamiento se dibujan a mano.
 */
final class Estilo {

    private Estilo() {
    }

    static final Color FONDO = new Color(0x0E, 0x11, 0x16);
    static final Color PANEL = new Color(0x16, 0x1B, 0x22);
    static final Color PANEL_2 = new Color(0x1B, 0x21, 0x29);
    static final Color BORDE = new Color(0x26, 0x2D, 0x36);
    static final Color TEXTO = new Color(0xE6, 0xED, 0xF3);
    static final Color TENUE = new Color(0x8B, 0x94, 0x9E);
    static final Color ACENTO = new Color(0x54, 0xDA, 0xF4);
    static final Color ACENTO_TINTA = new Color(0x06, 0x22, 0x2B);
    static final Color BIEN = new Color(0x3F, 0xB9, 0x50);
    static final Color MAL = new Color(0xF8, 0x51, 0x49);
    static final Color AVISO = new Color(0xD2, 0x99, 0x22);

    /**
     * Fuente de interfaz.
     *
     * <p>Se busca una de las habituales de cada sistema en vez de fijar una: si
     * no está instalada, Java cae en Dialog, que en Windows acaba siendo una
     * tipografía de mapa de bits con muy mal aspecto a tamaños grandes.
     */
    static Font fuente(int estilo, int tamano) {
        return new Font(nombreDeFuente(), estilo, tamano);
    }

    static Font monoespaciada(int tamano) {
        return new Font(Font.MONOSPACED, Font.PLAIN, tamano);
    }

    private static String nombreDeFuente() {
        Set<String> disponibles = Set.of(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());

        for (String candidata : new String[] {"Segoe UI", "SF Pro Text", "Inter", "Ubuntu", "Noto Sans"}) {
            if (disponibles.contains(candidata)) {
                return candidata;
            }
        }

        return Font.SANS_SERIF;
    }

    // ------------------------------------------------------------------

    /**
     * Un botón plano, dibujado a mano.
     *
     * <p>Swing no permite cambiarle el color de fondo a un botón con el aspecto
     * nativo de Windows: lo ignora y sigue pintando el suyo. La única forma de
     * tener un botón que pegue con un fondo oscuro es pintarlo entero.
     */
    static final class Boton extends JButton {

        private final boolean principal;
        private boolean encima;

        Boton(String texto, boolean principal) {
            super(texto);
            this.principal = principal;

            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(fuente(Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent evento) {
                    encima = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent evento) {
                    encima = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            boolean activo = isEnabled();

            if (principal) {
                if (activo) {
                    g2.setColor(encima ? ACENTO.brighter() : ACENTO);
                    g2.fillRoundRect(0, 0, w, h, 8, 8);
                } else {
                    // Apagado se dibuja como un contorno, no como un bloque
                    // relleno: un bloque gris pesa tanto en la pantalla que
                    // parece pulsable, y este botón no lo está hasta que hay
                    // algo revisado que instalar.
                    g2.setColor(PANEL_2);
                    g2.fillRoundRect(0, 0, w, h, 8, 8);
                    g2.setColor(BORDE);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                }

                setForeground(activo ? ACENTO_TINTA : BORDE.brighter());
            } else {
                if (encima && activo) {
                    g2.setColor(PANEL_2);
                    g2.fillRoundRect(0, 0, w, h, 8, 8);
                }

                g2.setColor(activo && encima ? TENUE : BORDE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                setForeground(activo ? TEXTO : TENUE);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Una tarjeta con esquinas redondeadas, para agrupar sin líneas duras. */
    static final class Tarjeta extends JPanel {

        Tarjeta(java.awt.LayoutManager disposicion) {
            super(disposicion);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(PANEL);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.setColor(BORDE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Barra de desplazamiento discreta.
     *
     * <p>La del sistema es un bloque gris claro con dos flechas: en una ventana
     * oscura es lo primero que se ve, y es lo que menos importa de la pantalla.
     */
    static void oscurecerBarras(JScrollPane scroll) {
        scroll.getVerticalScrollBar().setUI(new BarraDiscreta());
        scroll.getHorizontalScrollBar().setUI(new BarraDiscreta());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
    }

    private static final class BarraDiscreta extends BasicScrollBarUI {

        @Override
        protected void configureScrollBarColors() {
            trackColor = PANEL;
            thumbColor = BORDE;
        }

        @Override
        protected JButton createDecreaseButton(int orientacion) {
            return botonInvisible();
        }

        @Override
        protected JButton createIncreaseButton(int orientacion) {
            return botonInvisible();
        }

        private JButton botonInvisible() {
            JButton boton = new JButton();
            boton.setPreferredSize(new Dimension(0, 0));
            boton.setMinimumSize(new Dimension(0, 0));
            boton.setMaximumSize(new Dimension(0, 0));
            return boton;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent componente, java.awt.Rectangle limites) {
            g.setColor(PANEL);
            g.fillRect(limites.x, limites.y, limites.width, limites.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent componente, java.awt.Rectangle limites) {
            if (limites.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isThumbRollover() ? TENUE : BORDE);
            g2.fillRoundRect(limites.x + 2, limites.y + 2,
                    limites.width - 4, limites.height - 4, 6, 6);
            g2.dispose();
        }
    }

    /** Convierte un color a "#rrggbb", para el HTML del informe. */
    static String hex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
