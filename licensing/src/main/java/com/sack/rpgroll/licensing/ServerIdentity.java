package com.sack.rpgroll.licensing;

import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Identificador estable del servidor, para poder contar en cuántos
 * servidores distintos corre una licencia.
 * <p>
 * Es un UUID aleatorio que se genera una sola vez y queda en
 * {@code plugins/RPGRoll/.server-id}: reiniciar el servidor NO cuenta como
 * uno nuevo, que es justamente lo que hace útil el número. No se deriva de
 * la IP ni del hardware — una IP dinámica inventaría servidores fantasma, y
 * no hace falta identificar la máquina, solo distinguirla.
 * <p>
 * No es un dato de identidad del comprador: es un valor aleatorio sin
 * relación con nada suyo.
 */
final class ServerIdentity {

    /**
     * Vive en la carpeta {@code plugins/}, no en la de cada plugin: los 24
     * módulos verifican licencia por separado, y si cada uno generara su
     * propio id, un solo servidor aparecería 24 veces en el panel. Al ser un
     * archivo suelto (no un .jar) Bukkit lo ignora por completo.
     */
    private static final String FILE_NAME = ".rpgroll-server-id";

    private ServerIdentity() {
    }

    /**
     * Devuelve el id persistido, creándolo la primera vez. Si el disco no
     * deja leer ni escribir, devuelve un id efímero en vez de fallar: la
     * verificación de licencia no debe caerse por una estadística.
     */
    /**
     * {@code plugins/.rpgroll-server-id}, compartido por todos los módulos.
     * Si por lo que sea no se puede resolver la carpeta padre, se cae a la
     * del propio plugin: peor para las estadísticas, pero nunca un fallo.
     */
    private static Path sharedFile(Plugin plugin) {

        java.io.File dataFolder = plugin.getDataFolder();
        java.io.File pluginsFolder = dataFolder.getParentFile();

        return (pluginsFolder != null ? pluginsFolder : dataFolder).toPath().resolve(FILE_NAME);
    }

    static String resolve(Plugin plugin) {

        Path file = sharedFile(plugin);

        try {
            if (Files.exists(file)) {
                String existing = Files.readString(file, StandardCharsets.UTF_8).trim();

                if (!existing.isEmpty()) {
                    return existing;
                }
            }

            String generated = UUID.randomUUID().toString();
            Files.createDirectories(file.getParent());
            Files.writeString(file, generated, StandardCharsets.UTF_8);

            return generated;

        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar el id de servidor (" + e.getMessage()
                    + ") — se usa uno temporal para esta sesión.");
            return UUID.randomUUID().toString();
        }
    }

}
