package com.sack.rpgroll.packinstaller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Aplica un {@link Plan} ya revisado.
 *
 * <p>No decide nada: todo lo decidió el planificador. Acá solo se copia, y por
 * eso es la única clase que escribe en el disco del comprador.
 */
final class Installer {

    /** Resultado, para el informe final. */
    record Resultado(int copiados, int reemplazados, int omitidos, Path copiaDeSeguridad,
                     List<String> fallos) {

        boolean todoBien() {
            return fallos.isEmpty();
        }
    }

    private final PackSource pack;

    Installer(PackSource pack) {
        this.pack = pack;
    }

    /**
     * @param progreso recibe el nombre de cada archivo según se va copiando
     */
    Resultado instalar(Plan plan, Consumer<String> progreso) {
        List<String> fallos = new ArrayList<>();
        List<Plan.Entrada> aInstalar = plan.aInstalar();

        boolean hayReemplazos = aInstalar.stream()
                .anyMatch(entrada -> entrada.accion() == Plan.Accion.REEMPLAZA);

        // La carpeta de respaldo solo se crea si hay algo que respaldar: dejar
        // carpetas vacías por ahí solo genera dudas sobre si algo se rompió.
        Path respaldo = hayReemplazos ? carpetaDeRespaldo(plan.carpetaPlugins()) : null;

        int copiados = 0;
        int reemplazados = 0;

        for (Plan.Entrada entrada : aInstalar) {
            try {
                if (entrada.accion() == Plan.Accion.REEMPLAZA) {
                    respaldar(entrada.destino(), plan.carpetaPlugins(), respaldo);
                }

                Files.createDirectories(entrada.destino().getParent());

                // A un temporal y luego un movimiento: si el proceso muere a
                // mitad de la escritura, el archivo original queda intacto en
                // vez de convertirse en medio archivo que el plugin no puede
                // leer al arrancar.
                Path temporal = entrada.destino().resolveSibling(
                        entrada.destino().getFileName() + ".rpgroll-tmp");

                Files.write(temporal, pack.leer(entrada.archivo()));
                Files.move(temporal, entrada.destino(), StandardCopyOption.REPLACE_EXISTING);

                if (entrada.accion() == Plan.Accion.REEMPLAZA) {
                    reemplazados++;
                } else {
                    copiados++;
                }

                progreso.accept(entrada.archivo().rutaRelativa());
            } catch (IOException error) {
                fallos.add(entrada.archivo().rutaRelativa() + ": " + mensajeDe(error));
            }
        }

        int omitidos = (int) plan.entradas().stream()
                .filter(entrada -> !entrada.accion().instala())
                .count();

        return new Resultado(copiados, reemplazados, omitidos, respaldo, fallos);
    }

    /**
     * Guarda una copia del archivo que se va a pisar.
     *
     * <p>Se conserva la estructura de carpetas dentro del respaldo para poder
     * restaurar arrastrando la carpeta entera de vuelta a {@code plugins/}.
     */
    private void respaldar(Path original, Path carpetaPlugins, Path respaldo) throws IOException {
        Path relativa = carpetaPlugins.relativize(original);
        Path copia = respaldo.resolve(relativa);

        Files.createDirectories(copia.getParent());
        Files.copy(original, copia, StandardCopyOption.REPLACE_EXISTING);
    }

    private static Path carpetaDeRespaldo(Path carpetaPlugins) {
        String marca = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

        // Junto a plugins/, no dentro: una carpeta ahí dentro haría que el
        // servidor intentara cargarla como si fuera un plugin.
        Path padre = carpetaPlugins.getParent() == null ? carpetaPlugins : carpetaPlugins.getParent();
        return padre.resolve("rpgroll-respaldo-" + marca);
    }

    private static String mensajeDe(IOException error) {
        String mensaje = error.getMessage();
        return mensaje == null || mensaje.isBlank() ? error.getClass().getSimpleName() : mensaje;
    }
}
