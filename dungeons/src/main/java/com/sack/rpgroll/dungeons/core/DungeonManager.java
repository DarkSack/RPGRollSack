package com.sack.rpgroll.dungeons.core;

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
 * Carga las definiciones de dungeon desde plugins/RPGRoll-Dungeons/dungeons/**\/*.yml
 * (recursivo — admite subcarpetas de categoría) y permite registrarlas
 * por código vía {@link #register(DungeonDefinition)}.
 */
public class DungeonManager extends ContentManager<DungeonDefinition> {

    private final Map<String, DungeonDefinition> apiDungeons = new LinkedHashMap<>();

    public DungeonManager(JavaPlugin dungeonsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(dungeonsPlugin), "dungeons", "dungeon", new DungeonParser(), true);
    }

    public void register(DungeonDefinition dungeon) {
        apiDungeons.put(dungeon.id(), dungeon);
    }

    @Override
    public Optional<DungeonDefinition> get(String id) {
        Optional<DungeonDefinition> fromApi = Optional.ofNullable(apiDungeons.get(id));
        return fromApi.isPresent() ? fromApi : super.get(id);
    }

    @Override
    public boolean exists(String id) {
        return apiDungeons.containsKey(id) || super.exists(id);
    }

    @Override
    public Collection<DungeonDefinition> getAll() {

        List<DungeonDefinition> combined = new ArrayList<>(super.getAll());

        for (DungeonDefinition dungeon : apiDungeons.values()) {
            if (!super.exists(dungeon.id())) {
                combined.add(dungeon);
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
