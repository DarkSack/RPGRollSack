package com.sack.rpgroll.mobs.region;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class MobRegionManager extends ContentManager<MobRegion> {

    public MobRegionManager(JavaPlugin mobsPlugin) {
        super(owningPlugin(), new YamlLoader(mobsPlugin), "regions", "región", new MobRegionParser());
    }

    public Optional<MobRegion> findAt(Location location) {
        return getAll().stream().filter(region -> region.contains(location)).findFirst();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(MobRegionManager.class);
    }

}
