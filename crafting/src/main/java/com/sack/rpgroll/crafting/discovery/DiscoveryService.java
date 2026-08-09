package com.sack.rpgroll.crafting.discovery;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Servicio de descubrimiento de recetas: cachea en memoria y persiste un
 * archivo plano por jugador ({@code discoveries/<uuid>.yml}), mismo patrón
 * que el resto del ecosistema para datos por-jugador.
 */
public class DiscoveryService {

    private final File folder;
    private final Logger logger;
    private final Map<UUID, PlayerDiscovery> cache = new HashMap<>();

    public DiscoveryService(File dataFolder, Logger logger) {
        this.folder = new File(dataFolder, "discoveries");
        this.logger = logger;
    }

    public PlayerDiscovery get(UUID playerId) {
        return cache.computeIfAbsent(playerId, this::load);
    }

    /** @return true si esta receta era nueva para el jugador (útil para avisarle en el chat). */
    public boolean markDiscovered(UUID playerId, String recipeId) {

        PlayerDiscovery discovery = get(playerId);
        boolean isNew = discovery.markDiscovered(recipeId);

        if (isNew) {
            save(discovery);
        }

        return isNew;
    }

    private PlayerDiscovery load(UUID playerId) {

        File file = new File(folder, playerId + ".yml");
        if (!file.exists()) {
            return PlayerDiscovery.empty(playerId);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Set<String> ids = new LinkedHashSet<>(config.getStringList("discovered"));
        return new PlayerDiscovery(playerId, ids);
    }

    private void save(PlayerDiscovery discovery) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("discovered", discovery.discoveredRecipeIds().stream().toList());

        try {
            folder.mkdirs();
            config.save(new File(folder, discovery.playerId() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ No se pudo guardar el descubrimiento de recetas de " + discovery.playerId() + ": "
                    + e.getMessage());
        }
    }

}
