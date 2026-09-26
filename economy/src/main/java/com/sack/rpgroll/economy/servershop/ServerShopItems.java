package com.sack.rpgroll.economy.servershop;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.Locale;
import java.util.Optional;

/**
 * Construye una unidad de lo que vende una línea de la tienda. Vacío si no se
 * puede: el plugin que la define no está, o el id no existe.
 */
public final class ServerShopItems {

    private ServerShopItems() {
    }

    public static Optional<ItemStack> create(ServerShopEntry entry) {

        return switch (entry.kind()) {
            case MATERIAL -> Optional.of(Material.valueOf(entry.key())).filter(Material::isItem).map(ItemStack::new);
            case BOOK -> book(entry.key(), entry.level());
            case POTION -> potion(entry.key(), entry.form());
            case ITEM -> Bukkit.getPluginManager().isPluginEnabled("RPGRoll-Items")
                    ? ItemsBridge.create(entry.key()) : Optional.empty();
            case ENCHANT -> Bukkit.getPluginManager().isPluginEnabled("RPGRoll-Enchantments")
                    ? EnchantmentsBridge.book(entry.key(), entry.level()) : Optional.empty();
        };
    }

    /** Qué plugin falta para poder vender esta línea (null si no depende de ninguno). */
    public static String requiredPlugin(ServerShopEntry entry) {

        return switch (entry.kind()) {
            case ITEM -> "RPGRoll-Items";
            case ENCHANT -> "RPGRoll-Enchantments";
            default -> null;
        };
    }

    private static Optional<ItemStack> book(String key, int level) {

        Enchantment enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                .get(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));

        if (enchantment == null) {
            return Optional.empty();
        }

        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(enchantment, level, true);
        book.setItemMeta(meta);
        return Optional.of(book);
    }

    private static Optional<ItemStack> potion(String key, String form) {

        PotionType type = RegistryAccess.registryAccess().getRegistry(RegistryKey.POTION)
                .get(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));

        if (type == null) {
            return Optional.empty();
        }

        ItemStack potion = new ItemStack(Material.valueOf(form));
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(type);
        potion.setItemMeta(meta);
        return Optional.of(potion);
    }

}
