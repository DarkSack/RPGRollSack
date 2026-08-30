package com.sack.rpgroll.traps.engine;

import org.bukkit.entity.Player;

/**
 * Quién/qué disparó el intento de activación. {@code triggeringPlayer}
 * puede ser null (ej. TIMER, o REDSTONE activado sin nadie cerca) — las
 * acciones y condiciones que dependen de un jugador simplemente no aplican
 * en ese caso.
 */
public record TriggerContext(Player triggeringPlayer) {

    public static TriggerContext of(Player player) {
        return new TriggerContext(player);
    }

    public static TriggerContext none() {
        return new TriggerContext(null);
    }

}
