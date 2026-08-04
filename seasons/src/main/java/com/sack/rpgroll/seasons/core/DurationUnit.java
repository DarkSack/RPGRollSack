package com.sack.rpgroll.seasons.core;

/** En qué unidad se mide la duración de una {@link Season}/{@link SubSeason}. */
public enum DurationUnit {

    REAL_HOURS,
    REAL_DAYS,
    REAL_WEEKS,
    /** Un día de Minecraft = 24000 ticks (independiente de cuántas horas reales dure el servidor). */
    MINECRAFT_DAYS;

    private static final long TICKS_PER_SECOND = 20L;
    private static final long TICKS_PER_MINECRAFT_DAY = 24000L;

    public long toTicks(int amount) {
        return switch (this) {
            case REAL_HOURS -> amount * 60L * 60L * TICKS_PER_SECOND;
            case REAL_DAYS -> amount * 24L * 60L * 60L * TICKS_PER_SECOND;
            case REAL_WEEKS -> amount * 7L * 24L * 60L * 60L * TICKS_PER_SECOND;
            case MINECRAFT_DAYS -> amount * TICKS_PER_MINECRAFT_DAY;
        };
    }

}
