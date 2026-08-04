package com.sack.rpgroll.items.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Qué campos "de material" (color, textura de cabeza, trim) tiene sentido
 * mostrar en el editor para un {@link Material} dado. Usa exactamente el
 * mismo chequeo {@code instanceof} que {@link com.sack.rpgroll.items.core.ItemFactory}
 * aplica al construir el ítem real — así nunca queda un campo visible en la
 * GUI que después no tenga ningún efecto (ej. textura de cabeza en una
 * espada).
 */
public final class ItemMaterialTraits {

    private ItemMaterialTraits() {
    }

    public static boolean supportsDyeColor(Material material) {
        ItemMeta meta = metaOf(material);
        return meta instanceof LeatherArmorMeta || meta instanceof PotionMeta;
    }

    public static boolean supportsSkullTexture(Material material) {
        return metaOf(material) instanceof SkullMeta;
    }

    public static boolean supportsArmorTrim(Material material) {
        return metaOf(material) instanceof ArmorMeta;
    }

    public static boolean hasAnyMaterialExtras(Material material) {
        return supportsDyeColor(material) || supportsSkullTexture(material) || supportsArmorTrim(material);
    }

    private static ItemMeta metaOf(Material material) {
        return new ItemStack(material).getItemMeta();
    }

}
