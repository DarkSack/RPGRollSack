package com.sack.rpgroll.sackresourcepack.build;

import com.sack.rpgroll.sackresourcepack.event.PackBuildEvent;
import com.sack.rpgroll.sackresourcepack.event.PackGeneratedEvent;
import com.sack.rpgroll.sackresourcepack.manifest.AssetModule;
import com.sack.rpgroll.sackresourcepack.manifest.DependencyResolver;
import com.sack.rpgroll.sackresourcepack.manifest.ManifestScanner;
import com.sack.rpgroll.sackresourcepack.manifest.ResolutionResult;
import com.sack.rpgroll.sackresourcepack.merge.MergeEngine;
import com.sack.rpgroll.sackresourcepack.merge.MergeResult;
import com.sack.rpgroll.sackresourcepack.merge.PackMcMetaGenerator;
import com.sack.rpgroll.sackresourcepack.validation.ValidationEngine;
import com.sack.rpgroll.sackresourcepack.validation.ValidationIssue;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Construye el resource pack: escanea los módulos de assets, resuelve sus
 * dependencias, los fusiona en {@code build/merged/}, genera
 * {@code pack.mcmeta}, valida el resultado y lo empaqueta en
 * {@code build/resourcepack.zip}.
 * <p>
 * <b>Caché.</b> Construir con veinte módulos no es gratis, y la mayoría de
 * los arranques no cambian nada. Se guarda en {@code build/state.yml} una
 * firma de los módulos —id, versión, prioridad, dependencias y cada archivo
 * con su tamaño y fecha— y, si al volver a construir la firma coincide y el
 * ZIP sigue ahí, se devuelve el anterior sin tocar disco.
 * <p>
 * <b>Eventos.</b> Antes de construir se lanza {@link PackBuildEvent}, que otro
 * plugin puede cancelar; al terminar, {@link PackGeneratedEvent} con el ZIP y
 * su SHA-1, que es lo que necesita quien lo distribuye a los jugadores.
 * <p>
 * Reconstruido el 2026-09-12 a partir del bytecode del jar publicado: el
 * paquete {@code build} nunca llegó al repositorio porque el {@code .gitignore}
 * ignoraba cualquier carpeta llamada así. La lógica es la misma, línea por
 * línea; los comentarios originales se perdieron.
 */
public class BuildEngine {

    private final Plugin plugin;
    private final File contentDirectory;
    private final File buildDirectory;
    private final File mergedDirectory;
    private final File zipFile;
    private final File stateFile;
    private final int packFormat;

    private final ManifestScanner scanner;
    private final DependencyResolver resolver = new DependencyResolver();
    private final MergeEngine mergeEngine;
    private final PackMcMetaGenerator mcMetaGenerator = new PackMcMetaGenerator();
    private final ValidationEngine validationEngine = new ValidationEngine();

    private BuildResult lastResult;
    private MergeResult lastMergeResult;

    public BuildEngine(Plugin plugin, File contentDirectory, int packFormat) {
        this.plugin = plugin;
        this.contentDirectory = contentDirectory;
        this.buildDirectory = new File(plugin.getDataFolder(), "build");
        this.mergedDirectory = new File(buildDirectory, "merged");
        this.zipFile = new File(buildDirectory, "resourcepack.zip");
        this.stateFile = new File(buildDirectory, "state.yml");
        this.packFormat = packFormat;
        this.scanner = new ManifestScanner(plugin, contentDirectory);
        this.mergeEngine = new MergeEngine(plugin);

        buildDirectory.mkdirs();
    }

    public File getZipFile() {
        return zipFile;
    }

    public BuildResult getLastResult() {
        return lastResult;
    }

    public MergeResult getLastMergeResult() {
        return lastMergeResult;
    }

    public File getMergedDirectory() {
        return mergedDirectory;
    }

