package com.sack.rpgroll.quests.util;

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
            "60s, 60000",
            "30m, 1800000",
            "24h, 86400000",
            "7d, 604800000",
    })
    void parseMillisConvertsEachUnit(String raw, long expectedMillis) {
        assertEquals(expectedMillis, DurationParser.parseMillis(raw));
    }

    @Test
    void parseMillisDefaultsToSecondsWhenNoUnitGiven() {
        assertEquals(10_000L, DurationParser.parseMillis("10"));
    }

    @Test
    void parseMillisReturnsZeroForUnparseableInput() {
        assertEquals(0, DurationParser.parseMillis("not-a-duration"));
        assertEquals(0, DurationParser.parseMillis("-5s"));
    }

    @Test
    void parseTicksConvertsSecondsToTwentyTicksPerSecond() {
        assertEquals(200, DurationParser.parseTicks("10s"));
    }

    @Test
    void parseTicksConvertsMinutesToo() {
        assertEquals(20 * 60, DurationParser.parseTicks("1m"));
    }

    @Test
    void parseTicksIsZeroForBlankOrInvalidInput() {
        assertEquals(0, DurationParser.parseTicks(null));
        assertEquals(0, DurationParser.parseTicks("garbage"));
    }
}
