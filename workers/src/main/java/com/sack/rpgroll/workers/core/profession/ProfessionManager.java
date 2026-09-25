package com.sack.rpgroll.workers.core.profession;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class ProfessionManager extends ContentManager<Profession> {

    private final ProfessionDefinitionWriter writer;

    public ProfessionManager(JavaPlugin workersPlugin) {
        super(owningPlugin(), new YamlLoader(workersPlugin), "professions", "profesión", new ProfessionParser());
        this.writer = new ProfessionDefinitionWriter(workersPlugin.getDataFolder());
    }

    public void save(Profession profession) {
        writer.save(profession);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(ProfessionManager.class);
    }

}
