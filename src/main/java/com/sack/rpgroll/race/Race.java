package com.sack.rpgroll.race;

import com.sack.rpgroll.content.RPGContent;
import com.sack.rpgroll.gameplay.stats.StatType;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Representa una raza jugable dentro del sistema RPG.
 * <p>
 * Es un objeto de datos inmutable: no contiene lógica de carga desde YAML
 * ni de persistencia en base de datos. Esa responsabilidad corresponde
 * a un futuro {@code RaceLoader} / {@code RaceRegistry}.
 *
 * @param id              identificador único de la raza (coincide con el nombre
 *                        del archivo YAML)
 * @param displayName     nombre visible al jugador (soporta color codes de
 *                        Minecraft)
 * @param description     descripción corta usada en GUIs y comandos
 *                        informativos
 * @param baseAttributes  bonificadores de atributos base otorgados por la raza
 *                        (StatType -> valor)
 * @param passiveTraitIds identificadores de traits pasivos otorgados por la
 *                        raza (resueltos externamente por TraitRegistry)
 * @param icon            material usado para representar la raza en GUIs (ej.
 *                        RaceSelectionGUI)
 * @param lore            líneas de lore/flavor text adicionales, usadas como
 *                        lore del ItemStack en GUIs
 */
public record Race(
        String id,
        String displayName,
        String description,
        Map<StatType, Integer> baseAttributes,
        List<String> passiveTraitIds,
        Material icon,
        List<String> lore) implements RPGContent {

    public Race {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(displayName, "displayName no puede ser null");
        Objects.requireNonNull(icon, "icon no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        description = description == null ? "" : description;
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
}