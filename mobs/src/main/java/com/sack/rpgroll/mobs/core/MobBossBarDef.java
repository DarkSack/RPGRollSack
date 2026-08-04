package com.sack.rpgroll.mobs.core;

/** Configuración de la bossbar mostrada mientras el mob está en combate. */
public record MobBossBarDef(boolean enabled, String color, String style, String title) {

    public static MobBossBarDef disabled() {
        return new MobBossBarDef(false, "RED", "SOLID", null);
    }

}
