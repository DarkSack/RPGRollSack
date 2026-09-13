package com.sack.rpgroll.common.resource;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Copia archivos de configuración y carpetas de contenido de ejemplo desde
 * dentro del JAR de un plugin hacia su carpeta de datos, y los mantiene al día
 * en las actualizaciones <b>sin pisar nunca lo que el administrador editó</b>.
 * <p>
 * Qué se actualiza y qué no lo decide {@link ResourceSync}: un archivo que
 * sigue siendo exactamente la copia que escribió el plugin se reemplaza por la
 * versión nueva; uno editado se respeta, y la versión nueva queda aparte en
 * {@code .rpgroll/actualizaciones/} para comparar.
 * <p>
 * Compartido entre RPGRoll (:core) y sus addons (:npcs, :crates, ...) para
 * no reimplementar esta lógica en cada plugin nuevo.
 */
public class ResourceCopier {

    private final Plugin plugin;

    public ResourceCopier(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Copia o actualiza archivos individuales (ej. config.yml). */
    public void copyFiles(List<ResourceFile> files) {

        ResourceSync sync = new ResourceSync(plugin.getDataFolder().toPath());
        Map<ResourceSync.Outcome, Integer> tally = new EnumMap<>(ResourceSync.Outcome.class);

        for (ResourceFile file : files) {

            try (InputStream in = plugin.getResource(file.resource())) {

                if (in == null) {
                    if (file.required()) {
                        plugin.getLogger().severe("✘ Recurso obligatorio no encontrado en el JAR: " + file.resource());
                    } else {
                        plugin.getLogger().warning("✘ Recurso no encontrado en el JAR: " + file.resource());
                    }
                    continue;
                }

                record(sync, file.destination(), in.readAllBytes(), tally);

            } catch (IOException e) {
                plugin.getLogger().severe("✘ Error copiando " + file.resource() + ": " + e.getMessage());
            }
        }

        finish(sync, tally);
    }

    /**
     * Copia o actualiza todo el contenido de una o más carpetas empaquetadas
     * en resources/ (ej. "races", "crates"), archivo por archivo. Si una
     * carpeta no tiene contenido de ejemplo en el JAR, no hace nada (no es un
     * error — es un estado válido para un plugin sin ejemplos).
     */
    public void copyDirectories(List<String> directories) {

        ResourceSync sync = new ResourceSync(plugin.getDataFolder().toPath());
        Map<ResourceSync.Outcome, Integer> tally = new EnumMap<>(ResourceSync.Outcome.class);

        for (String directory : directories) {
            copyDirectory(directory, sync, tally);
        }

        finish(sync, tally);
    }

    private void copyDirectory(String resourceFolder, ResourceSync sync, Map<ResourceSync.Outcome, Integer> tally) {

        String prefix = resourceFolder.endsWith("/") ? resourceFolder : resourceFolder + "/";

        File jarFile;

        try {
            jarFile = new File(plugin.getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
        } catch (URISyntaxException e) {
            plugin.getLogger().severe("✘ No se pudo localizar el JAR del plugin para copiar: " + resourceFolder);
            return;
        }

        try (JarFile jar = new JarFile(jarFile)) {

            Enumeration<JarEntry> entries = jar.entries();
            boolean foundAny = false;

            while (entries.hasMoreElements()) {

                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (entry.isDirectory() || !name.startsWith(prefix)) {
                    continue;
                }

                foundAny = true;

                try (InputStream in = jar.getInputStream(entry)) {
                    record(sync, name, in.readAllBytes(), tally);
                } catch (IOException e) {
                    plugin.getLogger().warning("✘ Error copiando " + name + ": " + e.getMessage());
                }
            }

            if (!foundAny) {
                plugin.getLogger().info(
                        "… Sin contenido por defecto para: " + resourceFolder + " (carpeta vacía en resources)");
            }

        } catch (IOException e) {
            plugin.getLogger().severe("✘ Error al leer el JAR del plugin: " + e.getMessage());
        }
    }

    private void record(ResourceSync sync, String path, byte[] content,
                        Map<ResourceSync.Outcome, Integer> tally) throws IOException {

        ResourceSync.Outcome outcome = sync.sync(path, content);
        tally.merge(outcome, 1, Integer::sum);

        switch (outcome) {
            case CREATED -> plugin.getLogger().info("✔ Archivo creado: " + path);
            case UPDATED -> plugin.getLogger().info("↻ Actualizado a la versión nueva: " + path);
            case KEPT_EDITED -> plugin.getLogger().info(
                    "… " + path + " tiene cambios tuyos y no se tocó. La versión nueva está en "
                            + ResourceSync.INTERNAL_DIR + "/" + ResourceSync.UPDATES_DIR + "/" + path);
            case UNCHANGED -> {
                // Lo normal en cada arranque: no merece una línea.
            }
        }
    }

    private void finish(ResourceSync sync, Map<ResourceSync.Outcome, Integer> tally) {

        try {
            sync.save();
        } catch (IOException e) {
            // Sin registro, el próximo arranque cae al modo prudente: no pisa
            // nada. Se avisa, pero no es motivo para no arrancar.
            plugin.getLogger().warning("✘ No se pudo guardar el registro de recursos: " + e.getMessage());
        }

        int kept = tally.getOrDefault(ResourceSync.Outcome.KEPT_EDITED, 0);

        if (kept > 0) {
            // Una línea de resumen además de las individuales: con veinte
            // archivos, las líneas sueltas se pierden entre el resto del arranque.
            plugin.getLogger().warning("⚠ " + kept + " archivo(s) editados por ti tienen una versión nueva en "
                    + ResourceSync.INTERNAL_DIR + "/" + ResourceSync.UPDATES_DIR
                    + "/. No se cambió nada tuyo: compáralos y copia lo que quieras."
                    + " Si nunca los editaste, borra el archivo y se vuelve a crear con la versión nueva.");
        }
    }

}
