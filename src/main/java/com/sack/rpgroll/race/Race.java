package com.sack.rpgroll.race;

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
 * <p>
 * Ejemplo de origen esperado (races/elfo.yml):
 * 
 * <pre>
 * id: elfo
 * display-name: "Elfo"
 * description: "Longevos y ágiles, con afinidad natural a la magia."
 * base-attributes:
 *   dexterity: 2
 *   intelligence: 1
 * passive-traits:
 *   - vision_nocturna
 *   - resistencia_encantamiento
 * icon: END_CRYSTAL
 * lore:
 *   - "Habitantes ancestrales del bosque"
 * </pre>
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
        List<String> lore) {

    /**
     * Constructor compacto: valida invariantes y garantiza inmutabilidad profunda
     * de las colecciones (evita que un caller externo mute el estado interno).
     */
    public Race {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(displayName, "displayName no puede ser null");
        Objects.requireNonNull(icon, "icon no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        description = description == null ? "" : description;
        baseAttributes = baseAttributes == null
                ? Map.of()
                : Map.copyOf(baseAttributes);
        passiveTraitIds = passiveTraitIds == null
                ? List.of()
                : List.copyOf(passiveTraitIds);
        lore = lore == null
                ? List.of()
                : List.copyOf(lore);
    }

    /**
     * Obtiene el bonificador de un atributo base específico otorgado por esta raza.
     *
     * @param stat         el StatType a consultar
     * @param defaultValue valor a devolver si la raza no define bonificador para
     *                     ese stat
     * @return el bonificador definido, o {@code defaultValue} si no existe
     */
    public int getBaseAttribute(StatType stat, int defaultValue) {
        return baseAttributes.getOrDefault(stat, defaultValue);
    }

    /**
     * @return {@code true} si esta raza otorga al menos un trait pasivo
     */
    public boolean hasPassiveTraits() {
        return !passiveTraitIds.isEmpty();
    }
}