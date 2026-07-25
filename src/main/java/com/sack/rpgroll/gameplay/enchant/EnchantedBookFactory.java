package com.sack.rpgroll.gameplay.enchant;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Crea items "libro encantado" (ENCHANTED_BOOK) que cargan un encantamiento
 * custom, usando el mismo almacenamiento en PDC que cualquier otro item —
 * combinables en yunque, otorgables por drop o compra en tienda.
 */
public class EnchantedBookFactory {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final ItemEnchantmentStorage storage;

    public EnchantedBookFactory(ItemEnchantmentStorage storage) {
        this.storage = storage;
    }

    public ItemStack create(CustomEnchantment enchant, int level) {

        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();

        meta.displayName(Component.text("Libro: ", NamedTextColor.WHITE)
                .append(LEGACY.deserialize(enchant.displayName())));

        book.setItemMeta(meta);

        storage.addEnchantment(book, enchant.id(), level);
        return book;
    }

}