package com.sack.rpgroll.tab.sorting;

import java.util.List;

/**
 * Una regla de orden. Dos formas:
 * <ul>
 * <li>Nativa: {@code field} es uno de {@code name}/{@code ping}/{@code world}/
 * {@code online-time}/{@code permission} (para "permission", {@code values}
 * es una lista de permisos ordenada de mayor a menor prioridad — el jugador
 * se ordena según el índice del primero que tenga).</li>
 * <li>Por placeholder: {@code placeholder} es cualquier {@code {clave}} o
 * {@code %clave%} ya resuelto por el Placeholder Engine — así "nivel",
 * "clase", "guild", etc. se ordenan sin que TAB sepa qué son.</li>
 * </ul>
 */
public record SortRule(String field, List<String> values, String placeholder, SortOrder order, boolean numeric) {

    public static SortRule ofNative(String field, SortOrder order) {
        return new SortRule(field, List.of(), null, order, false);
    }

    public static SortRule ofNativePermission(List<String> permissions, SortOrder order) {
        return new SortRule("permission", permissions, null, order, false);
    }

    public static SortRule ofPlaceholder(String placeholder, SortOrder order, boolean numeric) {
        return new SortRule(null, List.of(), placeholder, order, numeric);
    }

    public boolean isPlaceholder() {
        return placeholder != null;
    }

}
