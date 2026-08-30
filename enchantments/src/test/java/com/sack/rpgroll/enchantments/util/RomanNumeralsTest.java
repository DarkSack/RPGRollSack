package com.sack.rpgroll.enchantments.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RomanNumeralsTest {

    @ParameterizedTest
    @CsvSource({
            "1, I",
            "4, IV",
            "9, IX",
            "40, XL",
            "50, L",
            "90, XC",
            "400, CD",
            "900, CM",
            "1000, M",
            "1994, MCMXCIV",
            "3999, MMMCMXCIX",
    })
    void ofConvertsArabicToRoman(int number, String expected) {
        assertEquals(expected, RomanNumerals.of(number));
    }

    @Test
    void ofFallsBackToArabicBelowMinimum() {
        assertEquals("0", RomanNumerals.of(0));
        assertEquals("-5", RomanNumerals.of(-5));
    }

    @Test
    void ofFallsBackToArabicAboveMaximum() {
        assertEquals("4000", RomanNumerals.of(4000));
    }
}
