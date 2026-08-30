package com.sack.rpgroll.npcs.core;

/**
 * Una acción a ejecutar cuando un jugador interactúa con un NPC.
 * Fase 1-3: solo MESSAGE y COMMAND. GUI y SHOP se agregan en fases futuras.
 */
public record NpcAction(NpcActionType type, String value) {

    public enum NpcActionType {
        MESSAGE,
        /** Corre como CONSOLE — pensado para comandos que no necesitan al jugador logueado (/give, LuckPerms...). */
        COMMAND,
        /** Corre como el jugador que clickeó — necesario para comandos propios de RPGRoll (ej. "quest start x"), que exigen un sender jugador. */
        COMMAND_AS_PLAYER,
        GIVE_ITEM,
        TAKE_ITEM,
        SOUND,
        TELEPORT,
        CONDITIONAL,
        OPEN_GUI,
        OPEN_INVENTORY
    }
}