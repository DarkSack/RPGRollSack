package com.sack.rpgroll.licensing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LicenseCacheTest {

    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final long NOW = 1_800_000_000_000L;

    @Test
    void recentValidationIsInGrace() {
        assertTrue(LicenseCache.isWithinGracePeriod(new LicenseCache.CachedState(true, NOW - DAY), NOW));
    }

    @Test
    void validationOlderThanSevenDaysIsNot() {
        assertFalse(LicenseCache.isWithinGracePeriod(new LicenseCache.CachedState(true, NOW - 8 * DAY), NOW));
    }

    @Test
    void invalidStateIsNeverInGrace() {
        assertFalse(LicenseCache.isWithinGracePeriod(new LicenseCache.CachedState(false, NOW - DAY), NOW));
    }

    @Test
    void futureTimestampWrittenByHandIsRejected() {
        assertFalse(LicenseCache.isWithinGracePeriod(new LicenseCache.CachedState(true, 9_999_999_999_999L), NOW));
        assertFalse(LicenseCache.isWithinGracePeriod(new LicenseCache.CachedState(true, NOW + DAY), NOW));
    }

    @Test
    void smallClockSkewIsTolerated() {
        assertTrue(LicenseCache.isWithinGracePeriod(new LicenseCache.CachedState(true, NOW + 30_000L), NOW));
    }
}
