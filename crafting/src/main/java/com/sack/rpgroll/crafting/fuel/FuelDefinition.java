package com.sack.rpgroll.crafting.fuel;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;

/**
 * Un ítem que puede usarse como combustible en una {@code CustomStation} con
 * {@code requiresFuel=true}. Mismo concepto que el combustible de horno
 * vanilla, pero medido en ticks de procesamiento de RPGRoll-Crafting, no en
 * la tabla interna de Bukkit.
 *
 * @param isCustomItem  si es true, {@code materialOrItemId} es un id de ítem (propio o de RPGRoll-Items);
 *                      si es false, es el nombre de un {@code Material} vanilla
 * @param burnTicks     ticks de procesamiento que aporta cada unidad consumida
 * @param consumeAmount cuántas unidades del ítem se consumen por cada {@code burnTicks} otorgados
 */
public record FuelDefinition(
        String id,
        String displayName,
        String icon,
        String materialOrItemId,
        boolean isCustomItem,
        int burnTicks,
        int consumeAmount) implements RPGContent {

    public FuelDefinition {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(materialOrItemId, "materialOrItemId no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "COAL" : icon;
        burnTicks = Math.max(1, burnTicks);
        consumeAmount = Math.max(1, consumeAmount);
    }

}
