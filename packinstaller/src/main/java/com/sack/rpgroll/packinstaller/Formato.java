package com.sack.rpgroll.packinstaller;

import java.util.Locale;

/** Cuatro ayudas de presentación, compartidas por la ventana y la consola. */
final class Formato {

    private Formato() {
    }

    static String tamano(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }

        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /** "1 archivo" / "3 archivos", porque "1 archivos" se lee mal. */
    static String archivos(int cuantos) {
        return cuantos + (cuantos == 1 ? " archivo" : " archivos");
    }
}
