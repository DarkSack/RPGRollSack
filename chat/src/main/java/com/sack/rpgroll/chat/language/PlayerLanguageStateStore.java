package com.sack.rpgroll.chat.language;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Persiste en plugins/RPGRoll-Chat/languagedata/&lt;uuid&gt;.yml qué idiomas conoce cada jugador. */
public class PlayerLanguageStateStore {

    private final Plugin plugin;
    private final File folder;

    public PlayerLanguageStateStore(Plugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "languagedata");

        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public PlayerLanguageState load(UUID uuid) {

        PlayerLanguageState state = new PlayerLanguageState(uuid);
        File file = new File(folder, uuid + ".yml");

        if (!file.exists()) {
            return state;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Set<String> known = new LinkedHashSet<>(config.getStringList("known"));

        state.restore(known, config.getString("speaking", null), config.getBoolean("seeded", false));

        return state;
    }

    public void save(PlayerLanguageState state) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("known", new java.util.ArrayList<>(state.knownLanguageIds()));
        config.set("speaking", state.speakingLanguageId());
        config.set("seeded", state.seeded());

        try {
            config.save(new File(folder, state.uuid() + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error guardando idiomas de " + state.uuid() + ": " + e.getMessage());
        }
    }

}
