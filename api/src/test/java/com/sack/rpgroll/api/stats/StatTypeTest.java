package com.sack.rpgroll.api.stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StatTypeTest {

    @Test
    void fromStringReturnsNullForNullInput() {
        assertNull(StatType.fromString(null));
    }

    @Test
    void fromStringReturnsNullForUnknownValue() {
        assertNull(StatType.fromString("nonexistent"));
    }

    @ParameterizedTest
    @CsvSource({
            "strength, STRENGTH",
            "STR, STRENGTH",
            "str, STRENGTH",
            "fuerza, STRENGTH",
            "STRENGTH, STRENGTH",
            "dexterity, DEXTERITY",
            "dex, DEXTERITY",
            "destreza, DEXTERITY",
            "constitution, CONSTITUTION",
            "con, CONSTITUTION",
            "constitucion, CONSTITUTION",
            "intelligence, INTELLIGENCE",
            "int, INTELLIGENCE",
            "inteligencia, INTELLIGENCE",
            "wisdom, WISDOM",
            "wis, WISDOM",
            "sabiduria, WISDOM",
            "charisma, CHARISMA",
            "cha, CHARISMA",
            "carisma, CHARISMA",
    })
    void fromStringResolvesEveryAliasCaseInsensitively(String alias, StatType expected) {
        assertEquals(expected, StatType.fromString(alias));
    }

    @Test
    void fromStringIsCaseInsensitiveForMixedCase() {
        assertEquals(StatType.STRENGTH, StatType.fromString("StR"));
    }
}
