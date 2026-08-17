package com.sack.rpgroll.crafting.station;

import com.sack.rpgroll.common.content.RPGContent;
import com.sack.rpgroll.crafting.station.structure.StructureRequirement;
import com.sack.rpgroll.crafting.station.tier.TierUpgrade;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Una estación de crafteo personalizada: un bloque del mundo que, al hacer
 * click derecho, abre un inventario propio con slots de ingrediente(s), un
 * slot de combustible opcional y un slot de resultado. El procesamiento en sí
 * (avance de progreso, consumo de combustible, entrega del resultado) lo
 * lleva {@code StationProcessingEngine}; esta clase es solo la definición.
 * <p>
 * {@code structureRequirements} y {@code tierUpgrades} son listas — igual que
 * {@code ingredientSlots}/{@code allowedRecipeIds}, se editan directo en el
 * YAML de la estación (el editor en el juego solo cubre los campos escalares).
 *
 * @param triggerBlockMaterial   material vanilla cuyo click derecho abre esta estación
 * @param inventorySize          tamaño del inventario propio (múltiplo de 9)
 * @param ingredientSlots        índices de slot usados como entrada de ingredientes
 * @param fuelSlot                índice del slot de combustible, o -1 si no requiere
 * @param outputSlot              índice del slot donde aparece el resultado
 * @param requiresFuel            si es true, no procesa sin combustible disponible
 * @param allowedRecipeIds        filtro opcional de recetas permitidas (vacío = todas las que apunten a este id)
 * @param structureRequirements   bloques exigidos alrededor del disparador (vacío = un único bloque, sin estructura)
 * @param maxTier                 nivel máximo alcanzable con {@code /crafting upgrade} (1 = sin mejoras)
 * @param tierUpgrades            costo de cada salto de nivel (entradas para tier 2..maxTier)
 * @param speedBonusPerTier       fracción de reducción de {@code processingTimeTicks} por cada nivel sobre el 1 (0-1)
 * @param failReductionPerTier    fracción de reducción de {@code failChance} por cada nivel sobre el 1 (0-1)
 * @param skillCategory           categoría de {@code CraftingProficiency} que progresa al craftear acá (vacío = usa {@code id})
 * @param allowExperimentation    si {@code /crafting experiment} puede usarse en esta estación
 */
public record CustomStation(
        String id,
        String displayName,
        String icon,
        String triggerBlockMaterial,
        int inventorySize,
        List<Integer> ingredientSlots,
        int fuelSlot,
        int outputSlot,
        boolean requiresFuel,
        String guiTitle,
        Set<String> allowedRecipeIds,
        List<StructureRequirement> structureRequirements,
        int maxTier,
        List<TierUpgrade> tierUpgrades,
        double speedBonusPerTier,
        double failReductionPerTier,
        String skillCategory,
        boolean allowExperimentation) implements RPGContent {

    public CustomStation {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "SMITHING_TABLE" : icon;
        triggerBlockMaterial = triggerBlockMaterial == null || triggerBlockMaterial.isBlank()
                ? "SMITHING_TABLE" : triggerBlockMaterial;
        inventorySize = normalizeSize(inventorySize);
        ingredientSlots = ingredientSlots == null ? List.of() : List.copyOf(ingredientSlots);
        guiTitle = guiTitle == null || guiTitle.isBlank() ? displayName : guiTitle;
        allowedRecipeIds = allowedRecipeIds == null ? Set.of() : Set.copyOf(allowedRecipeIds);
        structureRequirements = structureRequirements == null ? List.of() : List.copyOf(structureRequirements);
        maxTier = Math.max(1, maxTier);
        tierUpgrades = tierUpgrades == null ? List.of() : List.copyOf(tierUpgrades);
        speedBonusPerTier = Math.min(1, Math.max(0, speedBonusPerTier));
        failReductionPerTier = Math.min(1, Math.max(0, failReductionPerTier));
        skillCategory = skillCategory == null || skillCategory.isBlank() ? id : skillCategory;
    }

    private static int normalizeSize(int size) {
        int rounded = Math.max(9, (size / 9) * 9);
        return Math.min(54, rounded == 0 ? 27 : rounded);
    }

    /** Costo de subir al siguiente nivel, si no está ya en el máximo. */
    public java.util.Optional<TierUpgrade> nextTierUpgrade(int currentTier) {
        if (currentTier >= maxTier) {
            return java.util.Optional.empty();
        }
        return tierUpgrades.stream().filter(u -> u.tier() == currentTier + 1).findFirst();
    }

}
