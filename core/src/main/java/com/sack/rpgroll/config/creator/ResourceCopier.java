package com.sack.rpgroll.config.creator;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.ConfigFile;

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

public class ResourceCopier {

    private final RPGRoll plugin;
    private final List<ConfigFile> configFiles;

    /**
     * Carpetas de contenido variable que deben poblarse con YAML de ejemplo
     * desde resources/ la primera vez que el servidor arranca.
     */
    private static final List<String> CONTENT_DIRECTORIES = List.of(
            "races",
            "classes",
            "skills",
            "traits",
            "professions",
            "items",
            "quests",
            "jobs");

    public ResourceCopier(RPGRoll plugin, List<ConfigFile> configFiles) {
        this.plugin = plugin;
        this.configFiles = configFiles;
    }

    public void copy() {
        plugin.getLogger().info("Copiando recursos...");

        for (ConfigFile config : configFiles) {
            copyIfMissing(config);
        }

        for (String directory : CONTENT_DIRECTORIES) {
            copyDirectory(directory);
        }
    }

    private void copyIfMissing(ConfigFile config) {
        File destination = new File(plugin.getDataFolder(), config.destination());

        if (destination.exists()) {
            return;
        }

        File parent = destination.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (InputStream in = plugin.getResource(config.resource())) {
            if (in == null) {
                if (config.required()) {
                    plugin.getLogger().severe("✘ Recurso obligatorio no encontrado en el JAR: " + config.resource());
                } else {
                    plugin.getLogger().warning("✘ Recurso no encontrado en el JAR: " + config.resource());
                }
                return;
            }

            Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("✔ Archivo creado: " + config.destination());

        } catch (IOException e) {
            plugin.getLogger().severe("✘ Error copiando " + config.resource() + ": " + e.getMessage());
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
                plugin.getLogger()
                        .info("… Sin contenido por defecto para: " + resourceFolder + " (carpeta vacía en resources)");
            }

        } catch (IOException e) {
            plugin.getLogger().severe("✘ Error al leer el JAR del plugin: " + e.getMessage());
        }
    }
}