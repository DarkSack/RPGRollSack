package com.sack.rpgroll.crates.treasure;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChestClaimsTest {

    @Test
    void aChestIsLootedOnlyOnce() {

        ChestClaims claims = new ChestClaims();
        UUID chest = UUID.randomUUID();

        assertTrue(claims.claim(chest), "el primero lo abre");
        assertFalse(claims.claim(chest), "el segundo no saca otro botín");
        assertTrue(claims.claim(UUID.randomUUID()), "otro cofre no se ve afectado");
    }

}
