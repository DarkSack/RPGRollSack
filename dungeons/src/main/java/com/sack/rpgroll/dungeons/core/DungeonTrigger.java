package com.sack.rpgroll.dungeons.core;

/** Eventos de una corrida de dungeon en los que se pueden enganchar {@link DungeonAction}. */
public enum DungeonTrigger {
    DUNGEON_START,
    DUNGEON_COMPLETE,
    DUNGEON_FAIL,
    DUNGEON_ABANDON,
    ROOM_ENTER,
    ROOM_CLEAR,
    WAVE_START,
    WAVE_CLEAR,
    BOSS_SPAWN,
    BOSS_DEFEATED,
    PLAYER_DEATH,
    PLAYER_REVIVE
}
