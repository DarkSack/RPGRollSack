package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class SpellManager extends ContentManager<Spell> {

    private final SpellDefinitionWriter writer;

    public SpellManager(JavaPlugin magicPlugin) {
        super(owningPlugin(), new YamlLoader(magicPlugin), "spells", "hechizo", new SpellParser());
        this.writer = new SpellDefinitionWriter(magicPlugin.getDataFolder());
    }

    public void save(Spell spell) {
        writer.save(spell);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(SpellManager.class);
    }

}
