package com.sack.rpgroll.crates.treasure;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Qué cofres de tesoro ya se abrieron. El primero que abre uno se lleva el botín; un segundo clic (otro
 * jugador, doble clic) no tira otra vez. Un cofre abierto se retira al cerrarlo, así que no vuelve.
 */
final class ChestClaims {

    private final Set<UUID> claimed = ConcurrentHashMap.newKeySet();

    /** @return true si es la primera vez que se abre este cofre */
    boolean claim(UUID chest) {
        return claimed.add(chest);
    }

}
