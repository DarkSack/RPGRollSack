package com.sack.rpgroll.ascension.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrestigeLevelTest {

    @ParameterizedTest
    @CsvSource({"1,1", "2,2", "10,10", "999,999"})
    void numberParsesNumericId(String id, int expected) {
        assertEquals(expected, new PrestigeLevel(id, 50, 5.0, List.of()).number());
    }

    @ParameterizedTest
    @ValueSource(strings = {"one", "", "1.5", "I", "prestige-1"})
    void numberFallsBackToZeroForNonNumericId(String id) {
        assertEquals(0, new PrestigeLevel(id, 50, 5.0, List.of()).number());
    }

    @Test
    void nullIdIsRejected() {
        assertThrows(NullPointerException.class, () -> new PrestigeLevel(null, 50, 5.0, List.of()));
    }

    @Test
    void nullGrantedSkillsIsNormalizedToEmpty() {
        assertTrue(new PrestigeLevel("1", 50, 5.0, null).grantedSkills().isEmpty());
    }

    @Test
    void grantedSkillsAreDefensivelyCopied() {
        List<String> skills = new ArrayList<>(List.of("berserk"));
        PrestigeLevel level = new PrestigeLevel("1", 50, 5.0, skills);

        skills.add("mutated");

        assertEquals(List.of("berserk"), level.grantedSkills());
    }

    @Test
    void grantedSkillsListIsImmutable() {
        PrestigeLevel level = new PrestigeLevel("1", 50, 5.0, List.of("berserk"));

        assertThrows(UnsupportedOperationException.class, () -> level.grantedSkills().add("nope"));
    }
}
