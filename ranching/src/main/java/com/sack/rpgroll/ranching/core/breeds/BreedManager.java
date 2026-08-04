package com.sack.rpgroll.ranching.core.breeds;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class BreedManager extends ContentManager<Breed> {

    private final BreedDefinitionWriter writer;

    public BreedManager(JavaPlugin ranchingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(ranchingPlugin), "breeds", "raza", new BreedParser());
        this.writer = new BreedDefinitionWriter(ranchingPlugin.getDataFolder());
    }

    public void save(Breed breed) {
        writer.save(breed);
        reload();
    }

    public List<Breed> getForSpecies(String speciesId) {
        return getAll().stream().filter(breed -> breed.speciesId().equals(speciesId)).toList();
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
