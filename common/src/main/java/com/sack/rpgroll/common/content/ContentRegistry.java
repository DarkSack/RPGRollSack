package com.sack.rpgroll.common.content;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Almacén en memoria de contenido activo (razas, clases, trabajos, etc.).
 * Responsabilidad única: guardar y exponer consulta de solo lectura.
 *
 * @param <T> tipo de contenido almacenado
 */
public class ContentRegistry<T extends RPGContent> {

    private final JavaPlugin plugin;
    private final String label;
    private final Map<String, T> items = new LinkedHashMap<>();

    public ContentRegistry(JavaPlugin plugin, String label) {
        this.plugin = plugin;
        this.label = label;
    }

    /**
     * @return true si se registró, false si el id ya existía (se ignora, avisando)
     */
    public boolean register(T item) {
        if (items.containsKey(item.id())) {
            plugin.getLogger().warning(
                    "✘ " + label + " duplicado(a) ignorado(a): '" + item.id() + "'");
            return false;
        }
        items.put(item.id(), item);
        return true;
    }

    public void clear() {
        items.clear();
    }

    public Optional<T> get(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(items.get(id));
    }

    public boolean exists(String id) {
        return id != null && items.containsKey(id);
    }

    public Collection<T> getAll() {
        return List.copyOf(items.values());
    }

    public int count() {
        return items.size();
    }

}