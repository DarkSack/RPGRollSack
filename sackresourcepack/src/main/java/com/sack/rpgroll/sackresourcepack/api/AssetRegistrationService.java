package com.sack.rpgroll.sackresourcepack.api;

import com.sack.rpgroll.sackresourcepack.event.AssetRegisterEvent;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Toma el contenido de un {@code resourcepack/} embebido en el jar de OTRO
 * plugin (mismo formato que un módulo de {@code content/}: {@code pack.yml}
 * + {@code assets/}) y lo copia a {@code content/<plugin-name>/}. A partir
 * de ahí es un módulo de contenido normal — el {@code ManifestScanner} ya
 * existente lo recoge en el próximo escaneo, sin ningún caso especial.
 *
 * <p>Se re-copia solo lo que cambió (comparación por SHA-256) para no
 * pisar timestamps y disparar rebuilds del caché incremental sin motivo
 * cada vez que el plugin dueño simplemente reinicia.
 */
public class AssetRegistrationService {

    private static final String JAR_PREFIX = "resourcepack/";

    private final Plugin plugin;
    private final File contentDirectory;

    AssetRegistrationService(Plugin plugin, File contentDirectory) {
        this.plugin = plugin;
        this.contentDirectory = contentDirectory;
    }

    /**
     * Escanea el jar del plugin dado en busca de una carpeta
     * {@code resourcepack/} (con {@code pack.yml} adentro) y la sincroniza
     * a {@code content/<plugin-name-en-minúsculas>/}.
     *
     * @return true si se encontró y copió (o ya estaba al día) contenido; false si el
     *         plugin no trae ningún {@code resourcepack/} embebido.
     */
    public boolean registerPlugin(JavaPlugin sourcePlugin) {

        File jarFile = locateJarFile(sourcePlugin);

        if (jarFile == null || !jarFile.isFile()) {
            plugin.getLogger().warning("✘ No se pudo localizar el jar de '" + sourcePlugin.getName()
                    + "' para registrar sus assets.");
            return false;
        }

        String moduleId = sourcePlugin.getName().toLowerCase();
        File moduleDirectory = new File(contentDirectory, moduleId);

        boolean copiedAny = false;

        try (JarFile jar = new JarFile(jarFile)) {

            Enumeration<JarEntry> entries = jar.entries();
            boolean foundAny = false;

            while (entries.hasMoreElements()) {

                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (entry.isDirectory() || !name.startsWith(JAR_PREFIX)) {
                    continue;
                }

                String relative = name.substring(JAR_PREFIX.length());

                if (relative.isBlank()) {
                    continue;
                }

                foundAny = true;

                try (InputStream in = jar.getInputStream(entry)) {

                    byte[] bytes = in.readAllBytes();
                    File target = new File(moduleDirectory, relative);

                    if (writeIfChanged(target, bytes)) {
                        copiedAny = true;
                    }
                }
            }

            if (!foundAny) {
                plugin.getLogger().info("El plugin '" + sourcePlugin.getName()
                        + "' no trae ningún resourcepack/ embebido, nada que registrar.");
                return false;
            }

        } catch (IOException e) {
            plugin.getLogger().severe("✘ Error leyendo el jar de '" + sourcePlugin.getName() + "': " + e.getMessage());
            return false;
        }

        Bukkit.getPluginManager().callEvent(new AssetRegisterEvent(sourcePlugin.getName(), moduleId));
        return true;
    }

    private boolean writeIfChanged(File target, byte[] bytes) {

        try {

            if (target.isFile() && sameContent(target, bytes)) {
                return false;
            }

            Files.createDirectories(target.getParentFile().toPath());
            Files.write(target.toPath(), bytes);
            return true;

        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error escribiendo '" + target + "': " + e.getMessage());
            return false;
        }
    }

    private boolean sameContent(File existing, byte[] incoming) {

        try {

            byte[] existingBytes = Files.readAllBytes(existing.toPath());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return java.util.Arrays.equals(digest.digest(existingBytes), digest.digest(incoming));

        } catch (Exception e) {
            return false;
        }
    }

    private File locateJarFile(JavaPlugin sourcePlugin) {

        try {
            return new File(sourcePlugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException | NullPointerException e) {
            return null;
        }
    }

}
