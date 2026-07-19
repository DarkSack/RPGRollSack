package com.sack.rpgroll.race;

import com.sack.rpgroll.content.RPGContent;
import com.sack.rpgroll.gameplay.stats.StatType;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Representa una raza jugable dentro del sistema RPG.
 * <p>
 * Es un objeto de datos inmutable: no contiene lógica de carga desde YAML
 * ni de persistencia en base de datos.
 *
 * @param icon código base64 de textura de cabeza de jugador
 *             (minecraft-heads.com
 *             u otra fuente). Puede estar vacío si la raza no define ícono —
 *             en ese caso se muestra una cabeza sin textura custom.
 */
public record Race(
        String id,
        String displayName,
        String description,
        Map<StatType, Integer> baseAttributes,
        List<String> passiveTraitIds,
        String icon,
        List<String> lore) implements RPGContent {

    public Race {
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