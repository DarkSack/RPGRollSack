package com.sack.rpgroll.sackresourcepack.datapack;

import com.sack.rpgroll.sackresourcepack.manifest.AssetModule;
import com.sack.rpgroll.sackresourcepack.manifest.DependencyResolver;
import com.sack.rpgroll.sackresourcepack.manifest.ManifestScanner;
import com.sack.rpgroll.sackresourcepack.manifest.ResolutionResult;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

/**
 * Orquesta el build del datapack: escanea los mismos módulos de {@code
 * content/} (reusa {@link ManifestScanner}/{@link DependencyResolver}, que
 * ya son genéricos), fusiona solo la carpeta {@code data/} de cada uno,
 * genera su propio {@code pack.mcmeta} y copia el árbol resultante dentro
 * de {@code <mundo>/datapacks/<nombre>/}. A diferencia de un resource
 * pack, un datapack NO se empaqueta en ZIP ni se sirve por HTTP — vive
 * server-side y Minecraft lo lee directo de esa carpeta.
 * <p>
 * Aplicar los cambios requiere un reload explícito ({@link #reload()},
 * que llama a {@code Server#reloadData()}) — nunca se dispara solo, porque
 * recargar recetas/loot tables/advancements de golpe puede tener efectos
 * secundarios visibles para jugadores conectados.
 */
public class DatapackBuildEngine {

    private final Plugin plugin;
    private final File contentDirectory;
    private final File mergedDirectory;
    private final String packName;
    private final int packFormat;

    private final ManifestScanner scanner;
    private final DependencyResolver resolver = new DependencyResolver();
    private final DatapackMergeEngine mergeEngine;
    private final DatapackMcMetaGenerator mcMetaGenerator = new DatapackMcMetaGenerator();

    private DatapackBuildResult lastResult;

    public DatapackBuildEngine(Plugin plugin, File contentDirectory, String packName, int packFormat) {
        this.plugin = plugin;
        this.contentDirectory = contentDirectory;
        this.mergedDirectory = new File(plugin.getDataFolder(), "build/datapack-merged");
        this.packName = packName;
        this.packFormat = packFormat;
        this.scanner = new ManifestScanner(plugin, contentDirectory);
        this.mergeEngine = new DatapackMergeEngine(plugin);
    }

    public DatapackBuildResult getLastResult() {
        return lastResult;
    }

    public DatapackBuildResult build() {

        List<AssetModule> rawModules = scanner.scan();
        ResolutionResult resolution = resolver.resolve(rawModules);
        List<AssetModule> modules = resolution.orderedModules();

        mergeEngine.merge(modules, mergedDirectory);
        mcMetaGenerator.generate(mergedDirectory, packFormat, modules);

        lastResult = new DatapackBuildResult(mergedDirectory, modules, resolution.errors());
        return lastResult;
    }

    /** Copia el árbol fusionado a {@code <mundo primario>/datapacks/<packName>/}. NO recarga solo — llamar a {@link #reload()} aparte. */
    public void distribute() {

        World primaryWorld = Bukkit.getWorlds().get(0);
        File target = new File(new File(primaryWorld.getWorldFolder(), "datapacks"), packName);

        try {
            deleteRecursively(target);
            copyTree(mergedDirectory.toPath(), target.toPath());
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error copiando el datapack a " + target + ": " + e.getMessage());
        }
    }

    /** Recarga recetas/loot tables/tags/advancements sin reiniciar plugins — {@code Server#reloadData()}. */
    public void reload() {
        Bukkit.getServer().reloadData();
    }

    private void copyTree(Path source, Path target) throws IOException {

        try (Stream<Path> walk = Files.walk(source)) {

            for (Path path : walk.toList()) {

                Path destination = target.resolve(source.relativize(path));

                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void deleteRecursively(File file) {

        if (!file.exists()) {
            return;
        }

        File[] children = file.listFiles();

        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }

        file.delete();
    }

}
