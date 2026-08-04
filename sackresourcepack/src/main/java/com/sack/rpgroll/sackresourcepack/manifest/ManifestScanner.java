package com.sack.rpgroll.sackresourcepack.manifest;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Escanea {@code content/*&#47;pack.yml} y arma un {@link AssetModule} por
 * carpeta — una carpeta sin {@code pack.yml} simplemente se ignora (no es
 * un error, puede ser un módulo a medio copiar o basura).
 */
public class ManifestScanner {

    private final Plugin plugin;
    private final File contentDirectory;

    public ManifestScanner(Plugin plugin, File contentDirectory) {
        this.plugin = plugin;
        this.contentDirectory = contentDirectory;
    }

    public List<AssetModule> scan() {

        List<AssetModule> modules = new ArrayList<>();

        if (!contentDirectory.isDirectory()) {
            return modules;
        }

        File[] children = contentDirectory.listFiles(File::isDirectory);

        if (children == null) {
            return modules;
        }

        for (File moduleDirectory : children) {

            File manifestFile = new File(moduleDirectory, "pack.yml");

            if (!manifestFile.isFile()) {
                continue;
            }

            try {
                modules.add(parseManifest(moduleDirectory, manifestFile));
            } catch (Exception e) {
                plugin.getLogger().warning(
                        "✘ No se pudo leer pack.yml de '" + moduleDirectory.getName() + "': " + e.getMessage());
            }
        }

        return modules;
    }

    private AssetModule parseManifest(File moduleDirectory, File manifestFile) {

        YamlConfiguration config = YamlConfiguration.loadConfiguration(manifestFile);

        String id = config.getString("id", moduleDirectory.getName());

        return new AssetModule(
                id,
                config.getString("name", id),
                config.getString("version", "1.0.0"),
                config.getString("author", ""),
                config.getString("namespace", id),
                config.getInt("priority", 0),
                config.getStringList("depends"),
                config.getStringList("optional"),
                config.getString("description", ""),
                moduleDirectory);
    }

}
