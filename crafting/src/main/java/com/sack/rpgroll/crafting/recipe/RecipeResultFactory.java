package com.sack.rpgroll.crafting.recipe;

import com.sack.rpgroll.crafting.integration.ItemsBridge;
import com.sack.rpgroll.crafting.quality.CraftQuality;
import com.sack.rpgroll.crafting.quality.QualityApplier;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Optional;

/** Construye el {@link ItemStack} final de un {@link RecipeResult}, aplicando calidad si corresponde. */
public class RecipeResultFactory {

    private final QualityApplier qualityApplier = new QualityApplier();

    public Optional<ItemStack> build(RecipeResult result, CraftQuality quality) {

        Optional<ItemStack> built = switch (result.type()) {
            case MATERIAL -> buildMaterial(result);
            case ITEM_ID -> buildCustomItem(result);
        };

        built.ifPresent(stack -> {
            if (quality != null) {
                qualityApplier.apply(stack, quality);
            }
        });

        return built;
    }

    private Optional<ItemStack> buildMaterial(RecipeResult result) {

        try {
            Material material = Material.valueOf(result.value().trim().toUpperCase(Locale.ROOT));
            return Optional.of(new ItemStack(material, result.amount()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Optional<ItemStack> buildCustomItem(RecipeResult result) {

        return ItemsBridge.createItem(result.value()).map(stack -> {
            stack.setAmount(result.amount());
            return stack;
        });
    }

}
