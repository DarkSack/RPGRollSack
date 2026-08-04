package com.sack.rpgroll.common.content;

import com.sack.rpgroll.common.yaml.YamlLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Lee todos los YAML de una carpeta de contenido variable y los convierte
 * en instancias de T usando el ContentParser correspondiente.
 * <p>
 * Cada archivo se procesa de forma independiente: uno inválido se descarta
 * con un warning y no afecta a los demás.
 *
 * @param <T> tipo de contenido a cargar
 */
public class ContentLoader<T extends RPGContent> {

    private final JavaPlugin plugin;
    private final YamlLoader yamlLoader;
    private final String folder;
    private final String label;
    private final ContentParser<T> parser;
    private final boolean recursive;

    /**
     * @param folder carpeta relativa a plugins/RPGRoll/ (ej. "races",
     *               "professions")
     * @param label  nombre singular usado en logs (ej. "raza", "trabajo")
     */
    public ContentLoader(JavaPlugin plugin, YamlLoader yamlLoader, String folder, String label, ContentParser<T> parser) {
        this(plugin, yamlLoader, folder, label, parser, false);
    }

    /**
     * @param recursive si es true, también lee YAML dentro de subcarpetas
     *                  (ej. items/sword/flame_blade.yml) — útil para
     *                  contenido organizado en categorías.
     */
    public ContentLoader(JavaPlugin plugin, YamlLoader yamlLoader, String folder, String label,
            ContentParser<T> parser, boolean recursive) {
        this.plugin = plugin;
        this.yamlLoader = yamlLoader;
        this.folder = folder;
        this.label = label;
        this.parser = parser;
        this.recursive = recursive;
    }

    public List<T> load() {

        List<YamlConfiguration> files = recursive
                ? yamlLoader.loadAllInFolderRecursive(folder)
                : yamlLoader.loadAllInFolder(folder);
        List<T> results = new ArrayList<>();

        for (YamlConfiguration config : files) {
            try {
                results.add(parser.parse(config));
            } catch (Exception e) {
                plugin.getLogger().warning("✘ Error cargando " + label + ": " + e.getMessage());
            }
        }

        return results;
    }

}