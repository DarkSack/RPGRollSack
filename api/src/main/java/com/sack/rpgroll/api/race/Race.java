package com.sack.rpgroll.api.race;

import com.sack.rpgroll.api.stats.StatType;
import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Representa una raza del sistema RPG (Elfo, Enano, Draconido, etc.).
 * Objeto de datos inmutable, cargado desde races/*.yml por RaceParser.
 *
 * @param baseAttributes    bonos de atributo aplicados a PlayerStats al crear personaje
 * @param passiveTraits     ids de traits otorgados automáticamente por esta raza
 * @param icon              textura base64 usada para la cabeza en las GUIs
 * @param physicalModifiers modificadores físicos aplicados sobre el Player real
 */
public record Race(
        String id,
        String displayName,
        String description,
        Map<StatType, Integer> baseAttributes,
        List<String> passiveTraits,
        String icon,
        List<String> lore,
        RacePhysicalModifiers physicalModifiers) implements RPGContent {

    public Race {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(displayName, "displayName no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        description = description == null ? "" : description;
        icon = icon == null ? "" : icon;
        baseAttributes = baseAttributes == null ? Map.of() : Map.copyOf(baseAttributes);
        passiveTraits = passiveTraits == null ? List.of() : List.copyOf(passiveTraits);
        lore = lore == null ? List.of() : List.copyOf(lore);
        physicalModifiers = physicalModifiers == null ? RacePhysicalModifiers.none() : physicalModifiers;
    }

}
