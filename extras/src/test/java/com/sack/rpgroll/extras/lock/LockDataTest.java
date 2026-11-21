package com.sack.rpgroll.extras.lock;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LockDataTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void onlyFourDigitsAreACode() {
        assertTrue(LockData.validCode("0420"));
        assertFalse(LockData.validCode("420"));
        assertFalse(LockData.validCode("12345"));
        assertFalse(LockData.validCode("12a4"));
        assertFalse(LockData.validCode(null));
    }

    @Test
    void ownerAndCodeSurviveTheRoundTrip() {
        LockData lock = new LockData(OWNER, "0420");
        assertEquals(lock, LockData.decode(lock.encode()));
    }

    @Test
    void brokenValuesReadAsNoLock() {
        assertNull(LockData.decode(null));
        assertNull(LockData.decode("no-es-uuid;1234"));
        assertNull(LockData.decode(OWNER + ";12"));
        assertNull(LockData.decode(OWNER.toString()));
    }

    @Test
    void threeFailuresBlockTypingForAMinute() {
        Attempts attempts = new Attempts(3, 60_000);
        UUID player = UUID.randomUUID();

        assertEquals(0, attempts.blockedFor(player, 0));
        attempts.fail(player, 0);
        attempts.fail(player, 1_000);
        assertEquals(0, attempts.blockedFor(player, 1_500));
        attempts.fail(player, 2_000);
        assertEquals(60_000, attempts.blockedFor(player, 2_000));
        assertEquals(1_000, attempts.blockedFor(player, 61_000));
        assertEquals(0, attempts.blockedFor(player, 62_000));

        // Cumplido el castigo vuelve a tener tres intentos.
        attempts.fail(player, 62_000);
        assertEquals(0, attempts.blockedFor(player, 62_000));
    }

    @Test
    void aGoodCodeForgetsTheFailures() {
        Attempts attempts = new Attempts(3, 60_000);
        UUID player = UUID.randomUUID();
        attempts.fail(player, 0);
        attempts.fail(player, 0);
        attempts.success(player);
        attempts.fail(player, 0);
        assertEquals(0, attempts.blockedFor(player, 0));
    }
}
