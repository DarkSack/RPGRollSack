package com.sack.rpgroll.fishing.core;

/**
 * Clima requerido para que un pez pique. No incluye niebla — Bukkit no
 * expone ningún estado de niebla real consultable, así que un pez que la
 * requiriera nunca podría pescarse.
 */
public enum WeatherType {
    SUNNY,
    RAIN,
    STORM,
    SNOW
}
