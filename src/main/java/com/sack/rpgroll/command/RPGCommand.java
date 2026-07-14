package com.sack.rpgroll.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Interfaz base para todos los subcomandos de RPGRoll.
 */
public interface RPGCommand {

    /**
     * Ejecuta el subcomando.
     *
     * @param sender quien ejecutó el comando
     * @param args   argumentos del comando
     */
    void execute(CommandSender sender, String[] args);

    /**
     * Nombre del subcomando.
     *
     * @return nombre (ej: "stats", "level")
     */
    String getName();

    /**
     * Descripción del subcomando.
     *
     * @return descripción breve
     */
    String getDescription();

    /**
     * Uso del comando.
     *
     * @return string de uso (ej: "/rpg stats")
     */
    String getUsage();

    /**
     * Permiso requerido para usar el comando.
     *
     * @return string del permiso, o null si no requiere
     */
    default String getPermission() {
        return null;
    }

    /**
     * Aliases del comando.
     *
     * @return lista de aliases
     */
    default List<String> getAliases() {
        return List.of();
    }

    /**
     * Si el comando solo puede ser usado por jugadores.
     *
     * @return true si requiere ser jugador
     */
    default boolean isPlayerOnly() {
        return true;
    }

}
