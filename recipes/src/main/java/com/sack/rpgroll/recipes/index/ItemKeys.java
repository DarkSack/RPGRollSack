package com.sack.rpgroll.recipes.index;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Claves con las que se busca un ítem en el índice, de la más exacta a la más general.
 * <p>
 * Un ítem vanilla sin nada raro es solo su material. Un ítem con datos propios se busca
 * primero tal cual (huella de todo el ítem), luego por su id de RPGRoll, luego por su modelo
 * (así se reconocen los ítems de ItemsAdder, Oraxen, Nexo...) y, si no tiene identidad propia
 * (una espada gastada, una poción), por su material. Una espada personalizada NUNCA cae al
 * material: enseñaría la receta de la espada vanilla.
 */
public final class ItemKeys {

    private static final NamespacedKey ITEMS_ID = new NamespacedKey("rpgroll-items", "item-id");
    private static final NamespacedKey CRAFTING_ID = new NamespacedKey("rpgroll", "item_id");

    private ItemKeys() {
    }

    public static List<String> keysOf(ItemStack stack) {

        if (stack == null || stack.getType().isAir()) {
            return List.of();
        }

        ItemStack one = stack.asOne();
        Material type = one.getType();

        if (one.isSimilar(ItemStack.of(type))) {
            return List.of(material(type));
        }

        List<String> keys = new ArrayList<>(4);
        keys.add("x:" + fingerprint(one));

        ItemMeta meta = one.getItemMeta();
        if (meta == null) {
            keys.add(material(type));
            return keys;
        }

        String id = customId(meta);
        if (id != null) {
            keys.add("id:" + id);
        }

        if (meta.hasItemModel()) {
            keys.add("model:" + meta.getItemModel());
        }

        if (!hasIdentity(meta)) {
            keys.add(material(type));
        }

        return keys;
    }

    /** La clave más exacta (la que distingue un ítem de otro en el catálogo). */
    public static String primary(ItemStack stack) {
        List<String> keys = keysOf(stack);
        return keys.isEmpty() ? null : keys.getFirst();
    }

    public static String material(Material type) {
        return "m:" + type.getKey().asString();
    }

    public static boolean isPlain(ItemStack stack) {
        return stack != null && stack.asOne().isSimilar(ItemStack.of(stack.getType()));
    }

    /** Nombre, modelo o datos de plugin: es un ítem propio, no una variante del vanilla. */
    static boolean hasIdentity(ItemMeta meta) {
        return meta.hasDisplayName() || meta.hasItemName() || meta.hasItemModel() || meta.hasCustomModelDataComponent()
                || !meta.getPersistentDataContainer().isEmpty();
    }

    private static String customId(ItemMeta meta) {
        var pdc = meta.getPersistentDataContainer();
        String id = pdc.get(ITEMS_ID, PersistentDataType.STRING);
        if (id == null) {
            id = pdc.get(CRAFTING_ID, PersistentDataType.STRING);
        }
        return id == null || id.isBlank() ? null : id.toLowerCase(Locale.ROOT);
    }

    private static String fingerprint(ItemStack one) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(one.serializeAsBytes());
            return HexFormat.of().formatHex(hash, 0, 10);
        } catch (Exception | LinkageError e) {
            // Sin servidor (tests) o un ítem que no se deja serializar: basta con algo estable.
            byte[] text = one.toString().getBytes(StandardCharsets.UTF_8);
            return Integer.toHexString(java.util.Arrays.hashCode(text));
        }
    }
}
