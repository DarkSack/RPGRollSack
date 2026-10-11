package com.sack.rpgroll.common.content;

import com.sack.rpgroll.common.yaml.YamlLoader;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee todos los YAML de una carpeta de contenido variable y los convierte
 * en instancias de T usando el ContentParser correspondiente.
 * <p>
 * Cada archivo se procesa de forma independiente: uno inválido se descarta
 * con un warning y no afecta a los demás.
 * <p>
 * Un archivo puede traer sus textos en varios idiomas: lo que haya bajo
 * {@code translations.<idioma>} pisa los campos del mismo nombre cuando ese es
 * el idioma del servidor ({@code language} en la config del plugin).
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
        String locale = locale();

        for (YamlConfiguration config : files) {
            try {
                results.add(parser.parse(translate(config, locale)));
            } catch (Exception e) {
                plugin.getLogger().warning("✘ Error cargando " + label + ": " + e.getMessage());
            }
        }

        return results;
    }

    private String locale() {
        return locale(plugin.getDataFolder());
    }

    /** Se lee del archivo cada vez: getConfig() guarda la copia del arranque y un reload no vería el cambio. */
    private static String locale(File dataFolder) {
        return YamlConfiguration.loadConfiguration(new File(dataFolder, "config.yml")).getString("language", "es");
    }

    /**
     * Guarda lo que escribe un editor del panel sin perder las traducciones del archivo. El editor trabaja
     * con el contenido ya resuelto en el idioma del servidor: si ese idioma es uno traducido, lo editado va
     * a su bloque y el texto base se queda como estaba.
     */
    // shortcut: da por hecho que el archivo cuelga de <carpeta de datos>/<tipo>/, ampliar si un tipo de
    // contenido con subcarpetas (los packs de ítems) gana editor de textos traducidos.
    public static void save(YamlConfiguration fresh, File target) throws java.io.IOException {

        YamlConfiguration old = YamlConfiguration.loadConfiguration(target);
        ConfigurationSection translations = old.getConfigurationSection("translations");

        if (translations != null) {
            copyLeaves(translations, fresh, "translations.");

            String locale = locale(target.getParentFile().getParentFile());
            ConfigurationSection active = translations.getConfigurationSection(locale);

            if (active != null) {
                for (String path : active.getKeys(true)) {
                    if (!active.isConfigurationSection(path)) {
                        fresh.set("translations." + locale + "." + path, fresh.get(path));
                        fresh.set(path, old.get(path));
                    }
                }
            }
        }

        fresh.save(target);
    }

    private static void copyLeaves(ConfigurationSection from, YamlConfiguration to, String prefix) {
        from.getValues(true).forEach((path, value) -> {
            if (!(value instanceof ConfigurationSection)) {
                to.set(prefix + path, value);
            }
        });
    }

    static YamlConfiguration translate(YamlConfiguration config, String locale) {
        ConfigurationSection translated = config.getConfigurationSection("translations." + locale);
        if (translated != null) {
            copyLeaves(translated, config, "");
        }
        return config;
    }

}