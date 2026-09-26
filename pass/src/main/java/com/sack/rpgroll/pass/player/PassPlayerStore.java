package com.sack.rpgroll.pass.player;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Un YAML por jugador en {@code data/players/<uuid>.yml}, cargado al entrar y guardado al salir. */
public class PassPlayerStore {

    private final File folder;
    private final Logger logger;
    private final Map<UUID, PassPlayer> loaded = new ConcurrentHashMap<>();

    public PassPlayerStore(File dataFolder, Logger logger) {
        this.folder = new File(dataFolder, "data/players");
        this.logger = logger;
    }

    public PassPlayer get(UUID uuid) {
        return loaded.computeIfAbsent(uuid, this::read);
    }

    public Collection<PassPlayer> loaded() {
        return loaded.values();
    }

    public void unload(UUID uuid) {
        PassPlayer player = loaded.remove(uuid);
        if (player != null) {
            save(player);
        }
    }

    public void saveDirty() {
        for (PassPlayer player : loaded.values()) {
            if (player.isDirty()) {
                save(player);
            }
        }
    }

    public void saveAll() {
        loaded.values().forEach(this::save);
    }

    public void save(PassPlayer player) {

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("season.id", player.seasonId);
        yaml.set("season.xp", player.xp);
        yaml.set("season.claimed-free", new ArrayList<>(player.claimedFree));
        yaml.set("season.claimed-premium", new ArrayList<>(player.claimedPremium));
        yaml.set("season.playtime-minutes", player.seasonPlaytime);
        yaml.set("season.missions-completed", player.seasonMissions);
        yaml.set("missions.daily-day", player.dailyMissionDay);
        yaml.set("missions.daily", player.dailyMissions);
        yaml.set("missions.weekly-key", player.weeklyKey);
        yaml.set("missions.weekly", player.weeklyMissions);
        player.progress.forEach((id, value) -> yaml.set("missions.progress." + id, value));
        yaml.set("missions.completed", new ArrayList<>(player.completed));
        yaml.set("daily.last-claim-day", player.lastDailyClaimDay);
        yaml.set("daily.streak", player.dailyStreak);
        yaml.set("votes.total", player.votesTotal);
        yaml.set("votes.last-day", player.lastVoteDay);
        yaml.set("votes.streak", player.voteStreak);

        try {
            folder.mkdirs();
            yaml.save(file(player.uuid()));
            player.markClean();
        } catch (IOException e) {
            logger.log(Level.WARNING, "No se pudo guardar el pase de " + player.uuid(), e);
        }
    }

    private PassPlayer read(UUID uuid) {

        PassPlayer player = new PassPlayer(uuid);
        File file = file(uuid);

        if (!file.isFile()) {
            return player;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        player.seasonId = yaml.getString("season.id", "");
        player.xp = yaml.getInt("season.xp");
        player.claimedFree.addAll(yaml.getIntegerList("season.claimed-free"));
        player.claimedPremium.addAll(yaml.getIntegerList("season.claimed-premium"));
        player.seasonPlaytime = yaml.getInt("season.playtime-minutes");
        player.seasonMissions = yaml.getInt("season.missions-completed");
        player.dailyMissionDay = yaml.getLong("missions.daily-day", Long.MIN_VALUE);
        player.dailyMissions.addAll(yaml.getStringList("missions.daily"));
        player.weeklyKey = yaml.getString("missions.weekly-key", "");
        player.weeklyMissions.addAll(yaml.getStringList("missions.weekly"));

        ConfigurationSection progress = yaml.getConfigurationSection("missions.progress");
        if (progress != null) {
            for (String id : progress.getKeys(false)) {
                player.progress.put(id, progress.getInt(id));
            }
        }

        player.completed.addAll(yaml.getStringList("missions.completed"));
        player.lastDailyClaimDay = yaml.getLong("daily.last-claim-day", Long.MIN_VALUE);
        player.dailyStreak = yaml.getInt("daily.streak");
        player.votesTotal = yaml.getInt("votes.total");
        player.lastVoteDay = yaml.getLong("votes.last-day", Long.MIN_VALUE);
        player.voteStreak = yaml.getInt("votes.streak");

        return player;
    }

    private File file(UUID uuid) {
        return new File(folder, uuid + ".yml");
    }

}
