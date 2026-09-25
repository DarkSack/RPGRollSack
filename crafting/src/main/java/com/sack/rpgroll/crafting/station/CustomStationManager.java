package com.sack.rpgroll.crafting.station;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class CustomStationManager extends ContentManager<CustomStation> {

    private final CustomStationDefinitionWriter writer;

    public CustomStationManager(JavaPlugin craftingPlugin) {
        super(owningPlugin(), new YamlLoader(craftingPlugin), "stations", "estación", new CustomStationParser());
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

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(CustomStationManager.class);
    }

}
