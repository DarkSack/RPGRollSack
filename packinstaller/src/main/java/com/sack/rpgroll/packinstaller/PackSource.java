package com.sack.rpgroll.packinstaller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * De dónde salen los archivos de un pack: una carpeta o un {@code .zip}.
 *
 * <p>Se admiten los dos porque son los dos que existen de verdad. El comprador
 * descarga un zip; quien arma packs trabaja sobre una carpeta. Obligar a
 * descomprimir antes es un paso donde la gente se equivoca —descomprime una
 * carpeta de más y el pack queda anidado— y ese error es invisible hasta que
 * nada se instala.
 */
abstract class PackSource implements AutoCloseable {

    /** Un archivo del pack, con su ruta relativa a la raíz. */
    record Archivo(String rutaRelativa, long tamano) {

        /** El primer segmento: la carpeta del plugin al que pertenece. */
        String carpetaRaiz() {
            int corte = rutaRelativa.indexOf('/');
            return corte < 0 ? "" : rutaRelativa.substring(0, corte);
        }

        /** La ruta dentro de la carpeta del plugin. */
        String rutaDentroDelPlugin() {
            int corte = rutaRelativa.indexOf('/');
            return corte < 0 ? rutaRelativa : rutaRelativa.substring(corte + 1);
        }
    }

    abstract String nombre();

    abstract List<Archivo> archivos() throws IOException;

    abstract byte[] leer(Archivo archivo) throws IOException;

    @Override
    public void close() throws IOException {
    }

    static PackSource desde(Path ruta) throws IOException {
        if (Files.isDirectory(ruta)) {
            return new Carpeta(ruta);
        }

        if (ruta.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            return new Zip(ruta);
        }

        throw new IOException("Un pack tiene que ser una carpeta o un archivo .zip: " + ruta);
    }

    /**
     * Rechaza rutas que se salen del pack.
     *
     * <p>Un zip puede declarar entradas como {@code ../../.ssh/authorized_keys}
     * y, si se copian tal cual, escriben donde les da la gana. Se conoce como
     * <i>zip slip</i> y no es teórico: los packs se descargan de internet, que
     * es exactamente el escenario en el que aplica.
     *
     * <p>También se descartan las rutas absolutas y las que empiezan por barra,
     * por el mismo motivo.
     */
    static boolean rutaSegura(String ruta) {
        if (ruta.isBlank() || ruta.startsWith("/") || ruta.startsWith("\\")) {
            return false;
        }

        // Windows: "C:/..." también es absoluta aunque no empiece por barra.
        if (ruta.length() > 1 && ruta.charAt(1) == ':') {
            return false;
        }

        for (String segmento : ruta.split("/")) {
            if (segmento.equals("..")) {
                return false;
            }
        }

        return true;
    }

    /** Normaliza separadores y quita el prefijo "./" que meten algunos zips. */
    private static String normalizar(String ruta) {
        String limpia = ruta.replace('\\', '/');

        while (limpia.startsWith("./")) {
            limpia = limpia.substring(2);
        }

        return limpia;
    }

    // ------------------------------------------------------------------

    private static final class Carpeta extends PackSource {

        private final Path raiz;

        Carpeta(Path raiz) {
            this.raiz = raiz;
        }

        @Override
        String nombre() {
            return raiz.getFileName() == null ? raiz.toString() : raiz.getFileName().toString();
        }

        @Override
        List<Archivo> archivos() throws IOException {
            try (var flujo = Files.walk(raiz)) {
                return flujo
                        .filter(Files::isRegularFile)
                        .map(archivo -> {
                            String relativa = normalizar(raiz.relativize(archivo).toString());
                            try {
                                return new Archivo(relativa, Files.size(archivo));
                            } catch (IOException error) {
                                throw new UncheckedIOException(error);
                            }
                        })
                        .sorted(Comparator.comparing(Archivo::rutaRelativa))
                        .toList();
            } catch (UncheckedIOException error) {
                throw error.getCause();
            }
        }

        @Override
        byte[] leer(Archivo archivo) throws IOException {
            return Files.readAllBytes(raiz.resolve(archivo.rutaRelativa()));
        }
    }

    // ------------------------------------------------------------------

    private static final class Zip extends PackSource {

        private final Path ruta;
        private final ZipFile zip;

        Zip(Path ruta) throws IOException {
            this.ruta = ruta;
            this.zip = new ZipFile(ruta.toFile());
        }

        @Override
        String nombre() {
            String archivo = ruta.getFileName().toString();
            return archivo.substring(0, archivo.length() - ".zip".length());
        }

        @Override
        List<Archivo> archivos() {
            List<Archivo> encontrados = new ArrayList<>();

            zip.stream()
                    .filter(entrada -> !entrada.isDirectory())
                    .forEach(entrada -> {
                        String relativa = normalizar(entrada.getName());

                        // Las inseguras se descartan acá y no llegan al plan:
                        // lo que no está en la lista no se puede copiar por error
                        // más adelante.
                        if (rutaSegura(relativa)) {
                            encontrados.add(new Archivo(relativa, entrada.getSize()));
                        }
                    });

            encontrados.sort(Comparator.comparing(Archivo::rutaRelativa));
            return encontrados;
        }

        @Override
        byte[] leer(Archivo archivo) throws IOException {
            ZipEntry entrada = zip.getEntry(archivo.rutaRelativa());

            if (entrada == null) {
                throw new IOException("El zip ya no contiene " + archivo.rutaRelativa());
            }

            try (InputStream entrante = zip.getInputStream(entrada);
                 ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
                entrante.transferTo(salida);
                return salida.toByteArray();
            }
        }

        @Override
        public void close() throws IOException {
            zip.close();
        }
    }
}
