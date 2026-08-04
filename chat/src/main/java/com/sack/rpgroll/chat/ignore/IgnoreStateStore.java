package com.sack.rpgroll.chat.ignore;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Persiste en plugins/RPGRoll-Chat/ignoredata/&lt;uuid&gt;.yml. */
public class IgnoreStateStore {

    private final Plugin plugin;
    private final File folder;

    public IgnoreStateStore(Plugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "ignoredata");

        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public PlayerIgnoreState load(UUID uuid) {

        PlayerIgnoreState state = new PlayerIgnoreState(uuid);
        File file = new File(folder, uuid + ".yml");

        if (!file.exists()) {
            return state;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        Set<UUID> players = config.getStringList("players").stream()
                .map(UUID::fromString)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        state.restore(players, new LinkedHashSet<>(config.getStringList("guilds")),
                new LinkedHashSet<>(config.getStringList("channels")));

        return state;
    }

    public void save(PlayerIgnoreState state) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("players", state.ignoredPlayers().stream().map(UUID::toString).toList());
        config.set("guilds", new java.util.ArrayList<>(state.ignoredGuilds()));
        config.set("channels", new java.util.ArrayList<>(state.ignoredChannels()));

        try {
            config.save(new File(folder, state.uuid() + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error guardando ignorados de " + state.uuid() + ": " + e.getMessage());
        }
    }

}
