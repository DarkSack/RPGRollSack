package com.sack.rpgroll.ranching.integration;

import com.sack.rpgroll.seasons.api.SeasonsAPI;

import org.bukkit.Location;

/**
 * Puente blando con RPGRoll-Seasons (softdepend) — sin él, el clima
 * simplemente no modula nada (bienestar/fertilidad/enfermedad/producción
 * funcionan igual, solo que sin ese factor extra).
 * <p>
 * La integración es puramente por TEMPERATURA, no por id de estación:
 * evita pedirle a cada Species/Breed/Disease que declare sus propias
 * "estaciones permitidas" (como sí hace RPGRoll-Fishing con sus peces),
 * lo que habría inflado el modelo de contenido de esta pasada. El calor
 * o frío extremo ya alcanza para que el clima se sienta real en
 * bienestar/fertilidad/producción.
 */
public final class SeasonsIntegration {

    private SeasonsIntegration() {
    }

    public static Double temperature(Location location) {

        if (!SeasonsAPI.isReady()) {
            return null;
        }

        return SeasonsAPI.get().getTemperature(location);
    }

    public static String currentSeasonId(Location location) {

        if (!SeasonsAPI.isReady()) {
            return null;
        }

        return SeasonsAPI.get().getCurrentSeason(location).map(season -> season.id()).orElse(null);
    }

}
