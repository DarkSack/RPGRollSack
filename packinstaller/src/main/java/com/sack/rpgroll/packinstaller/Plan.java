package com.sack.rpgroll.packinstaller;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Lo que va a pasar, antes de que pase.
 *
 * <p>El instalador nunca copia nada sin construir primero uno de estos y
 * enseñarlo. Un instalador que actúa mientras decide deja el servidor a medias
 * cuando algo falla, y a nadie a quien preguntar qué llegó a tocar.
 */
record Plan(String nombrePack, Path carpetaPlugins, List<Entrada> entradas, List<Aviso> avisos) {

    /** Qué se hará con un archivo concreto. */
    enum Accion {
        /** No existe en el destino: se copia. */
        NUEVO("nuevo"),
        /** Existe con contenido distinto: se reemplaza (con copia de seguridad). */
        REEMPLAZA("reemplaza"),
        /** Existe y es idéntico: no se toca. */
        IDENTICO("ya está"),
        /** El plugin no está instalado en este servidor. */
        SIN_PLUGIN("plugin ausente"),
        /** Documentación del pack, fuera de toda carpeta de plugin. */
        DOCUMENTACION("documentación");

        private final String etiqueta;

        Accion(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        String etiqueta() {
            return etiqueta;
        }

        /** Solo estas dos escriben en disco. */
        boolean instala() {
            return this == NUEVO || this == REEMPLAZA;
        }
    }

    record Entrada(PackSource.Archivo archivo, String plugin, Path destino, Accion accion) {
    }

    /** Algo que el usuario debería leer antes de instalar. */
    record Aviso(Nivel nivel, String mensaje) {

        enum Nivel {
            /** Impide instalar. */
            ERROR,
            /** Se puede instalar, pero conviene mirarlo. */
            ADVERTENCIA,
            /** Solo para que conste. */
            NOTA
        }
    }

    boolean tieneErrores() {
        return avisos.stream().anyMatch(aviso -> aviso.nivel() == Aviso.Nivel.ERROR);
    }

    List<Entrada> aInstalar() {
        return entradas.stream().filter(entrada -> entrada.accion().instala()).toList();
    }

    long bytesAEscribir() {
        return aInstalar().stream().mapToLong(entrada -> entrada.archivo().tamano()).sum();
    }

    /** Cuántos archivos por acción, para el resumen. */
    Map<Accion, Integer> resumen() {
        Map<Accion, Integer> conteo = new TreeMap<>();

        for (Entrada entrada : entradas) {
            conteo.merge(entrada.accion(), 1, Integer::sum);
        }

        return conteo;
    }

    /** Los plugins que se van a tocar, ordenados. */
    List<String> pluginsAfectados() {
        return aInstalar().stream().map(Entrada::plugin).distinct().sorted().toList();
    }

    /** Los que traía el pack pero no están en el servidor. */
    List<String> pluginsOmitidos() {
        return entradas.stream()
                .filter(entrada -> entrada.accion() == Accion.SIN_PLUGIN)
                .map(Entrada::plugin)
                .distinct()
                .sorted()
                .toList();
    }
}
