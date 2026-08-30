package com.sack.rpgroll.tab.format;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PingFormatterTest {

    @Test
    void usesDefaultTiersWhenNullOrEmptyProvided() {
        assertEquals("&a49ms", PingFormatter.format(49, null));
        assertEquals("&a49ms", PingFormatter.format(49, List.of()));
    }

    @Test
    void selectsFirstTierWhosePingIsBelowItsMaxMillis() {
        List<PingTier> tiers = List.of(
                new PingTier("GOOD:{ping}", 50),
                new PingTier("OK:{ping}", 150),
                new PingTier("BAD:{ping}", null));

        assertEquals("GOOD:0", PingFormatter.format(0, tiers));
        assertEquals("GOOD:49", PingFormatter.format(49, tiers));
        assertEquals("OK:50", PingFormatter.format(50, tiers));
        assertEquals("OK:149", PingFormatter.format(149, tiers));
        assertEquals("BAD:150", PingFormatter.format(150, tiers));
        assertEquals("BAD:9999", PingFormatter.format(9999, tiers));
    }

    @Test
    void nullCatchAllTierMatchesAnyRemainingPing() {
        List<PingTier> tiers = List.of(new PingTier("ANY:{ping}", null));

        assertEquals("ANY:0", PingFormatter.format(0, tiers));
        assertEquals("ANY:100000", PingFormatter.format(100000, tiers));
    }

    @Test
    void fallsBackToRawPingWhenNoTierMatches() {
        List<PingTier> tiers = List.of(new PingTier("LOW:{ping}", 10));

        assertEquals("50", PingFormatter.format(50, tiers));
    }

    @Test
    void replacesAllOccurrencesOfPingPlaceholder() {
        List<PingTier> tiers = List.of(new PingTier("{ping}-{ping}", null));

        assertEquals("42-42", PingFormatter.format(42, tiers));
    }
}
