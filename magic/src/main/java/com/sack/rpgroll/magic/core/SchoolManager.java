package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class SchoolManager extends ContentManager<MagicSchool> {

    private final SchoolDefinitionWriter writer;

    public SchoolManager(JavaPlugin magicPlugin) {
        super(owningPlugin(), new YamlLoader(magicPlugin), "schools", "escuela", new SchoolParser());
        this.writer = new SchoolDefinitionWriter(magicPlugin.getDataFolder());
    }

    public void save(MagicSchool school) {
        writer.save(school);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(SchoolManager.class);
    }

}
