package com.sack.rpgroll.fishing.integration;

import com.sack.rpgroll.seasons.api.SeasonsAPI;

import org.bukkit.Bukkit;
import org.bukkit.Location;

/** Puente blando con RPGRoll-Seasons (softdepend) — sin él, cualquier estación filtra como "cualquiera". */
public final class SeasonsIntegration {

    private SeasonsIntegration() {
    }

    /**
     * Primero se mira si el plugin está: {@code SeasonsAPI.isReady()} ya carga la clase, y sin
     * RPGRoll-Seasons eso lanza NoClassDefFoundError.
     */
    private static boolean ready() {
        return Bukkit.getPluginManager().isPluginEnabled("RPGRoll-Seasons") && SeasonsAPI.isReady();
    }

    public static Double temperature(Location location) {
        return ready() ? SeasonsAPI.get().getTemperature(location) : null;
    }

    public static String currentSeasonId(Location location) {

        if (!ready()) {
            return null;
        }

        return SeasonsAPI.get().getCurrentSeason(location).map(season -> season.id()).orElse(null);
    }

}
