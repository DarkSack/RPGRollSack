package com.sack.rpgroll.gameplay.enchant;

import com.sack.rpgroll.RPGRoll;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Guarda y lee encantamientos custom de un ItemStack vía
 * PersistentDataContainer
 * (una clave NamespacedKey por encantamiento), y mantiene sincronizada una
 * sección de lore visual con los encantamientos activos del item.
 */
public class ItemEnchantmentStorage {

    private static final String LORE_MARKER_TEXT = "⚡ Encantamientos ⚡";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final String[] ROMAN = { "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" };

    private final RPGRoll plugin;
    private final EnchantManager enchantManager;
    private final NamespacedKey glowDummyKey;

    public ItemEnchantmentStorage(RPGRoll plugin, EnchantManager enchantManager) {
        this.plugin = plugin;
        this.enchantManager = enchantManager;
        this.glowDummyKey = new NamespacedKey(plugin, "glow_dummy_active");
    }

    private NamespacedKey keyFor(String enchantId) {
        return new NamespacedKey(plugin, "ench_" + enchantId);
    }

    public ItemStack addEnchantment(ItemStack item, String enchantId, int level) {

        if (item == null || item.getType().isAir()) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.getPersistentDataContainer().set(keyFor(enchantId), PersistentDataType.INTEGER, level);
        item.setItemMeta(meta);

        refreshLore(item);
        refreshGlow(item);
        return item;
    }

    public ItemStack removeEnchantment(ItemStack item, String enchantId) {

        if (item == null || item.getType().isAir()) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.getPersistentDataContainer().remove(keyFor(enchantId));
        item.setItemMeta(meta);

        refreshLore(item);
        refreshGlow(item);
        return item;
    }

    public int getLevel(ItemStack item, String enchantId) {

        if (item == null || item.getType().isAir()) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        Integer level = meta.getPersistentDataContainer().get(keyFor(enchantId), PersistentDataType.INTEGER);
        return level == null ? 0 : level;
    }

    public Map<String, Integer> getEnchantments(ItemStack item) {

        Map<String, Integer> result = new LinkedHashMap<>();

        if (item == null || item.getType().isAir()) {
            return result;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return result;
        }

        for (CustomEnchantment enchant : enchantManager.getAll()) {
            Integer level = meta.getPersistentDataContainer().get(keyFor(enchant.id()), PersistentDataType.INTEGER);
            if (level != null && level > 0) {
                result.put(enchant.id(), level);
            }
        }

        return result;
    }

    /**
     * Sincroniza el brillo visual del item con si tiene encantamientos custom,
     * usando un encantamiento vanilla "dummy" (LUCK) solo para activar el
     * glow. Rastrea si el dummy está activo mediante un flag propio en el PDC
     * (glowDummyKey) — nunca infiere esto a partir del estado de LUCK en sí,
     * así que no hay riesgo de confundirlo con un LUCK real aplicado por el
     * jugador vía yunque/mesa de encantar vanilla.
     */
    private void refreshGlow(ItemStack item) {

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        boolean hasCustomEnchants = !getEnchantments(item).isEmpty();
        boolean dummyCurrentlyActive = meta.getPersistentDataContainer()
                .getOrDefault(glowDummyKey, PersistentDataType.BYTE, (byte) 0) == 1;

        if (hasCustomEnchants && !dummyCurrentlyActive) {

            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.getPersistentDataContainer().set(glowDummyKey, PersistentDataType.BYTE, (byte) 1);

        } else if (!hasCustomEnchants && dummyCurrentlyActive) {

            meta.removeEnchant(Enchantment.LUCK_OF_THE_SEA);
            meta.getPersistentDataContainer().remove(glowDummyKey);
        }

        // HIDE_ENCHANTS se mantiene si hay CUALQUIER encantamiento vanilla real
        // (incluido el dummy) — evita que se vea "Suerte del Mar" en el lore.
        // Los encantamientos vanilla reales del jugador siguen aplicados en
        // el item (no se tocan), solo se ocultan visualmente del lore, tal
        // como ya hace el juego cuando el propio jugador aplica ese flag.
        if (!meta.getEnchants().isEmpty()) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
    }

    private void refreshLore(ItemStack item) {

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        List<Component> currentLore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        int markerIndex = -1;
        for (int i = 0; i < currentLore.size(); i++) {
            if (PLAIN.serialize(currentLore.get(i)).equals(LORE_MARKER_TEXT)) {
                markerIndex = i;
                break;
            }
        }

        List<Component> baseLore = markerIndex >= 0
                ? new ArrayList<>(currentLore.subList(0, markerIndex))
                : currentLore;

        Map<String, Integer> enchants = getEnchantments(item);

        if (!enchants.isEmpty()) {

            baseLore.add(Component.text(LORE_MARKER_TEXT, NamedTextColor.DARK_GRAY));

            for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
                enchantManager.get(entry.getKey()).ifPresent(enchant -> {
                    String line = enchant.displayName() + " " + toRoman(entry.getValue());
                    baseLore.add(LEGACY.deserialize(line));
                });
            }
        }

        meta.lore(baseLore);
        item.setItemMeta(meta);
    }

    private String toRoman(int level) {
        if (level >= 1 && level <= ROMAN.length) {
            return ROMAN[level - 1];
        }
        return String.valueOf(level);
    }

}