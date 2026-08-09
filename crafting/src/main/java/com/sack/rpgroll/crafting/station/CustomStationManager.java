package com.sack.rpgroll.crafting.station;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomStationManager extends ContentManager<CustomStation> {

    private final CustomStationDefinitionWriter writer;

    public CustomStationManager(JavaPlugin craftingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(craftingPlugin), "stations", "estación", new CustomStationParser());
        this.writer = new CustomStationDefinitionWriter(craftingPlugin.getDataFolder());
    }

    public void save(CustomStation station) {
        writer.save(station);
        reload();
    }

    public void delete(String id) {
        writer.delete(id);
        reload();
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
