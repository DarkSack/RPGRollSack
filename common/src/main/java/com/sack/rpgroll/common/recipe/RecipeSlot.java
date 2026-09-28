package com.sack.rpgroll.common.recipe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Un hueco de la receta: lo que puede ir ahí. Con varias opciones (cualquier tabla, cualquier
 * lana...) el visor las va rotando. La cantidad es la del propio {@link ItemStack}.
 */
public record RecipeSlot(List<ItemStack> options) {

    public static final RecipeSlot EMPTY = new RecipeSlot(List.of());

    public RecipeSlot {
        List<ItemStack> copy = new ArrayList<>();
        if (options != null) {
            for (ItemStack stack : options) {
                if (stack != null && !stack.getType().isAir()) {
                    copy.add(stack.clone());
                }
            }
        }
        options = List.copyOf(copy);
    }

    public static RecipeSlot of(ItemStack... options) {
        return new RecipeSlot(List.of(options));
    }

    public static RecipeSlot of(Collection<ItemStack> options) {
        return new RecipeSlot(new ArrayList<>(options));
    }

    public static RecipeSlot of(Material material, int amount) {
        return material == null || material.isAir() || !material.isItem()
                ? EMPTY : of(new ItemStack(material, Math.max(1, amount)));
    }

    public boolean isEmpty() {
        return options.isEmpty();
    }

    @Override
    public List<ItemStack> options() {
        return options.stream().map(ItemStack::clone).toList();
    }
}
