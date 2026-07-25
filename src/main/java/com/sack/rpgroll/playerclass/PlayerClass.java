package com.sack.rpgroll.playerclass;

import com.sack.rpgroll.content.RPGContent;
import com.sack.rpgroll.gameplay.stats.StatType;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Representa una clase jugable dentro del sistema RPG.
 * Objeto de datos inmutable, mismo patrón que Race — no contiene lógica
 * de carga desde YAML ni de persistencia en base de datos.
 *
 * @param baseAttributes bonificadores de atributos base otorgados por la
 *                        clase (StatType -> valor), mismo criterio que Race
 * @param icon            código base64 de textura de cabeza de jugador
 */
public record PlayerClass(
        String id,
        String displayName,
        String description,
        Map<StatType, Integer> baseAttributes,
        List<String> passiveTraitIds,
        String icon,
        List<String> lore
) implements RPGContent {

    public PlayerClass {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(displayName, "displayName no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        description = description == null ? "" : description;
        icon = icon == null ? "" : icon;
        baseAttributes = baseAttributes == null ? Map.of() : Map.copyOf(baseAttributes);
        passiveTraitIds = passiveTraitIds == null ? List.of() : List.copyOf(passiveTraitIds);
        lore = lore == null ? List.of() : List.copyOf(lore);
    }

    public int getBaseAttribute(StatType stat, int defaultValue) {
        return baseAttributes.getOrDefault(stat, defaultValue);
    }

    public boolean hasPassiveTraits() {
        return !passiveTraitIds.isEmpty();
    }

    public boolean hasCustomIcon() {
        return !icon.isBlank();
    }

}