package com.sack.rpgroll.dungeons.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Convierte duraciones en texto tipo "10s", "5m" a milisegundos. */
public final class DurationParser {

    private static final Pattern PATTERN = Pattern.compile("^(\\d+)\\s*(s|m|h|d)?$", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

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

}
