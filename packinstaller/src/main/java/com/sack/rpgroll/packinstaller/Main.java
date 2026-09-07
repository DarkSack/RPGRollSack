package com.sack.rpgroll.packinstaller;

import java.awt.GraphicsEnvironment;

/**
 * Instalador de packs de contenido de RPGRoll.
 *
 * <p>Reparte los archivos de un pack en las carpetas de los plugins que el
 * servidor tenga instalados, y omite los que no.
 *
 * <p><b>Por qué Java y no otra cosa.</b> El comprador corre Paper, que exige
 * Java para arrancar: ya lo tiene, sin excepción. Un ejecutable en Python o
 * Node obligaría a instalar un intérprete solo para copiar archivos. Swing va
 * dentro del propio JDK, así que esto es un único .jar sin dependencias que se
 * abre con doble clic.
 *
 * <p><b>Por qué también hay modo consola.</b> Muchos servidores viven en un VPS
 * al que se entra por SSH, sin escritorio. Sin este modo, la herramienta no le
 * serviría justamente a quien más archivos tiene que mover. Se elige solo: si
 * hay pantalla, ventana; si no, consola.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Consola.ajustarCodificacion();

        if (args.length > 0) {
            System.exit(Cli.ejecutar(args));
            return;
        }

        if (GraphicsEnvironment.isHeadless()) {
            System.out.println(Cli.ayuda());
            System.exit(2);
            return;
        }

        InstallerWindow.mostrar();
    }
}
