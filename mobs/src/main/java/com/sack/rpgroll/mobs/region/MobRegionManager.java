package com.sack.rpgroll.mobs.region;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class MobRegionManager extends ContentManager<MobRegion> {

    public MobRegionManager(JavaPlugin mobsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(mobsPlugin), "regions", "región", new MobRegionParser());
    }

    public Optional<MobRegion> findAt(Location location) {
        return getAll().stream().filter(region -> region.contains(location)).findFirst();
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
