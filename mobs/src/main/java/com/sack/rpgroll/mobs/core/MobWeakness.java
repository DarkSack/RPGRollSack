package com.sack.rpgroll.mobs.core;

/** Un tipo de daño extra que el mob recibe (ej. +50% si te pegan con fuego, o si eres clase mago). */
public record MobWeakness(MobWeaknessType type, String key, double extraDamagePercent) {
}
