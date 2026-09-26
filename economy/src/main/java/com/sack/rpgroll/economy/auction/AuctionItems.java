package com.sack.rpgroll.economy.auction;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lo que la subasta necesita saber de un ItemStack: su nombre, si se puede vender y cómo buscarlo. */
public final class AuctionItems {

    private AuctionItems() {
    }

    public static String name(ItemStack item) {
        return PlainTextComponentSerializer.plainText().serialize(item.effectiveName());
    }

    /** null si se puede publicar; si no, la clave del mensaje de rechazo. */
    public static String blockReason(AuctionSettings settings, ItemStack item) {

        if (settings.blacklist().contains(item.getType())) {
            return "auction_house.blacklisted";
        }

        if (item.hasItemMeta()) {
            var container = item.getItemMeta().getPersistentDataContainer();
            for (String raw : settings.blockedDataKeys()) {
                NamespacedKey key = NamespacedKey.fromString(raw.toLowerCase(Locale.ROOT));
                if (key != null && container.has(key)) {
                    return "auction_house.blocked_item";
                }
            }
        }

        return null;
    }

    /** Sección y texto de búsqueda: nombre, material y encantamientos (también los guardados en libros). */
    public static AuctionManager.Index index(ItemStack item) {

        StringBuilder text = new StringBuilder(name(item)).append(' ')
                .append(item.getType().name().replace('_', ' '));

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            appendEnchantments(text, meta.getEnchants());
            if (meta instanceof EnchantmentStorageMeta book) {
                appendEnchantments(text, book.getStoredEnchants());
            }
        }

        return new AuctionManager.Index(AuctionCategory.of(item), text.toString().toLowerCase(Locale.ROOT));
    }

    private static void appendEnchantments(StringBuilder text, Map<Enchantment, Integer> enchantments) {
        enchantments.forEach((enchantment, level) -> text.append(' ')
                .append(enchantment.getKey().getKey().replace('_', ' ')).append(' ')
                .append(PlainTextComponentSerializer.plainText().serialize(enchantment.description())));
    }

    /** Una copia del ítem con líneas añadidas al final de su lore, para mostrarla en la GUI. */
    public static ItemStack display(ItemStack item, List<Component> extra) {

        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();

        if (meta == null) {
            return copy;
        }

        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.addAll(extra);
        meta.lore(lore);
        copy.setItemMeta(meta);
        return copy;
    }

    /** "2d 4h", "35m", "12s": el tiempo que falta, en las dos unidades más grandes. */
    public static String timeLeft(long millis) {

        long seconds = Math.max(0, millis / 1000);
        long days = seconds / 86_400;
        long hours = seconds % 86_400 / 3_600;
        long minutes = seconds % 3_600 / 60;

        if (days > 0) {
            return hours > 0 ? days + "d " + hours + "h" : days + "d";
        }
        if (hours > 0) {
            return minutes > 0 ? hours + "h " + minutes + "m" : hours + "h";
        }
        if (minutes > 0) {
            return minutes + "m";
        }
        return seconds + "s";
    }

}
