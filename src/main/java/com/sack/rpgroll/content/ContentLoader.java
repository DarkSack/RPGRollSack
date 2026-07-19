package com.sack.rpgroll.content;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.loader.YamlLoader;
import org.bukkit.configuration.file.YamlConfiguration;

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

    private final RPGRoll plugin;
    private final YamlLoader yamlLoader;
    private final String folder;
    private final String label;
    private final ContentParser<T> parser;

    /**
     * @param folder carpeta relativa a plugins/RPGRoll/ (ej. "races",
     *               "professions")
     * @param label  nombre singular usado en logs (ej. "raza", "trabajo")
     */
    public ContentLoader(RPGRoll plugin, YamlLoader yamlLoader, String folder, String label, ContentParser<T> parser) {
        this.plugin = plugin;
        this.yamlLoader = yamlLoader;
        this.folder = folder;
        this.label = label;
        this.parser = parser;
    }

    public List<T> load() {

        List<YamlConfiguration> files = yamlLoader.loadAllInFolder(folder);
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