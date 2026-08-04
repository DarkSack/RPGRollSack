package com.sack.rpgroll.chat.channel;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Comparator;
import java.util.List;

/** Carga los canales desde plugins/RPGRoll-Chat/channels/*.yml y permite editarlos en caliente. */
public class ChannelManager extends ContentManager<ChatChannel> {

    private final ChannelDefinitionWriter writer;

    public ChannelManager(JavaPlugin chatPlugin) {
        super(resolveCoreInstance(), new YamlLoader(chatPlugin), "channels", "canal", new ChannelParser());
        this.writer = new ChannelDefinitionWriter(new File(chatPlugin.getDataFolder(), "channels"),
                chatPlugin.getLogger());
    }

    /** Persiste el canal a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(ChatChannel channel) {
        writer.save(channel);
        reload();
    }

    public List<ChatChannel> sortedByPriority() {
        return getAll().stream()
                .sorted(Comparator.comparingInt(ChatChannel::priority).reversed())
                .toList();
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
