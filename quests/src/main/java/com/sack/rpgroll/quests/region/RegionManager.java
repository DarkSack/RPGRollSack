package com.sack.rpgroll.quests.region;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Optional;

public class RegionManager extends ContentManager<Region> {

    private final RegionDefinitionWriter writer;

    public RegionManager(JavaPlugin questsPlugin) {
        super(owningPlugin(), new YamlLoader(questsPlugin), "regions", "región", new RegionParser());
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

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(RegionManager.class);
    }

}
