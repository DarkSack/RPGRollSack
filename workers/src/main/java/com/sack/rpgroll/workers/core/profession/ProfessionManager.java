package com.sack.rpgroll.workers.core.profession;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ProfessionManager extends ContentManager<Profession> {

    private final ProfessionDefinitionWriter writer;

    public ProfessionManager(JavaPlugin workersPlugin) {
        super(resolveCoreInstance(), new YamlLoader(workersPlugin), "professions", "profesión", new ProfessionParser());
        this.writer = new ProfessionDefinitionWriter(workersPlugin.getDataFolder());
    }

    public void save(Profession profession) {
        writer.save(profession);
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
