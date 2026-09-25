package com.sack.rpgroll.pass.season;

import java.time.LocalDate;
import java.util.NavigableMap;
import java.util.Optional;

/**
 * Una temporada del pase: dura entre {@code start} y {@code end} (ambos
 * incluidos) y cada {@code xpPerLevel} puntos de pase sube un nivel.
 */
public record Season(String id, String displayName, LocalDate start, LocalDate end, int xpPerLevel,
        NavigableMap<Integer, SeasonLevel> levels) {

    public int maxLevel() {
        return levels.isEmpty() ? 0 : levels.lastKey();
    }

    public int levelFor(int xp) {
        return Math.min(maxLevel(), Math.max(0, xp) / xpPerLevel);
    }

    public boolean isOpen(LocalDate today) {
        return !today.isBefore(start) && !today.isAfter(end);
    }

    public Optional<SeasonLevel> level(int level) {
        return Optional.ofNullable(levels.get(level));
    }

}
