package com.sack.rpgroll.packinstaller;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Hace que los acentos lleguen enteros a la terminal.
 *
 * <p>Desde Java 18 el código fuente y las cadenas son UTF-8, pero la consola de
 * Windows no: suele estar en CP-850 o CP-1252. El resultado es que cada palabra
 * con tilde sale rota — «no está instalado» se lee «no est� instalado». En una
 * herramienta escrita entera en español eso es casi cada línea.
 *
 * <p>La solución no es forzar UTF-8, que dejaría el texto igual de roto en una
 * consola que no lo entiende, sino escribir en la codificación que la terminal
 * dice usar. Cuando la salida está redirigida a un archivo no hay terminal a la
 * que preguntar, y entonces UTF-8 es la elección correcta.
 */
final class Consola {

    private Consola() {
    }

    /**
     * Reemplaza {@code System.out} y {@code System.err} por otros que codifican
     * bien. Es lo primero que hace el programa, antes de imprimir nada.
     */
    static void ajustarCodificacion() {
        Charset destino = codificacionDeLaTerminal();

        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, destino));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, destino));
    }

    private static Charset codificacionDeLaTerminal() {
        var terminal = System.console();

        if (terminal != null) {
            return terminal.charset();
        }

        // Sin terminal la salida va a un archivo o a otro programa, y ahí lo
        // correcto es UTF-8.
        //
        // Aquí NO sirve `stdout.encoding`: en Windows vale Cp1252 —la página de
        // códigos ANSI del sistema— incluso cuando la salida está redirigida,
        // así que hacerle caso escribe Cp1252 en un archivo que todo el mundo
        // va a leer como UTF-8. Se comprobó en la máquina de desarrollo:
        // stdout.encoding=Cp1252, file.encoding=UTF-8, console=null.
        return StandardCharsets.UTF_8;
    }
}