    /**
     * @param force true para reconstruir aunque la caché diga que no cambió nada
     */
    public BuildResult build(boolean force) {

        List<AssetModule> rawModules = scanner.scan();
        ResolutionResult resolution = resolver.resolve(rawModules);
        List<AssetModule> modules = resolution.orderedModules();

        String signature = computeSignature(modules);

        if (!force) {

            BuildState cached = loadState();

            if (cached != null && cached.signature().equals(signature) && zipFile.isFile()) {

                List<ValidationIssue> issues = new ArrayList<>();
                resolution.errors().forEach(error -> issues.add(ValidationIssue.error(error)));

                lastResult = new BuildResult(zipFile, cached.sha1(), issues, true, modules, resolution.errors());
                return lastResult;
            }
        }

        PackBuildEvent buildEvent = new PackBuildEvent();
        Bukkit.getPluginManager().callEvent(buildEvent);

        if (buildEvent.isCancelled()) {
            lastResult = new BuildResult(zipFile, "",
                    List.of(ValidationIssue.error("Build cancelado por otro plugin (PackBuildEvent).")),
                    false, modules, resolution.errors());
            return lastResult;
        }

        MergeResult mergeResult = mergeEngine.merge(modules, mergedDirectory);
        lastMergeResult = mergeResult;

        mcMetaGenerator.generate(mergedDirectory, packFormat, modules);

        List<ValidationIssue> issues = new ArrayList<>();
        resolution.errors().forEach(error -> issues.add(ValidationIssue.error(error)));
        issues.addAll(validationEngine.validate(mergeResult, modules));

        String sha1;

        try {
            sha1 = zipDirectory(mergedDirectory, zipFile);
        } catch (IOException e) {
            issues.add(ValidationIssue.error("No se pudo generar el ZIP: " + e.getMessage()));
            lastResult = new BuildResult(zipFile, "", issues, false, modules, resolution.errors());
            return lastResult;
        }

        saveState(new BuildState(signature, sha1, System.currentTimeMillis()));

        lastResult = new BuildResult(zipFile, sha1, issues, false, modules, resolution.errors());

        Bukkit.getPluginManager().callEvent(new PackGeneratedEvent(zipFile, sha1, false));

        return lastResult;
    }

    /**
     * Huella de lo que entraría en el pack: metadatos de cada módulo y cada
     * archivo de assets con su tamaño y fecha de modificación. No lee el
     * contenido de los archivos — con tamaño y fecha basta para saber si algo
     * cambió, y leerlos todos en cada arranque anularía el sentido de la caché.
     */
    private String computeSignature(List<AssetModule> modules) {

        StringBuilder builder = new StringBuilder();

        for (AssetModule module : modules) {

            builder.append(module.id()).append('|')
                    .append(module.version()).append('|')
                    .append(module.priority()).append('|')
                    .append(module.depends()).append('|')
                    .append(module.optional()).append(';');

            File assetsDir = module.assetsDirectory();

            if (!assetsDir.isDirectory()) {
                continue;
            }

            try (Stream<Path> walk = Files.walk(assetsDir.toPath())) {
                walk.filter(Files::isRegularFile)
                        .sorted()
                        .forEach(path -> {
                            File file = path.toFile();
                            builder.append(path).append(':')
                                    .append(file.length()).append(':')
                                    .append(file.lastModified()).append(';');
                        });
            } catch (IOException ignored) {
                // Un módulo ilegible cambia la firma igual (le faltan archivos),
                // así que el próximo build no saldrá de una caché equivocada.
            }
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            return builder.toString();
        }
    }

    /** Empaqueta la carpeta en un ZIP y devuelve su SHA-1 en hexadecimal. */
    private String zipDirectory(File sourceDirectory, File targetZip) throws IOException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            try (Stream<Path> walk = Files.walk(sourceDirectory.toPath())) {
                for (Path path : walk.filter(Files::isRegularFile).sorted().toList()) {
                    // Barras normales siempre: el cliente de Minecraft no
                    // entiende las de Windows dentro de un ZIP.
                    String relative = sourceDirectory.toPath().relativize(path).toString().replace('\\', '/');
                    zip.putNextEntry(new ZipEntry(relative));
                    Files.copy(path, zip);
                    zip.closeEntry();
                }
            }
        }

        byte[] zipBytes = buffer.toByteArray();
        Files.write(targetZip.toPath(), zipBytes);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(zipBytes);
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IOException("No se pudo calcular SHA1", e);
        }
    }

    private BuildState loadState() {

        if (!stateFile.isFile()) {
            return null;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(stateFile);
        String signature = config.getString("signature");
        String sha1 = config.getString("sha1");

        if (signature == null || sha1 == null) {
            return null;
        }

        return new BuildState(signature, sha1, config.getLong("built-at", 0L));
    }

    private void saveState(BuildState state) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("signature", state.signature());
        config.set("sha1", state.sha1());
        config.set("built-at", state.builtAtMillis());

        try {
            config.save(stateFile);
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error guardando build/state.yml: " + e.getMessage());
        }
    }

    /** Borra la caché: el próximo {@link #build(boolean)} reconstruye sí o sí. */
    public void invalidateCache() {
        if (stateFile.isFile()) {
            stateFile.delete();
        }
    }

}
