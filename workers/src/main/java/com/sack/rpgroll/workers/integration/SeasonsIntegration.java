package com.sack.rpgroll.workers.integration;

import com.sack.rpgroll.seasons.api.SeasonsAPI;

import org.bukkit.Location;

/**
 * Puente blando con RPGRoll-Seasons (softdepend). Los horarios de
 * Workers ya comparten el mismo reloj que Seasons ({@code
 * World#getTime()}), así que no hace falta traducir nada ahí — esta
 * clase solo expone la estación actual, por si un futuro
 * {@code ProfessionBehavior} custom quiere reaccionar a ella (ej. un
 * granjero que no siembra en invierno).
 */
public final class SeasonsIntegration {

    private SeasonsIntegration() {
    }

    public static String currentSeasonId(Location location) {

        if (!SeasonsAPI.isReady()) {
            return null;
        }

        return SeasonsAPI.get().getCurrentSeason(location).map(season -> season.id()).orElse(null);
    }

}
