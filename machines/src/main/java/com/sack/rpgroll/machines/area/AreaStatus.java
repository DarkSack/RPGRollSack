package com.sack.rpgroll.machines.area;

/** Lo que está haciendo una máquina de área (lo pone el trabajador en cada tick). */
public enum AreaStatus {
    WORKING, IDLE, OFF, NO_OWNER, NO_CHEST, NO_CLAIM, FULL, WAITING_CHUNK, NO_WATER
}
