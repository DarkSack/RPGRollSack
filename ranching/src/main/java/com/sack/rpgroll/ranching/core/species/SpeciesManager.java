package com.sack.rpgroll.ranching.core.species;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class SpeciesManager extends ContentManager<Species> {

    private final SpeciesDefinitionWriter writer;

    public SpeciesManager(JavaPlugin ranchingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(ranchingPlugin), "species", "especie", new SpeciesParser());
        this.writer = new SpeciesDefinitionWriter(ranchingPlugin.getDataFolder());
    }

    public void save(Species species) {
        writer.save(species);
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
