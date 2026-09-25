package com.sack.rpgroll.guilds.guild.quest;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/** Carga las definiciones de quest de guild desde plugins/RPGRoll-Guilds/quests/*.yml. */
public class GuildQuestManager extends ContentManager<GuildQuestDefinition> {

    private final GuildQuestDefinitionWriter writer;

    public GuildQuestManager(JavaPlugin guildsPlugin) {
        super(owningPlugin(), new YamlLoader(guildsPlugin), "quests", "quest de guild", new GuildQuestParser());
        this.writer = new GuildQuestDefinitionWriter(new File(guildsPlugin.getDataFolder(), "quests"),
                guildsPlugin.getLogger());
    }

    /** Persiste la quest a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(GuildQuestDefinition definition) {
        writer.save(definition);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(GuildQuestManager.class);
    }

}
