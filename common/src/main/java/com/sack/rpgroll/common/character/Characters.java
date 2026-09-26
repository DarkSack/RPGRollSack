package com.sack.rpgroll.common.character;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.util.Optional;

/** Acceso a {@link RPGCharacters}: presente solo si el core de RPGRoll está instalado y activo. */
public final class Characters {

    private Characters() {
    }

    public static Optional<RPGCharacters> get() {

        if (Bukkit.getServer() == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(Bukkit.getServicesManager().load(RPGCharacters.class));
    }

    /** Lo llama el core al arrancar. Bukkit lo da de baja solo cuando el core se deshabilita. */
    public static void register(Plugin owner, RPGCharacters characters) {
        Bukkit.getServicesManager().register(RPGCharacters.class, characters, owner, ServicePriority.Normal);
    }

}
