package com.sack.rpgroll.npcs.core;

/**
 * Una acción a ejecutar cuando un jugador interactúa con un NPC.
 * Fase 1-3: solo MESSAGE y COMMAND. GUI y SHOP se agregan en fases futuras.
 */
public record NpcAction(NpcActionType type, String value) {

    public enum NpcActionType {
        MESSAGE,
        COMMAND,
        GIVE_ITEM,
        TAKE_ITEM,
        SOUND,
        TELEPORT,
        CONDITIONAL,
        OPEN_GUI,
        OPEN_INVENTORY
    }
}