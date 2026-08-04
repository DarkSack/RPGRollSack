package com.sack.rpgroll.chat.emote;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/** Carga las emotes desde plugins/RPGRoll-Chat/emotes/*.yml. */
public class EmoteManager extends ContentManager<EmoteDefinition> {

    private final EmoteDefinitionWriter writer;

    public EmoteManager(JavaPlugin chatPlugin) {
        super(resolveCoreInstance(), new YamlLoader(chatPlugin), "emotes", "emote", new EmoteParser());
        this.writer = new EmoteDefinitionWriter(new File(chatPlugin.getDataFolder(), "emotes"),
                chatPlugin.getLogger());
    }

    /** Persiste la emote a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(EmoteDefinition emote) {
        writer.save(emote);
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
