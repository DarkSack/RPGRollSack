package com.sack.rpgroll.crafting.station;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Una estación de crafteo personalizada: un bloque del mundo que, al hacer
 * click derecho, abre un inventario propio con slots de ingrediente(s), un
 * slot de combustible opcional y un slot de resultado. El procesamiento en sí
 * (avance de progreso, consumo de combustible, entrega del resultado) lo
 * lleva {@code StationProcessingEngine}; esta clase es solo la definición.
 *
 * @param triggerBlockMaterial material vanilla cuyo click derecho abre esta estación
 * @param inventorySize        tamaño del inventario propio (múltiplo de 9)
 * @param ingredientSlots      índices de slot usados como entrada de ingredientes
 * @param fuelSlot             índice del slot de combustible, o -1 si no requiere
 * @param outputSlot           índice del slot donde aparece el resultado
 * @param requiresFuel         si es true, no procesa sin combustible disponible
 * @param allowedRecipeIds     filtro opcional de recetas permitidas (vacío = todas las que apunten a este id)
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
        Set<String> allowedRecipeIds) implements RPGContent {

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
    }

    private static int normalizeSize(int size) {
        int rounded = Math.max(9, (size / 9) * 9);
        return Math.min(54, rounded == 0 ? 27 : rounded);
    }

}
