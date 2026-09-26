package com.sack.rpgroll.extras.backpack;

import java.util.List;

/**
 * Un nivel de mochila. {@code index} es su posición en backpacks.yml: la
 * receta de un nivel con {@code backpack} pide la mochila del índice anterior.
 *
 * @param recipe null si el nivel no se fabrica (solo por comando o tienda)
 */
public record BackpackTier(String id, int index, String name, List<String> lore, String texture, int slots,
        BackpackRecipe recipe) {
}
