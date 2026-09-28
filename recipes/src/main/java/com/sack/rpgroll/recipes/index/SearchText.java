package com.sack.rpgroll.recipes.index;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Búsqueda del catálogo: sin mayúsculas ni tildes, todas las palabras tienen que aparecer, y
 * una palabra que empieza por {@code @} busca en el origen ({@code @crafting} = recetas de
 * RPGRoll-Crafting), como en JEI.
 */
public final class SearchText {

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern FORMAT = Pattern.compile("(?i)&[0-9a-fk-orx#]|<[^>]*>");

    private SearchText() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String clean = FORMAT.matcher(text).replaceAll("");
        return MARKS.matcher(Normalizer.normalize(clean, Normalizer.Form.NFD)).replaceAll("")
                .replace('_', ' ')
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    public static List<String> terms(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(normalized.split("\\s+")).filter(term -> !term.isEmpty() && !term.equals("@")).toList();
    }

    /**
     * @param haystack texto ya normalizado del ítem (nombre, material, estaciones...)
     * @param sources  orígenes ya normalizados de las recetas del ítem
     */
    public static boolean matches(List<String> terms, String haystack, Collection<String> sources) {
        for (String term : terms) {
            if (term.startsWith("@")) {
                String wanted = term.substring(1);
                if (sources.stream().noneMatch(source -> source.contains(wanted))) {
                    return false;
                }
            } else if (!haystack.contains(term)) {
                return false;
            }
        }
        return true;
    }
}
