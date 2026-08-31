package com.sack.rpgroll.common.command;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

/**
 * Resolución de quién ejecuta realmente un comando.
 * <p>
 * Existe por {@code execute as}: cuando un bloque de comandos, un datapack o
 * la consola corren {@code execute as <jugador> run /rpg stats}, Bukkit no
 * entrega al jugador sino un {@link ProxiedCommandSender} que lo envuelve. Un
 * {@code sender instanceof Player} devuelve false y el comando se rechaza con
 * "solo jugadores", aunque haya un jugador clarísimo detrás.
 * <p>
 * Eso dejaba fuera a {@code execute as @a run <comando>}, que es la forma
 * estándar de automatizar cualquier cosa en un servidor.
 */
public final class Senders {

    private Senders() {
    }

    /**
     * El jugador detrás de un sender, desenvolviendo {@code execute as}.
     *
     * @return el jugador, o null si de verdad no hay ninguno (consola, RCON,
     *         bloque de comandos sin {@code as})
     */
    public static Player asPlayer(CommandSender sender) {

        if (sender instanceof Player player) {
            return player;
        }

        if (sender instanceof ProxiedCommandSender proxied && proxied.getCallee() instanceof Player player) {
            return player;
        }

        return null;
    }

    /** ¿Hay un jugador real detrás de este sender? */
    public static boolean isPlayer(CommandSender sender) {
        return asPlayer(sender) != null;
    }

}
