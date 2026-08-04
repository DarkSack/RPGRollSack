package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;

/**
 * Un libro que enseña hechizos. Al usarse (click derecho sostenido, ver
 * {@code GrimoireListener}), se consume 1 del stack e intenta enseñar todos
 * sus {@code spellIds} al jugador — los que ya sabe, o para los que no
 * cumple {@code requiredLevel}, se saltean con un mensaje, sin cancelar el
 * resto.
 */
public record Grimoire(
        String id,
        String displayName,
        String icon,
        String description,
        String schoolId,
        int requiredLevel,
        List<String> spellIds) implements RPGContent {

    public Grimoire {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "WRITTEN_BOOK" : icon;
        description = description == null ? "" : description;
        schoolId = (schoolId == null || schoolId.isBlank()) ? null : schoolId;
        requiredLevel = Math.max(0, requiredLevel);
        spellIds = spellIds == null ? List.of() : List.copyOf(spellIds);
    }

}
