package com.sack.rpgroll.fishing.runtime;

import com.sack.rpgroll.fishing.core.CatchQuality;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/** Persiste el {@link PlayerFishingProfile} de cada jugador en plugins/RPGRoll-Fishing/playerdata/&lt;uuid&gt;.yml. */
public class FishingProfileStore {

    private final Plugin plugin;
    private final File folder;

    public FishingProfileStore(Plugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");

        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public PlayerFishingProfile load(UUID uuid) {

        PlayerFishingProfile profile = new PlayerFishingProfile(uuid);
        File file = new File(folder, uuid + ".yml");

        if (!file.exists()) {
            return profile;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        var section = config.getConfigurationSection("records");

        if (section != null) {
            for (String speciesId : section.getKeys(false)) {

                int count = section.getInt(speciesId + ".count", 1);
                double bestWeight = section.getDouble(speciesId + ".best-weight", 0);
                double bestLength = section.getDouble(speciesId + ".best-length", 0);
                long firstCaughtAt = section.getLong(speciesId + ".first-caught-at", System.currentTimeMillis());

                CatchQuality quality;
                try {
                    quality = CatchQuality.valueOf(
                            section.getString(speciesId + ".best-quality", "COMMON").toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    quality = CatchQuality.COMMON;
                }

                profile.allRecords().put(speciesId,
                        new FishRecord(count, bestWeight, bestLength, quality, firstCaughtAt));
            }
        }

        profile.restoreTotals(config.getInt("total-caught", 0), config.getInt("total-treasures", 0),
                config.getInt("total-junk", 0));

        return profile;
    }

    public void save(PlayerFishingProfile profile) {

        YamlConfiguration config = new YamlConfiguration();

        for (var entry : profile.allRecords().entrySet()) {

            String speciesId = entry.getKey();
            FishRecord record = entry.getValue();

            config.set("records." + speciesId + ".count", record.caughtCount());
            config.set("records." + speciesId + ".best-weight", record.bestWeight());
            config.set("records." + speciesId + ".best-length", record.bestLength());
            config.set("records." + speciesId + ".best-quality", record.bestQuality().name());
            config.set("records." + speciesId + ".first-caught-at", record.firstCaughtAtMillis());
        }

        config.set("total-caught", profile.totalCaught());
        config.set("total-treasures", profile.totalTreasures());
        config.set("total-junk", profile.totalJunk());

        try {
            config.save(new File(folder, profile.uuid() + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error guardando el perfil de pesca de " + profile.uuid() + ": "
                    + e.getMessage());
        }
    }

}
