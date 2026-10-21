package com.sack.rpgroll.disasters.integration;

import com.sack.rpgroll.seasons.api.SeasonsAPI;

import org.bukkit.Bukkit;
import org.bukkit.Location;

/** Puente blando con RPGRoll-Seasons (softdepend): sin él no se filtra por estación. */
public final class SeasonsIntegration {

    private SeasonsIntegration() {
    }

    /**
     * Primero se mira si el plugin está: {@code SeasonsAPI.isReady()} ya carga la clase, y sin
     * RPGRoll-Seasons eso lanza NoClassDefFoundError.
     */
    public static boolean ready() {
        return Bukkit.getPluginManager().isPluginEnabled("RPGRoll-Seasons") && SeasonsAPI.isReady();
    }

    /** La estación en ese sitio; null sin Seasons o en un mundo sin estaciones. */
    public static String currentSeasonId(Location location) {
        return ready() ? SeasonsAPI.get().getCurrentSeason(location).map(season -> season.id()).orElse(null) : null;
    }

}
