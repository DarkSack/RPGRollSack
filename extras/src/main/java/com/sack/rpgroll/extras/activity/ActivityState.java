package com.sack.rpgroll.extras.activity;

/** Estado de actividad detectado del jugador — usado por reglas de regeneración condicional (sección 4). */
public enum ActivityState {
    COMBAT,
    SPRINTING,
    WALKING,
    RESTING
}
