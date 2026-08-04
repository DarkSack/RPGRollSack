package com.sack.rpgroll.dungeons.player;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Persiste el {@link DungeonPlayerState} de cada jugador en su propio
 * archivo plugins/RPGRoll-Dungeons/playerdata/&lt;uuid&gt;.yml — igual que
 * Quests, autocontenido, sin depender de la base SQLite de :core.
 */
public class DungeonPlayerStateStore {

    private final Plugin plugin;
    private final File folder;

    public DungeonPlayerStateStore(Plugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");

        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public DungeonPlayerState load(UUID uuid) {

        DungeonPlayerState state = new DungeonPlayerState(uuid);
        File file = new File(folder, uuid + ".yml");

        if (!file.exists()) {
            return state;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        var section = config.getConfigurationSection("completed-at");

        if (section != null) {
            for (String dungeonId : section.getKeys(false)) {
                state.restore(dungeonId, section.getLong(dungeonId));
            }
        }

        return state;
    }

    public void save(DungeonPlayerState state) {

        YamlConfiguration config = new YamlConfiguration();

        for (var entry : state.allCompletions().entrySet()) {
            config.set("completed-at." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(new File(folder, state.uuid() + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error guardando cooldowns de dungeon de " + state.uuid() + ": "
                    + e.getMessage());
        }
    }

}
