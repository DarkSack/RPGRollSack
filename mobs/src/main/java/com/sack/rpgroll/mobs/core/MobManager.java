package com.sack.rpgroll.mobs.core;

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
 * Carga las definiciones de mob desde plugins/RPGRoll-Mobs/mobs/**\/*.yml
 * (recursivo — admite subcarpetas de categoría) y permite registrarlas por
 * código — {@code api.mobs().registerMob(...)} de la filosofía del addon,
 * expuesto acá como {@link #register(MobDefinition)}.
 */
public class MobManager extends ContentManager<MobDefinition> {

    private final Map<String, MobDefinition> apiMobs = new LinkedHashMap<>();

    public MobManager(JavaPlugin mobsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(mobsPlugin), "mobs", "mob", new MobParser(), true);
    }

    public void register(MobDefinition mob) {
        apiMobs.put(mob.id(), mob);
    }

    @Override
    public Optional<MobDefinition> get(String id) {
        Optional<MobDefinition> fromApi = Optional.ofNullable(apiMobs.get(id));
        return fromApi.isPresent() ? fromApi : super.get(id);
    }

    @Override
    public boolean exists(String id) {
        return apiMobs.containsKey(id) || super.exists(id);
    }

    @Override
    public Collection<MobDefinition> getAll() {

        List<MobDefinition> combined = new ArrayList<>(super.getAll());

        for (MobDefinition mob : apiMobs.values()) {
            if (!super.exists(mob.id())) {
                combined.add(mob);
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
