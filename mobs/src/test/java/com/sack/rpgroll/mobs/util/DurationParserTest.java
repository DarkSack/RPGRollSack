package com.sack.rpgroll.mobs.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurationParserTest {

    @Test
    void nullOrBlankReturnsZero() {
        assertEquals(0, DurationParser.parseMillis(null));
        assertEquals(0, DurationParser.parseMillis(""));
        assertEquals(0, DurationParser.parseMillis("   "));
    }

    @Test
    void unrecognizedFormatReturnsZero() {
        assertEquals(0, DurationParser.parseMillis("abc"));
        assertEquals(0, DurationParser.parseMillis("10x"));
        assertEquals(0, DurationParser.parseMillis("-5s"));
    }

    @Test
    void plainNumberDefaultsToSeconds() {
        assertEquals(10_000L, DurationParser.parseMillis("10"));
    }

    @ParameterizedTest
    @CsvSource({
            "10s, 10000",
            "5m, 300000",
            "2h, 7200000",
            "1d, 86400000",
            "0s, 0"
    })
    void parsesEachUnitToMillis(String raw, long expectedMillis) {
        assertEquals(expectedMillis, DurationParser.parseMillis(raw));
    }

    @Test
    void isCaseInsensitiveAndTrimsWhitespace() {
        assertEquals(60_000L, DurationParser.parseMillis(" 1M "));
        assertEquals(60_000L, DurationParser.parseMillis("1 m"));
    }
}
