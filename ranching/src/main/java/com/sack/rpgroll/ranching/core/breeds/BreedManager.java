package com.sack.rpgroll.ranching.core.breeds;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class BreedManager extends ContentManager<Breed> {

    private final BreedDefinitionWriter writer;

    public BreedManager(JavaPlugin ranchingPlugin) {
        super(owningPlugin(), new YamlLoader(ranchingPlugin), "breeds", "raza", new BreedParser());
        this.writer = new BreedDefinitionWriter(ranchingPlugin.getDataFolder());
    }

    public void save(Breed breed) {
        writer.save(breed);
        reload();
    }

    public List<Breed> getForSpecies(String speciesId) {
        return getAll().stream().filter(breed -> breed.speciesId().equals(speciesId)).toList();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(BreedManager.class);
    }

}
