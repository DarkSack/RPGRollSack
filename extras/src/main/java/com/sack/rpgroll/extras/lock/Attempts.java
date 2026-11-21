package com.sack.rpgroll.extras.lock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Los fallos al teclear un código. Tras {@code maxFails} seguidos el jugador no puede teclear durante
 * {@code penaltyMillis}: así nadie prueba los 10 000 códigos. Vive en memoria; reiniciar lo olvida.
 */
final class Attempts {

    private final int maxFails;
    private final long penaltyMillis;
    private final Map<UUID, Integer> fails = new HashMap<>();
    private final Map<UUID, Long> blockedUntil = new HashMap<>();

    Attempts(int maxFails, long penaltyMillis) {
        this.maxFails = Math.max(1, maxFails);
        this.penaltyMillis = Math.max(0, penaltyMillis);
    }

    /** Milisegundos que le quedan sin poder teclear; 0 si puede. */
    long blockedFor(UUID player, long now) {
        Long until = blockedUntil.get(player);
        if (until == null) {
            return 0;
        }
        if (now >= until) {
            blockedUntil.remove(player);
            return 0;
        }
        return until - now;
    }

    void fail(UUID player, long now) {
        int count = fails.merge(player, 1, Integer::sum);
        if (count >= maxFails) {
            fails.remove(player);
            blockedUntil.put(player, now + penaltyMillis);
        }
    }

    void success(UUID player) {
        fails.remove(player);
    }
}
