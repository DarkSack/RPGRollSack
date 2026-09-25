package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class TitleManager extends ContentManager<Title> {

    private final TitleDefinitionWriter writer;

    public TitleManager(JavaPlugin ascensionPlugin) {
        super(owningPlugin(), new YamlLoader(ascensionPlugin), "titles", "título", new TitleParser());
        this.writer = new TitleDefinitionWriter(ascensionPlugin.getDataFolder());
    }

    public void save(Title title) {
        writer.save(title);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(TitleManager.class);
    }

}
