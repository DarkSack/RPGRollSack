package com.sack.rpgroll.crafting.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * Identidad de ítem para RPGRoll-Crafting: cualquier resultado de
 * {@code CustomRecipe}/{@code CustomStation} que no sea un ítem vanilla puro
 * queda marcado con la clave compartida {@code rpgroll:item_id} (namespace
 * literal, no atado a este plugin) para que otros sistemas del ecosistema
 * puedan reconocerlo igual de simple que un ítem de RPGRoll-Items.
 *
 * Además sabe leer, sin depender en tiempo de compilación de RPGRoll-Items,
 * la clave PDC que ese addon ya usa ({@code rpgroll-items:item-id}) — así un
 * ítem personalizado de Items puede usarse como ingrediente o resultado de
 * una receta de Crafting sin que este módulo necesite la clase Java de Items.
 */
public final class ItemIdentity {

    private static final NamespacedKey CRAFTING_ID_KEY = new NamespacedKey("rpgroll", "item_id");
    private static final NamespacedKey ITEMS_ADDON_ID_KEY = new NamespacedKey("rpgroll-items", "item-id");
    private static final NamespacedKey QUALITY_KEY = new NamespacedKey("rpgroll", "crafting_quality");

    private ItemIdentity() {
    }

    public static void tag(ItemMeta meta, String id) {
        meta.getPersistentDataContainer().set(CRAFTING_ID_KEY, PersistentDataType.STRING, id);
    }

    public static void tagQuality(ItemMeta meta, String qualityId) {
        meta.getPersistentDataContainer().set(QUALITY_KEY, PersistentDataType.STRING, qualityId);
    }

    public static Optional<String> readOwnId(ItemStack item) {
        return read(item, CRAFTING_ID_KEY);
    }

    public static Optional<String> readItemsAddonId(ItemStack item) {
        return read(item, ITEMS_ADDON_ID_KEY);
    }

    public static Optional<String> readQuality(ItemStack item) {
        return read(item, QUALITY_KEY);
    }

    /** Id de identidad sea cual sea el origen: primero propio, si no hay, el de RPGRoll-Items. */
    public static Optional<String> readAnyId(ItemStack item) {
        return readOwnId(item).or(() -> readItemsAddonId(item));
    }

    public static boolean matchesId(ItemStack item, String id) {
        return readAnyId(item).map(id::equals).orElse(false);
    }

    private static Optional<String> read(ItemStack item, NamespacedKey key) {

        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return Optional.ofNullable(container.get(key, PersistentDataType.STRING));
    }

}
