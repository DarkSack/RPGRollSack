package com.sack.rpgroll.chat.player;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerChannelStateStore {

    private final Plugin plugin;
    private final File folder;

    public PlayerChannelStateStore(Plugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "channeldata");

        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public PlayerChannelState load(UUID uuid) {

        PlayerChannelState state = new PlayerChannelState(uuid);
        File file = new File(folder, uuid + ".yml");

        if (!file.exists()) {
            return state;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Set<String> joined = new LinkedHashSet<>(config.getStringList("joined"));
        state.restore(joined, config.getString("active", null));

        return state;
    }

    public void save(PlayerChannelState state) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("joined", new java.util.ArrayList<>(state.joinedChannelIds()));
        config.set("active", state.activeChannelId());

        try {
            config.save(new File(folder, state.uuid() + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error guardando canales de " + state.uuid() + ": " + e.getMessage());
        }
    }

}
