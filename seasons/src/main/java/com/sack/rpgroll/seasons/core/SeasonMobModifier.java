package com.sack.rpgroll.seasons.core;

import java.util.Objects;

/** Boost de spawn para un mob de RPGRoll-Mobs mientras esta estación está activa. @param extraSpawnChance fracción 0.0-1.0 evaluada por intento del SeasonMobSpawnTask. */
public record SeasonMobModifier(String mobId, double extraSpawnChance) {

    public SeasonMobModifier {
        Objects.requireNonNull(mobId, "mobId no puede ser null");
        extraSpawnChance = Math.max(0.0, Math.min(1.0, extraSpawnChance));
    }

}
