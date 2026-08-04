package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class SpellManager extends ContentManager<Spell> {

    private final SpellDefinitionWriter writer;

    public SpellManager(JavaPlugin magicPlugin) {
        super(resolveCoreInstance(), new YamlLoader(magicPlugin), "spells", "hechizo", new SpellParser());
        this.writer = new SpellDefinitionWriter(magicPlugin.getDataFolder());
    }

    public void save(Spell spell) {
        writer.save(spell);
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
