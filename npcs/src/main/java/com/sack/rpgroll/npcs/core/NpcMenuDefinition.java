package com.sack.rpgroll.npcs.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;

public record NpcMenuDefinition(
        String id,
        String title,
        int rows,
        List<NpcMenuItem> items) implements RPGContent {

    public NpcMenuDefinition {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(title, "title no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        rows = Math.max(1, Math.min(6, rows));
        items = items == null ? List.of() : List.copyOf(items);
    }

}