package com.sack.rpgroll.furniture.core;

import org.bukkit.DyeColor;

/**
 * Una versión del mismo mueble con otro modelo: otra madera, otro color.
 *
 * @param id        como se nombra en comandos y recetas (oak, red...)
 * @param name      nombre del ítem (reemplaza al del mueble), o null
 * @param itemModel modelo de esta versión
 * @param dye       tinte que la aplica con clic derecho sobre el mueble colocado, o null
 * @param recipe    receta propia, o null para usar la del mueble
 */
public record FurnitureVariant(String id, String name, String itemModel, DyeColor dye, CarpenterRecipe recipe) {
}
