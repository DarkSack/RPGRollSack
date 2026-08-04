package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class CatalystManager extends ContentManager<SpellCatalyst> {

    private final CatalystDefinitionWriter writer;

    public CatalystManager(JavaPlugin magicPlugin) {
        super(resolveCoreInstance(), new YamlLoader(magicPlugin), "catalysts", "catalizador", new CatalystParser());
        this.writer = new CatalystDefinitionWriter(magicPlugin.getDataFolder());
    }

    public void save(SpellCatalyst catalyst) {
        writer.save(catalyst);
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
