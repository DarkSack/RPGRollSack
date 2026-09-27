package com.sack.rpgroll.sackresourcepack.manifest;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Escanea {@code content/*&#47;pack.yml} y arma un {@link AssetModule} por
 * carpeta — una carpeta sin {@code pack.yml} simplemente se ignora (no es
 * un error, puede ser un módulo a medio copiar o basura).
 * <p>
 * Además suma los {@code external-packs} del config: packs que genera otro
 * plugin en su propia carpeta (FreeMinecraftModels, por ejemplo), leídos en su
 * sitio en cada build, sin copiarlos a mano. Cuentan como un módulo más.
 */
public class ManifestScanner {

    private final Plugin plugin;
    private final File contentDirectory;

    public ManifestScanner(Plugin plugin, File contentDirectory) {
        this.plugin = plugin;
        this.contentDirectory = contentDirectory;
    }

    public List<AssetModule> scan() {
        List<AssetModule> modules = scanContent();
        modules.addAll(scanExternal());
        return modules;
    }

    private List<AssetModule> scanContent() {

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

    /**
     * Cada entrada de {@code external-packs}: {@code path} (relativo a la carpeta
     * plugins/), {@code id}, {@code namespace}, {@code priority} y {@code exclude}
     * (rutas dentro de su assets/ que no entran al pack). Una carpeta que
     * todavía no existe (el otro plugin aún no generó su pack) se salta sin error.
     */
    private List<AssetModule> scanExternal() {

        List<AssetModule> modules = new ArrayList<>();
        File pluginsDirectory = plugin.getDataFolder().getParentFile();

        for (Map<?, ?> entry : plugin.getConfig().getMapList("external-packs")) {

            Object rawPath = entry.get("path");
            if (rawPath == null || rawPath.toString().isBlank()) {
                continue;
            }

            File directory = new File(pluginsDirectory, rawPath.toString());
            if (!new File(directory, "assets").isDirectory()) {
                plugin.getLogger().info("· Pack externo sin assets todavía, se salta: " + rawPath);
                continue;
            }

            String id = entry.get("id") != null ? entry.get("id").toString() : directory.getName().toLowerCase();
            String namespace = entry.get("namespace") != null ? entry.get("namespace").toString() : id;
            int priority = entry.get("priority") instanceof Number number ? number.intValue() : 0;
            List<String> exclude = new ArrayList<>();
            if (entry.get("exclude") instanceof List<?> list) {
                list.forEach(path -> exclude.add(path.toString().replace('\\', '/')));
            }

            modules.add(new AssetModule(id, id, "externo", "", namespace, priority, List.of(), List.of(),
                    "Pack externo: " + rawPath, directory, exclude));
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
