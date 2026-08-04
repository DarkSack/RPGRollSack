package com.sack.rpgroll.dungeons.core;

public record DungeonReviveConfig(ReviveMode mode, long reviveTimerMillis, String itemMaterial) {

    public DungeonReviveConfig {
        mode = mode == null ? ReviveMode.NONE : mode;
    }

    public static DungeonReviveConfig none() {
        return new DungeonReviveConfig(ReviveMode.NONE, 0, null);
    }

}
