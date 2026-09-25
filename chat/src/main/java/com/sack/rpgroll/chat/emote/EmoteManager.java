package com.sack.rpgroll.chat.emote;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/** Carga las emotes desde plugins/RPGRoll-Chat/emotes/*.yml. */
public class EmoteManager extends ContentManager<EmoteDefinition> {

    private final EmoteDefinitionWriter writer;

    public EmoteManager(JavaPlugin chatPlugin) {
        super(owningPlugin(), new YamlLoader(chatPlugin), "emotes", "emote", new EmoteParser());
        this.writer = new EmoteDefinitionWriter(new File(chatPlugin.getDataFolder(), "emotes"),
                chatPlugin.getLogger());
    }

    /** Persiste la emote a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(EmoteDefinition emote) {
        writer.save(emote);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(EmoteManager.class);
    }

}
