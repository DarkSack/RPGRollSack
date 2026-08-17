package com.sack.rpgroll.crafting.proficiency;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * El "sistema de habilidades" propio de RPGRoll-Crafting: cachea en memoria y
 * persiste un archivo plano por jugador ({@code proficiency/<uuid>.yml}),
 * mismo patrón que {@code DiscoveryService}. Es el reemplazo real del
 * {@code skillFactor} que antes era una constante en {@code StationProcessingEngine}.
 */
public class ProficiencyService {

    private final File folder;
    private final Logger logger;
    private final Map<UUID, PlayerProficiency> cache = new HashMap<>();

    public ProficiencyService(File dataFolder, Logger logger) {
        this.folder = new File(dataFolder, "proficiency");
        this.logger = logger;
    }

    public PlayerProficiency get(UUID playerId) {
        return cache.computeIfAbsent(playerId, this::load);
    }

    /** @return el xp total de la categoría después de sumar (0 si amount es 0/negativo). */
    public double grantXp(UUID playerId, String category, double amount) {

        if (playerId == null || category == null || amount <= 0) {
            return playerId != null ? get(playerId).xp(category) : 0;
        }

        PlayerProficiency proficiency = get(playerId);
        double updated = proficiency.addXp(category, amount);
        save(proficiency);
        return updated;
    }

    public int level(UUID playerId, String category) {
        return ProficiencyLevelCurve.levelFor(get(playerId).xp(category));
    }

    /** Factor 0-1 para alimentar el roll de calidad — 0 si el jugador es null (offline sin historial cacheado aún). */
    public double factor(UUID playerId, String category) {
        if (playerId == null) {
            return 0;
        }
        return ProficiencyLevelCurve.factorFor(get(playerId).xp(category));
    }

    private PlayerProficiency load(UUID playerId) {

        File file = new File(folder, playerId + ".yml");
        if (!file.exists()) {
            return PlayerProficiency.empty(playerId);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<String, Double> xpByCategory = new HashMap<>();

        ConfigurationSection section = config.getConfigurationSection("xp");
        if (section != null) {
            for (String category : section.getKeys(false)) {
                xpByCategory.put(category, section.getDouble(category, 0));
            }
        }

        return new PlayerProficiency(playerId, xpByCategory);
    }

    private void save(PlayerProficiency proficiency) {

        YamlConfiguration config = new YamlConfiguration();
        proficiency.xpByCategory().forEach((category, xp) -> config.set("xp." + category, xp));

        try {
            folder.mkdirs();
            config.save(new File(folder, proficiency.playerId() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ No se pudo guardar la proficiencia de " + proficiency.playerId() + ": " + e.getMessage());
        }
    }

}
