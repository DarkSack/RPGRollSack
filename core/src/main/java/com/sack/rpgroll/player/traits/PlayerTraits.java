package com.sack.rpgroll.player.traits;

import java.util.*;

/**
 * Record que representa los traits adquiridos por un jugador.
 */
public record PlayerTraits(Set<String> acquiredTraits) {

    /**
     * Crea PlayerTraits vacío (sin traits).
     */
    public static PlayerTraits empty() {
        return new PlayerTraits(new HashSet<>());
    }

    /**
     * Verifica si el jugador tiene un trait específico.
     */
    public boolean hasTrait(String traitId) {
        return acquiredTraits.contains(traitId);
    }

    /**
     * Adquiere un nuevo trait.
     */
    public PlayerTraits acquire(String traitId) {
        Set<String> updated = new HashSet<>(acquiredTraits);
        updated.add(traitId);
        return new PlayerTraits(updated);
    }

    /**
     * Obtiene todos los traits adquiridos.
     */
    public Set<String> getTraitIds() {
        return new HashSet<>(acquiredTraits);
    }

    /**
     * Obtiene el número de traits adquiridos.
     */
    public int count() {
        return acquiredTraits.size();
    }

}
