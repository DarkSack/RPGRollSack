package com.sack.rpgroll.emotes.gui;

import org.bukkit.event.inventory.ClickType;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Reparto de huecos del menú (54) y paginación. Lógica pura, sin Bukkit.
 * <pre>
 * fila 0: pestañas de categoría (0-8)
 * filas 1-4: contenido en columnas 1-7 (28 casillas); las columnas 0 y 8 llevan «anterior» y «siguiente»
 * fila 5: favoritos en 46-52, y «parar emote» en 53
 * </pre>
 */
final class MenuLayout {

    static final int SIZE = 54;
    static final int PER_PAGE = 28;

    /** Pestañas: «Todos» y hasta 8 categorías; las que sobren no se muestran. */
    static final List<Integer> CATEGORY_SLOTS = IntStream.rangeClosed(0, 8).boxed().toList();
    static final List<Integer> CONTENT_SLOTS = IntStream.rangeClosed(1, 4)
            .flatMap(row -> IntStream.rangeClosed(1, 7).map(col -> row * 9 + col)).boxed().toList();
    /** Siete huecos de favoritos; si {@code favorites.max} fuera mayor, los de más no se muestran. */
    static final List<Integer> FAVORITE_SLOTS = IntStream.rangeClosed(46, 52).boxed().toList();
    static final int PREV = 27;
    static final int NEXT = 35;
    static final int STOP = 53;

    /** Qué hace un clic sobre un emote. */
    enum ClickAction { LAUNCH, TOGGLE_FAVORITE, NONE }

    private MenuLayout() {
    }

    /** Izquierdo lanza, derecho alterna el favorito; teclas numéricas, soltar, central, etc. no hacen nada. */
    static ClickAction action(ClickType click) {
        return switch (click) {
            case LEFT, SHIFT_LEFT -> ClickAction.LAUNCH;
            case RIGHT, SHIFT_RIGHT -> ClickAction.TOGGLE_FAVORITE;
            default -> ClickAction.NONE;
        };
    }

    /** Páginas necesarias; mínimo 1. */
    static int pages(int size, int perPage) {
        return Math.max(1, (size + perPage - 1) / perPage);
    }

    /** La página {@code page} (desde 0); si se sale de rango se ajusta a la primera o a la última. */
    static <T> List<T> page(List<T> list, int page, int perPage) {
        int p = Math.max(0, Math.min(page, pages(list.size(), perPage) - 1));
        int from = Math.min(list.size(), p * perPage);
        return list.subList(from, Math.min(list.size(), from + perPage));
    }
}
