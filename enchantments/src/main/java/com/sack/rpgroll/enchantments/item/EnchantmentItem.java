package com.sack.rpgroll.enchantments.item;

import com.sack.rpgroll.enchantments.core.CustomEnchantment;
import com.sack.rpgroll.enchantments.core.EnchantCategory;
import com.sack.rpgroll.enchantments.core.EnchantmentManager;
import com.sack.rpgroll.enchantments.util.RomanNumerals;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lee y escribe los encantamientos custom de un ItemStack. El dato real
 * vive en el PersistentDataContainer (a prueba de renombrar con yunque);
 * el lore es solo una representación visual que se reconstruye a partir
 * de ese dato — por eso también se guarda cuántas líneas de lore ocupa el
 * bloque actual, para poder reemplazarlo sin tocar el resto del lore del
 * ítem (ej. el de un sistema de crates u otro addon).
 */
public class EnchantmentItem {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final String ENTRY_SEPARATOR = ";";
    private static final String KEY_VALUE_SEPARATOR = ":";

    private final EnchantmentManager manager;
    private final NamespacedKey dataKey;
    private final NamespacedKey loreCountKey;

    public EnchantmentItem(Plugin plugin, EnchantmentManager manager) {
        this.manager = manager;
        this.dataKey = new NamespacedKey(plugin, "custom-enchantments");
        this.loreCountKey = new NamespacedKey(plugin, "custom-enchantments-lore-count");
    }

    public enum ApplyResult {
        OK,
        LEVEL_TOO_HIGH,
        CONFLICT,
        CATEGORY_NOT_ALLOWED
    }

    public ApplyResult apply(ItemStack item, CustomEnchantment enchantment, int level) {

        if (level < 1 || level > enchantment.maxLevel()) {
            return ApplyResult.LEVEL_TOO_HIGH;
        }

        if (!isCategoryAllowed(item, enchantment)) {
            return ApplyResult.CATEGORY_NOT_ALLOWED;
        }

        if (hasConflict(item, enchantment)) {
            return ApplyResult.CONFLICT;
        }

        Map<String, Integer> current = getAll(item);
        current.put(enchantment.id(), level);
        write(item, current);

        return ApplyResult.OK;
    }

    public boolean remove(ItemStack item, String enchantId) {

        Map<String, Integer> current = getAll(item);

        if (current.remove(enchantId) == null) {
            return false;
        }

        write(item, current);
        return true;
    }

    public int getLevel(ItemStack item, String enchantId) {
        return getAll(item).getOrDefault(enchantId, 0);
    }

    public Map<String, Integer> getAll(ItemStack item) {

        Map<String, Integer> result = new LinkedHashMap<>();

        if (item == null || !item.hasItemMeta()) {
            return result;
        }

        String raw = item.getItemMeta().getPersistentDataContainer().get(dataKey, PersistentDataType.STRING);

        if (raw == null || raw.isBlank()) {
            return result;
        }

        for (String entry : raw.split(ENTRY_SEPARATOR)) {

            String[] parts = entry.split(KEY_VALUE_SEPARATOR);
            if (parts.length != 2) {
                continue;
            }

            try {
                result.put(parts[0], Integer.parseInt(parts[1]));
            } catch (NumberFormatException ignored) {
            }
        }

        return result;
    }

    public boolean hasConflict(ItemStack item, CustomEnchantment enchantment) {

        for (String existingId : getAll(item).keySet()) {

            if (enchantment.conflicts().stream().anyMatch(c -> c.equalsIgnoreCase(existingId))) {
                return true;
            }

            Optional<CustomEnchantment> existing = manager.get(existingId);

            if (existing.isPresent()
                    && existing.get().conflicts().stream().anyMatch(c -> c.equalsIgnoreCase(enchantment.id()))) {
                return true;
            }
        }

        return false;
    }

    public boolean isCategoryAllowed(ItemStack item, CustomEnchantment enchantment) {

        if (!enchantment.allowedItems().isEmpty()) {
            return enchantment.allowedItems().contains(item.getType());
        }

        if (enchantment.categories().isEmpty()) {
            return true;
        }

        for (EnchantCategory category : enchantment.categories()) {
            if (category.matches(item.getType())) {
                return true;
            }
        }

        return false;
    }

    private void write(ItemStack item, Map<String, Integer> enchantments) {

        ItemMeta meta = item.getItemMeta();

        StringBuilder raw = new StringBuilder();
        for (var entry : enchantments.entrySet()) {
            if (!raw.isEmpty()) {
                raw.append(ENTRY_SEPARATOR);
            }
            raw.append(entry.getKey()).append(KEY_VALUE_SEPARATOR).append(entry.getValue());
        }

        meta.getPersistentDataContainer().set(dataKey, PersistentDataType.STRING, raw.toString());
        rebuildLore(meta, enchantments);

        item.setItemMeta(meta);
    }

    private void rebuildLore(ItemMeta meta, Map<String, Integer> enchantments) {

        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        Integer previousCount = meta.getPersistentDataContainer().get(loreCountKey, PersistentDataType.INTEGER);
        int toRemove = previousCount != null ? previousCount : 0;

        for (int i = 0; i < toRemove && !lore.isEmpty(); i++) {
            lore.remove(0);
        }

        List<Component> block = new ArrayList<>();

        for (var entry : enchantments.entrySet()) {

            String displayName = manager.get(entry.getKey())
                    .map(CustomEnchantment::displayName)
                    .orElse(entry.getKey());

            block.add(LEGACY.deserialize(displayName)
                    .append(Component.text(" " + RomanNumerals.of(entry.getValue()), NamedTextColor.GRAY)));
        }

        lore.addAll(0, block);

        meta.lore(lore);
        meta.getPersistentDataContainer().set(loreCountKey, PersistentDataType.INTEGER, block.size());
    }

}
