package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class SchoolManager extends ContentManager<MagicSchool> {

    private final SchoolDefinitionWriter writer;

    public SchoolManager(JavaPlugin magicPlugin) {
        super(resolveCoreInstance(), new YamlLoader(magicPlugin), "schools", "escuela", new SchoolParser());
        this.writer = new SchoolDefinitionWriter(magicPlugin.getDataFolder());
    }

    public void save(MagicSchool school) {
        writer.save(school);
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
