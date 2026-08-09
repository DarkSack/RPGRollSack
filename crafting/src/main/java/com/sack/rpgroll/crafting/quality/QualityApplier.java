package com.sack.rpgroll.crafting.quality;

import com.sack.rpgroll.crafting.item.ItemIdentity;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Marca un {@link ItemStack} ya construido con su {@link CraftQuality}:
 * etiqueta PDC (para que {@code IngredientMatcher} pueda exigir calidad
 * mínima en otras recetas) + una línea de lore visible para el jugador.
 */
public class QualityApplier {

    public void apply(ItemStack stack, CraftQuality quality) {

        if (stack == null || quality == null) {
            return;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        ItemIdentity.tagQuality(meta, quality.name());

        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("§8Calidad: " + quality.coloredLabel());
        meta.setLore(lore);

        stack.setItemMeta(meta);
    }

}
