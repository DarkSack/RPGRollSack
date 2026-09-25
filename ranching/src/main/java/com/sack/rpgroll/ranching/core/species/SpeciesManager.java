package com.sack.rpgroll.ranching.core.species;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class SpeciesManager extends ContentManager<Species> {

    private final SpeciesDefinitionWriter writer;

    public SpeciesManager(JavaPlugin ranchingPlugin) {
        super(owningPlugin(), new YamlLoader(ranchingPlugin), "species", "especie", new SpeciesParser());
        this.writer = new SpeciesDefinitionWriter(ranchingPlugin.getDataFolder());
    }

    public void save(Species species) {
        writer.save(species);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(SpeciesManager.class);
    }

}
