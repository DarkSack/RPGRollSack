package com.sack.rpgroll.quests.region;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Optional;

public class RegionManager extends ContentManager<Region> {

    private final RegionDefinitionWriter writer;

    public RegionManager(JavaPlugin questsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(questsPlugin), "regions", "región", new RegionParser());
        this.writer = new RegionDefinitionWriter(new File(questsPlugin.getDataFolder(), "regions"),
                questsPlugin.getLogger());
    }

    /** Persiste la región a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(Region region) {
        writer.save(region);
        reload();
    }

    public Optional<Region> findAt(Location location) {
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
