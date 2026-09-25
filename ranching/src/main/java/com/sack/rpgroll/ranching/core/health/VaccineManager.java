package com.sack.rpgroll.ranching.core.health;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class VaccineManager extends ContentManager<Vaccine> {

    private final VaccineDefinitionWriter writer;

    public VaccineManager(JavaPlugin ranchingPlugin) {
        super(owningPlugin(), new YamlLoader(ranchingPlugin), "vaccines", "vacuna", new VaccineParser());
        this.writer = new VaccineDefinitionWriter(ranchingPlugin.getDataFolder());
    }

    public void save(Vaccine vaccine) {
        writer.save(vaccine);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(VaccineManager.class);
    }

}
