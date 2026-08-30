package com.sack.rpgroll.items.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurationParserTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void parseMillisReturnsZeroForBlankInput(String raw) {
        assertEquals(0, DurationParser.parseMillis(raw));
    }

    @ParameterizedTest
    @CsvSource({
            "10s, 10000",
            "5m, 300000",
            "2h, 7200000",
            "1d, 86400000",
    })
    void parseMillisConvertsEachUnit(String raw, long expectedMillis) {
        assertEquals(expectedMillis, DurationParser.parseMillis(raw));
    }

    @Test
    void parseMillisDefaultsToSecondsWhenNoUnitGiven() {
        assertEquals(30_000L, DurationParser.parseMillis("30"));
    }

    @Test
    void parseMillisIsCaseInsensitiveAndTrimsWhitespace() {
        assertEquals(300_000L, DurationParser.parseMillis("  5M  "));
    }

    @Test
    void parseMillisReturnsZeroForUnparseableInput() {
        assertEquals(0, DurationParser.parseMillis("abc"));
        assertEquals(0, DurationParser.parseMillis("10x"));
        assertEquals(0, DurationParser.parseMillis("-5s"));
    }
}
