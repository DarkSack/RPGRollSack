package com.sack.rpgroll.quests.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convierte duraciones en texto tipo "60s", "24h", "7d", "30m" a
 * milisegundos. Usado por el cooldown de quests repetibles y por el
 * objetivo WAIT.
 */
public final class DurationParser {

    private static final Pattern PATTERN = Pattern.compile("^(\\d+)\\s*(s|m|h|d)?$", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

    /** @return milisegundos, o 0 si el texto no es una duración válida. */
    public static long parseMillis(String raw) {

        if (raw == null || raw.isBlank()) {
            return 0;
        }

        Matcher matcher = PATTERN.matcher(raw.trim());

        if (!matcher.matches()) {
            return 0;
        }

        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2) == null ? "s" : matcher.group(2).toLowerCase();

        return switch (unit) {
            case "s" -> value * 1000L;
            case "m" -> value * 60_000L;
            case "h" -> value * 3_600_000L;
            case "d" -> value * 86_400_000L;
            default -> 0;
        };
    }

    /** @return segundos redondeados hacia abajo, útil para tasks en ticks (20 ticks = 1s). */
    public static long parseTicks(String raw) {
        return (parseMillis(raw) / 1000L) * 20L;
    }

}
