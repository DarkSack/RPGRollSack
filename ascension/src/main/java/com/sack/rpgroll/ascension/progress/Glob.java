package com.sack.rpgroll.ascension.progress;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Comparación con comodín para {@code target} y {@code held}: {@code *}
 * vale por cualquier secuencia, sin distinguir mayúsculas. Vacío, {@code *}
 * y {@code ANY} valen por cualquier cosa. Así un criterio puede decir
 * {@code *_ORE} o {@code *_SWORD} sin enumerar materiales.
 */
public final class Glob {

    private Glob() {
    }

    public static boolean isWildcard(String pattern) {
        return pattern == null || pattern.isBlank() || pattern.equals("*") || pattern.equalsIgnoreCase("ANY");
    }

    public static boolean matches(String pattern, String value) {

        if (isWildcard(pattern)) {
            return true;
        }

        if (value == null) {
            return false;
        }

        String p = pattern.toLowerCase(Locale.ROOT);
        String v = value.toLowerCase(Locale.ROOT);

        if (!p.contains("*")) {
            return p.equals(v);
        }

        StringBuilder regex = new StringBuilder();
        for (String part : p.split("\\*", -1)) {
            if (!regex.isEmpty()) {
                regex.append(".*");
            }
            regex.append(Pattern.quote(part));
        }

        return v.matches(regex.toString());
    }

}
