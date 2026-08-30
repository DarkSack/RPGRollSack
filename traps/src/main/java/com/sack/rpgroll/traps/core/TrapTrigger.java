package com.sack.rpgroll.traps.core;

/**
 * Cómo se dispara una trampa. Los primeros ocho son dirigidos por evento
 * (un listener de Bukkit los detecta al instante); PROXIMITY, LINE_OF_SIGHT
 * y TIMER no tienen evento vanilla equivalente y se re-evalúan en el tick
 * periódico del motor ({@code TrapEngine}); COMMAND y CUSTOM_EVENT nunca se
 * disparan solos — solo vía {@code /trapadmin forcetrigger} o la acción
 * TRIGGER_TRAP de otra trampa/otro addon, para que un admin o un sistema
 * externo pueda forzar el disparo sin depender de un evento real.
 */
public enum TrapTrigger {
    PRESSURE,
    MOVEMENT,
    PROXIMITY,
    LINE_OF_SIGHT,
    INTERACTION,
    BLOCK_BREAK,
    BLOCK_PLACE,
    PROJECTILE,
    ENTITY,
    REDSTONE,
    TIMER,
    COMMAND,
    CUSTOM_EVENT;

    /** Triggers que el motor debe re-evaluar en su tick periódico en vez de esperar un evento. */
    public boolean isPolled() {
        return this == PROXIMITY || this == LINE_OF_SIGHT || this == TIMER;
    }

}
