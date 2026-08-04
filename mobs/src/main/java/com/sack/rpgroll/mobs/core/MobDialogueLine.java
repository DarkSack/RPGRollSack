package com.sack.rpgroll.mobs.core;

/**
 * Una línea de diálogo del jefe, asociada a un trigger (ej. SPAWN, DEATH)
 * o a una fase específica ({@code phaseId}, se dispara al entrar a esa
 * fase — tiene prioridad sobre {@code trigger} si ambos están definidos).
 */
public record MobDialogueLine(MobTrigger trigger, String phaseId, String text) {
}
