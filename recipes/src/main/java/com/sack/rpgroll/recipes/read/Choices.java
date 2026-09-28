package com.sack.rpgroll.recipes.read;

import com.sack.rpgroll.common.recipe.RecipeSlot;

import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.RecipeChoice;

import java.util.ArrayList;
import java.util.List;

/** Pasa los {@link RecipeChoice} de Bukkit (material, tag, ítem exacto) a huecos del visor. */
final class Choices {

    private Choices() {
    }

    @SuppressWarnings({"deprecation", "UnstableApiUsage"})
    static RecipeSlot slot(RecipeChoice choice) {

        if (choice == null) {
            return RecipeSlot.EMPTY;
        }

        List<ItemStack> options = new ArrayList<>();

        if (choice instanceof RecipeChoice.ExactChoice exact) {
            options.addAll(exact.getChoices());
        } else if (choice instanceof RecipeChoice.MaterialChoice materials) {
            for (Material material : materials.getChoices()) {
                if (material.isItem() && !material.isAir()) {
                    options.add(ItemStack.of(material));
                }
            }
        } else if (choice instanceof RecipeChoice.ItemTypeChoice types) {
            for (ItemType type : types.itemTypes().resolve(Registry.ITEM)) {
                options.add(type.createItemStack());
            }
        } else {
            // Opciones sin lista (PredicateRecipeChoice de las mezclas de pociones): lo que
            // Bukkit dé como ejemplo, si da algo.
            try {
                ItemStack example = choice.getItemStack();
                if (example != null) {
                    options.add(example);
                }
            } catch (RuntimeException ignored) {
                // No hay ejemplo: el hueco queda vacío.
            }
        }

        return RecipeSlot.of(options);
    }
}
