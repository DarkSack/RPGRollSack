package com.sack.rpgroll.common.menu;

/**
 * Una acción de un menú YAML (o de un NPC): qué hacer y con qué valor.
 * Vive en RPGRoll-Lib para que cualquier módulo pueda abrir menús sin
 * depender de RPGRoll-NPCs.
 */
public record MenuAction(ActionType type, String value) {

    public enum ActionType {
        /** Mensaje al jugador. {player} se reemplaza. */
        MESSAGE,
        /** Corre como CONSOLE — pensado para comandos que no necesitan al jugador logueado (/give, LuckPerms...). */
        COMMAND,
        /** Corre como el jugador que clickeó — necesario para comandos propios de RPGRoll (ej. "quest start x"), que exigen un sender jugador. */
        COMMAND_AS_PLAYER,
        /** MATERIAL[,cantidad] */
        GIVE_ITEM,
        /** MATERIAL[,cantidad] */
        TAKE_ITEM,
        /** SONIDO[,volumen[,tono]] */
        SOUND,
        /** mundo,x,y,z[,yaw,pitch] */
        TELEPORT,
        /** condicion;accionSiTrue[;accionSiFalse] — ver {@link MenuCondition}. */
        CONDITIONAL,
        /** Abre otro menú por su id. */
        OPEN_GUI,
        /** Contenedor vanilla: CHEST[,titulo], HOPPER o FURNACE. */
        OPEN_INVENTORY,
        /** Cierra el inventario abierto. */
        CLOSE
    }

}
