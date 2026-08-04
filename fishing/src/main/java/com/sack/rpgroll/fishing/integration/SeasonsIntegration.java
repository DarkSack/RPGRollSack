package com.sack.rpgroll.fishing.integration;

import com.sack.rpgroll.seasons.api.SeasonsAPI;

import org.bukkit.Location;

/** Puente blando con RPGRoll-Seasons (softdepend) — sin él, cualquier estación filtra como "cualquiera". */
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
