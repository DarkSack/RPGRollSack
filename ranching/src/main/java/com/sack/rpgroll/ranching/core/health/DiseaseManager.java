package com.sack.rpgroll.ranching.core.health;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class DiseaseManager extends ContentManager<Disease> {

    private final DiseaseDefinitionWriter writer;

    public DiseaseManager(JavaPlugin ranchingPlugin) {
        super(owningPlugin(), new YamlLoader(ranchingPlugin), "diseases", "enfermedad", new DiseaseParser());
        this.writer = new DiseaseDefinitionWriter(ranchingPlugin.getDataFolder());
    }

    public void save(Disease disease) {
        writer.save(disease);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(DiseaseManager.class);
    }

}
