package com.sack.rpgroll.ranching.core.health;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class DiseaseManager extends ContentManager<Disease> {

    private final DiseaseDefinitionWriter writer;

    public DiseaseManager(JavaPlugin ranchingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(ranchingPlugin), "diseases", "enfermedad", new DiseaseParser());
        this.writer = new DiseaseDefinitionWriter(ranchingPlugin.getDataFolder());
    }

    public void save(Disease disease) {
        writer.save(disease);
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
