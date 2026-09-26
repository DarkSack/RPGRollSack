package com.sack.rpgroll.common.menu;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;

/**
 * Un menú en YAML: título, filas y los ítems que lo componen. Con
 * {@code filler}, los huecos vacíos se rellenan con ese material.
 */
public record MenuDefinition(
        String id,
        String title,
        int rows,
        List<MenuItem> items,
        String filler) implements RPGContent {

    public MenuDefinition {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(title, "title no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        rows = Math.max(1, Math.min(6, rows));
        items = items == null ? List.of() : List.copyOf(items);
        filler = filler == null || filler.isBlank() ? null : filler.trim();
    }

    public MenuDefinition(String id, String title, int rows, List<MenuItem> items) {
        this(id, title, rows, items, null);
    }

}
