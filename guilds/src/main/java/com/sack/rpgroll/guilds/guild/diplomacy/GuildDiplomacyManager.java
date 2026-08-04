package com.sack.rpgroll.guilds.guild.diplomacy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Relaciones diplomáticas entre pares de guilds. Se guardan una sola vez
 * por par (no duplicadas en cada guild) para que ambos lados nunca queden
 * desincronizados.
 */
public class GuildDiplomacyManager {

    private final Plugin plugin;
    private final File file;
    private final Map<String, DiplomacyStatus> relations = new LinkedHashMap<>();

    public GuildDiplomacyManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "diplomacy.yml");
        load();
    }

    private static String key(String guildIdA, String guildIdB) {
        List<String> sorted = new java.util.ArrayList<>(List.of(guildIdA.toLowerCase(Locale.ROOT),
                guildIdB.toLowerCase(Locale.ROOT)));
        sorted.sort(String::compareTo);
        return sorted.get(0) + ":" + sorted.get(1);
    }

    public DiplomacyStatus relation(String guildIdA, String guildIdB) {
        if (guildIdA.equalsIgnoreCase(guildIdB)) {
            return DiplomacyStatus.ALLIED;
        }
        return relations.getOrDefault(key(guildIdA, guildIdB), DiplomacyStatus.NEUTRAL);
    }

    public void setRelation(String guildIdA, String guildIdB, DiplomacyStatus status) {

        if (status == DiplomacyStatus.NEUTRAL) {
            relations.remove(key(guildIdA, guildIdB));
        } else {
            relations.put(key(guildIdA, guildIdB), status);
        }

        save();
    }

    public List<String> alliesOf(String guildId) {

        List<String> allies = new java.util.ArrayList<>();

        for (var entry : relations.entrySet()) {
            if (entry.getValue() != DiplomacyStatus.ALLIED) {
                continue;
            }

            String[] parts = entry.getKey().split(":", 2);
            if (parts[0].equalsIgnoreCase(guildId)) {
                allies.add(parts[1]);
            } else if (parts[1].equalsIgnoreCase(guildId)) {
                allies.add(parts[0]);
            }
        }

        return allies;
    }

    public void removeGuild(String guildId) {

        relations.keySet().removeIf(key -> {
            String[] parts = key.split(":", 2);
            return parts[0].equalsIgnoreCase(guildId) || parts[1].equalsIgnoreCase(guildId);
        });

        save();
    }

    private void load() {

        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String key : config.getKeys(false)) {
            try {
                relations.put(key, DiplomacyStatus.valueOf(config.getString(key)));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void save() {

        YamlConfiguration config = new YamlConfiguration();

        for (var entry : relations.entrySet()) {
            config.set(entry.getKey(), entry.getValue().name());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error guardando diplomacia de guilds: " + e.getMessage());
        }
    }

}
