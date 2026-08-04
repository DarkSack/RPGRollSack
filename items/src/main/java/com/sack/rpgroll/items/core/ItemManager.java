package com.sack.rpgroll.items.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Carga las definiciones de ítem desde plugins/RPGRoll-Items/items/**\/*.yml
 * (recursivo — admite subcarpetas de categoría como sword/, armor/, etc.)
 * y permite además registrar ítems por código —
 * {@code api.items().registerItemType(...)} de la filosofía del addon,
 * expuesto acá como {@link #register(ItemDefinition)}. Sobrevive a
 * {@link #reload()}.
 */
public class ItemManager extends ContentManager<ItemDefinition> {

    private final Map<String, ItemDefinition> apiItems = new LinkedHashMap<>();

    public ItemManager(JavaPlugin itemsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(itemsPlugin), "items", "ítem", new ItemParser(), true);
    }

    public void register(ItemDefinition item) {
        apiItems.put(item.id(), item);
    }

    @Override
    public Optional<ItemDefinition> get(String id) {
        Optional<ItemDefinition> fromApi = Optional.ofNullable(apiItems.get(id));
        return fromApi.isPresent() ? fromApi : super.get(id);
    }

    @Override
    public boolean exists(String id) {
        return apiItems.containsKey(id) || super.exists(id);
    }

    @Override
    public Collection<ItemDefinition> getAll() {

        List<ItemDefinition> combined = new ArrayList<>(super.getAll());

        for (ItemDefinition item : apiItems.values()) {
            if (!super.exists(item.id())) {
                combined.add(item);
            }
        }

        return combined;
    }

    @Override
    public int count() {
        return getAll().size();
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
