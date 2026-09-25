package com.sack.rpgroll.pass.season;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeasonTest {

    private static Season season() {
        TreeMap<Integer, SeasonLevel> levels = new TreeMap<>();
        for (int i = 1; i <= 30; i++) {
            levels.put(i, new SeasonLevel(i, List.of(), List.of()));
        }
        return new Season("t1", "T1", LocalDate.of(2026, 9, 25), LocalDate.of(2026, 11, 8), 1000, levels);
    }

    @Test
    void levelsFollowThePointsAndStopAtTheTop() {

        Season season = season();

        assertEquals(0, season.levelFor(999));
        assertEquals(1, season.levelFor(1000));
        assertEquals(30, season.levelFor(1_000_000));
    }

    @Test
    void isOpenIncludesBothEnds() {

        Season season = season();

        assertTrue(season.isOpen(LocalDate.of(2026, 9, 25)));
        assertTrue(season.isOpen(LocalDate.of(2026, 11, 8)));
        assertFalse(season.isOpen(LocalDate.of(2026, 11, 9)));
    }

}
