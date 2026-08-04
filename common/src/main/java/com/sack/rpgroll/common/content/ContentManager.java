package com.sack.rpgroll.common.content;

import com.sack.rpgroll.common.yaml.YamlLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Servicio genérico de contenido: coordina ContentLoader + ContentRegistry
 * para un tipo T, y expone una API de consulta simple.
 * <p>
 * Cada tipo concreto de contenido (RaceManager, futuros JobManager,
 * carpeta, su label de log y su ContentParser específico.
 *
 * @param <T> tipo de contenido gestionado
 */
public class ContentManager<T extends RPGContent> implements Reloadable {

    private final JavaPlugin plugin;
    private final String label;
    private final ContentLoader<T> loader;
    private final ContentRegistry<T> registry;

    protected ContentManager(JavaPlugin plugin, YamlLoader yamlLoader, String folder, String label,
            ContentParser<T> parser) {
        this(plugin, yamlLoader, folder, label, parser, false);
    }

    /** @param recursive ver {@link ContentLoader#ContentLoader(JavaPlugin, YamlLoader, String, String, ContentParser, boolean)}. */
    protected ContentManager(JavaPlugin plugin, YamlLoader yamlLoader, String folder, String label,
            ContentParser<T> parser, boolean recursive) {
        this.plugin = plugin;
        this.label = label;
        this.loader = new ContentLoader<>(plugin, yamlLoader, folder, label, parser, recursive);
        this.registry = new ContentRegistry<>(plugin, label);
    }

    /**
     * Carga (o recarga) todo el contenido desde disco. Seguro de llamar
     * múltiples veces — cada llamada reemplaza por completo el estado anterior.
     */
    public void initialize() {

        plugin.getLogger().info("Cargando " + label + "s...");

        registry.clear();

        List<T> loaded = loader.load();

        int registered = 0;
        int duplicates = 0;

        for (T item : loaded) {
            if (registry.register(item)) {
                registered++;
            } else {
                duplicates++;
            }
        }

        plugin.getLogger().info("✔ " + label + "s leídas desde disco: " + loaded.size());
        plugin.getLogger().info("✔ " + label + "s registradas: " + registered);

        if (duplicates > 0) {
            plugin.getLogger().warning("✘ " + label + "s duplicadas ignoradas: " + duplicates);
        }

        if (registered == 0) {
            plugin.getLogger().warning(
                    "✘ No se registró ningún(a) " + label + ". El contenido podría no estar disponible en el juego.");
        }
    }

    @Override
    public void reload() {
        initialize();
    }

    public Optional<T> get(String id) {
        return registry.get(id);
    }

    public boolean exists(String id) {
        return registry.exists(id);
    }

    public Collection<T> getAll() {
        return registry.getAll();
    }

    public int count() {
        return registry.count();
    }

}