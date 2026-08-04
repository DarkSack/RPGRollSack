package com.sack.rpgroll.ranching.core.genetics;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class GeneManager extends ContentManager<Gene> {

    private final GeneDefinitionWriter writer;

    public GeneManager(JavaPlugin ranchingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(ranchingPlugin), "genes", "gen", new GeneParser());
        this.writer = new GeneDefinitionWriter(ranchingPlugin.getDataFolder());
    }

    public void save(Gene gene) {
        writer.save(gene);
        reload();
    }

    /** Genes que aplican a una especie dada — usado por el motor de genética al concebir una cría. */
    public List<Gene> getForSpecies(String speciesId) {
        return getAll().stream().filter(gene -> gene.appliesTo(speciesId)).toList();
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
