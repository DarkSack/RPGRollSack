package com.sack.rpgroll.sackresourcepack.api;

import com.sack.rpgroll.sackresourcepack.event.AssetRegisterEvent;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Toma el contenido de un {@code resourcepack/} embebido en el jar de OTRO
 * plugin (mismo formato que un módulo de {@code content/}: {@code pack.yml}
 * + {@code assets/}) y lo copia a {@code content/<plugin-name>/}. A partir
 * de ahí es un módulo de contenido normal — el {@code ManifestScanner} ya
 * existente lo recoge en el próximo escaneo, sin ningún caso especial.
 *
 * <p>{@link #registerPlugin} re-copia solo lo que cambió (comparación por SHA-256) para no
 * pisar timestamps y disparar rebuilds del caché incremental sin motivo
 * cada vez que el plugin dueño simplemente reinicia.
 */
public class AssetRegistrationService {

    private static final String JAR_PREFIX = "resourcepack/";
    /** Ids de módulo y namespaces que acepta {@link #registerArchive}: sin puntos ni barras. */
    private static final String SAFE_ID = "[a-z0-9_-]+";

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

    /**
     * Copia un pack de recursos en zip que genera otro plugin (hoy, el {@code build.zip} de BetterModel) a
     * {@code content/<moduleId>/}, con un {@code pack.yml} propio. A diferencia de {@link #registerPlugin},
     * reescribe el módulo entero en cada llamada, sin comparar hashes: así no quedan modelos que el zip ya no
     * trae.
     *
     * <ul>
     *   <li>Solo copia lo que cuelga de {@code assets/<namespace>/}; lo demás (otros namespaces,
     *       {@code assets/minecraft/}, overlays) se omite con un aviso. {@code pack.mcmeta} y {@code pack.png}
     *       tampoco se copian: los pone el pack final de SackResourcePack.</li>
     *   <li>Escribe en una carpeta hermana {@code content/.<moduleId>.tmp-<ts>} y al terminar la pone en el
     *       sitio del módulo: un fallo a medias no deja el módulo roto y un {@code /srp publish} no ve la mitad.
     *       El {@code ManifestScanner} se salta las carpetas que empiezan por punto.</li>
     * </ul>
     *
     * <p>No dispara {@link AssetRegisterEvent}: se llama desde el fin de recarga de BetterModel, que puede
     * ir fuera del hilo principal, y Bukkit no deja lanzar eventos síncronos desde otro hilo.
     *
     * @param moduleId  carpeta bajo content/: solo {@code [a-z0-9_-]}, porque se borra al reemplazarla
     * @param namespace el de los assets que se copian: solo {@code [a-z0-9_-]}
     * @return false si el zip no existe o está roto, si los ids no valen, si trae rutas que se salen del
     *         módulo o si falla la copia; en esos casos lo que había en {@code content/<moduleId>/} sigue igual
     */
    public boolean registerArchive(String moduleId, File zip, String namespace, int priority, String description) {

        if (zip == null || !zip.isFile()) {
            return false;
        }
        Path content = contentDirectory.toPath().toAbsolutePath().normalize();
        if (moduleId == null || !moduleId.matches(SAFE_ID) || namespace == null || !namespace.matches(SAFE_ID)
                || !content.equals(content.resolve(moduleId).normalize().getParent())) {
            plugin.getLogger().warning("✘ Módulo o namespace inválido ('" + moduleId + "', '" + namespace
                    + "'): no se registra " + zip + ".");
            return false;
        }

        Path moduleDirectory = content.resolve(moduleId);
        // Mismo zip y mismos datos que la última vez: no se copia nada (BetterModel trae miles de archivos y
        // recarga a menudo). Se compara el pack.yml entero: así un cambio de prioridad o descripción sí se aplica.
        String packYml;
        try {
            // Mismo formato que el pack.yml de RPGRoll-Furniture.
            packYml = String.join("\n",
                    "# GENERADO por SackResourcePack desde " + zip.getName() + ": no se edita a mano.",
                    "id: " + moduleId,
                    "name: " + moduleId,
                    "version: '1.0'",
                    "author: 'BetterModel'",
                    "namespace: " + namespace,
                    "priority: " + priority,
                    "description: '" + description.replace("'", "''") + "'",
                    "source-sha256: " + sha256(zip.toPath()),
                    "");
            Path previous = moduleDirectory.resolve("pack.yml");
            if (Files.isRegularFile(previous) && Files.readString(previous, StandardCharsets.UTF_8).equals(packYml)) {
                return true;
            }
        } catch (IOException e) {
            plugin.getLogger().severe("✘ No se pudo leer " + zip + ": " + e.getMessage());
            return false;
        }
        Path temp = content.resolve("." + moduleId + ".tmp-" + System.currentTimeMillis());
        Path assets = temp.resolve("assets").resolve(namespace);

        try (ZipFile archive = new ZipFile(zip)) {

            // Primero se valida todo el zip: si algo falla, lo que había se queda como estaba.
            for (ZipEntry entry : Collections.list(archive.entries())) {
                if (!temp.resolve(entry.getName()).normalize().startsWith(temp)) {
                    plugin.getLogger().warning("✘ " + zip + " trae una ruta fuera del módulo ('" + entry.getName()
                            + "'): no se registra.");
                    return false;
                }
            }

            Files.createDirectories(temp);
            int skipped = 0;
            for (ZipEntry entry : Collections.list(archive.entries())) {
                if (entry.isDirectory()) {
                    continue;
                }
                Path target = temp.resolve(entry.getName()).normalize();
                if (!target.startsWith(assets)) {
                    // pack.mcmeta y pack.png siempre vienen y nunca se copian: no cuentan para el aviso.
                    if (!entry.getName().equals("pack.mcmeta") && !entry.getName().equals("pack.png")) {
                        skipped++;
                    }
                    continue;
                }
                Files.createDirectories(target.getParent());
                try (InputStream in = archive.getInputStream(entry)) {
                    Files.copy(in, target);
                }
            }

            Files.writeString(temp.resolve("pack.yml"), packYml, StandardCharsets.UTF_8);

            replace(temp, moduleDirectory);
            if (skipped > 0) {
                plugin.getLogger().warning("⚠ " + skipped + " archivos de " + zip.getName() + " no están en assets/"
                        + namespace + "/ y no se copiaron a content/" + moduleId + ".");
            }
            return true;

        } catch (IOException | InvalidPathException e) {
            plugin.getLogger().severe("✘ Error copiando " + zip + " a content/" + moduleId + ": " + e.getMessage());
            try {
                deleteRecursively(temp);
            } catch (IOException ignored) {
                // Lleva un punto delante: el escáner no la lee y la próxima copia usa otro nombre.
            }
            return false;
        }
    }

    /** Pone {@code temp} en el sitio de {@code target}: de golpe si el sistema puede; si no, borra y mueve. */
    private static void replace(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Casi ningún sistema (Windows tampoco) reemplaza de golpe una carpeta que tiene archivos.
            deleteRecursively(target);
            Files.move(temp, target);
        }
    }

    private static void deleteRecursively(Path directory) throws IOException {

        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(directory)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
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

    private static String sha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            // Por bloques: el zip de BetterModel puede pesar decenas de MB.
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            for (int n; (n = in.read(buffer)) > 0; ) {
                digest.update(buffer, 0, n);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
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
