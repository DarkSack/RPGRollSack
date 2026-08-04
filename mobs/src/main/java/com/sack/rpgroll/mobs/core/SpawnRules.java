package com.sack.rpgroll.mobs.core;

import java.util.List;

/** Reglas de aparición: dónde, cuándo y con qué peso puede spawnear este mob. */
public record SpawnRules(
        List<String> biomes,
        List<String> regions,
        List<String> worlds,
        int hourMin,
        int hourMax,
        String weather,
        int minHeight,
        int maxHeight,
        double minDistanceFromPlayers,
        boolean naturalSpawn,
        double spawnWeight) {

    public SpawnRules {
        biomes = biomes == null ? List.of() : List.copyOf(biomes);
        regions = regions == null ? List.of() : List.copyOf(regions);
        worlds = worlds == null ? List.of() : List.copyOf(worlds);
        hourMin = hourMin < 0 ? -1 : hourMin;
        hourMax = hourMax < 0 ? -1 : hourMax;
        spawnWeight = spawnWeight <= 0 ? 1.0 : spawnWeight;
    }

    public static SpawnRules none() {
        return new SpawnRules(List.of(), List.of(), List.of(), -1, -1, null, -64, 320, 0, false, 1.0);
    }

    public boolean hasTimeRange() {
        return hourMin >= 0 && hourMax >= 0;
    }

}
