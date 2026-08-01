package com.sack.rpgroll.common.resource;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Copia archivos de configuración y carpetas de contenido de ejemplo desde
 * dentro del JAR de un plugin hacia su carpeta de datos, la primera vez
 * que arranca — nunca sobreescribe lo que ya exista en disco.
 * <p>
 * Compartido entre RPGRoll (:core) y sus addons (:npcs, :crates, ...) para
 * no reimplementar esta lógica en cada plugin nuevo.
 */
public class ResourceCopier {

    private final Plugin plugin;

    public ResourceCopier(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Copia archivos individuales (ej. config.yml) uno por uno, solo si
     * todavía no existen en la carpeta de datos.
     */
    public void copyFiles(List<ResourceFile> files) {
        for (ResourceFile file : files) {
            copyIfMissing(file);
        }
    }

    /**
     * Copia todo el contenido de una o más carpetas empaquetadas en
     * resources/ (ej. "races", "crates") hacia la carpeta de datos,
     * archivo por archivo, sin sobreescribir lo que ya exista. Si una
     * carpeta no tiene contenido de ejemplo en el JAR, no hace nada (no
     * es un error — es un estado válido para un plugin sin ejemplos).
     */
    public void copyDirectories(List<String> directories) {
        for (String directory : directories) {
            copyDirectory(directory);
        }
    }

    private void copyIfMissing(ResourceFile file) {

        File destination = new File(plugin.getDataFolder(), file.destination());

        if (destination.exists()) {
            return;
        }

        File parent = destination.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (InputStream in = plugin.getResource(file.resource())) {

            if (in == null) {
                if (file.required()) {
                    plugin.getLogger().severe("✘ Recurso obligatorio no encontrado en el JAR: " + file.resource());
                } else {
                    plugin.getLogger().warning("✘ Recurso no encontrado en el JAR: " + file.resource());
                }
                return;
            }

            Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("✔ Archivo creado: " + file.destination());

        } catch (IOException e) {
            plugin.getLogger().severe("✘ Error copiando " + file.resource() + ": " + e.getMessage());
        }
    }

    private void copyDirectory(String resourceFolder) {

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
                File destination = new File(plugin.getDataFolder(), name);

                if (destination.exists()) {
                    continue;
                }

                File parent = destination.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }

                try (InputStream in = plugin.getResource(name)) {

                    if (in == null) {
                        plugin.getLogger().warning("✘ No se pudo leer del JAR: " + name);
                        continue;
                    }

                    Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("✔ Archivo creado: " + name);

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

}
