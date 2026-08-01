package com.sack.rpgroll.player.identity;

import java.util.UUID;

/**
 * Información de identidad de un jugador RPG.
 * 
 * Record inmutable que representa:
 * - UUID de Minecraft
 * - Nombre del jugador
 * - Raza seleccionada
 * - Clase seleccionada
 */
public record PlayerIdentity(
        UUID uuid,
        String username,
        String race,
        String playerClass
) {

    /**
     * Factory method para crear una identidad nueva.
     * Utilizado cuando un jugador entra por primera vez.
     */
    public static PlayerIdentity create(UUID uuid, String username) {
        return new PlayerIdentity(uuid, username, null, null);
    }

    /**
     * Verifica si el jugador ha completado su creación de personaje.
     * (Tiene raza Y clase seleccionadas)
     */
    public boolean isCharacterComplete() {
        return race != null && !race.isEmpty() && 
               playerClass != null && !playerClass.isEmpty();
    }

}
