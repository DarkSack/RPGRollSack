package com.sack.rpgroll.items.instance;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lee y escribe el estado propio de un ItemStack concreto (a prueba de
 * renombrar con yunque, vive en su PersistentDataContainer): qué
 * definición es, su durabilidad actual, nivel de mejora, skin activa y
 * gemas socketeadas. La creación del ItemStack en sí (material, lore,
 * encantamientos) es responsabilidad de {@link com.sack.rpgroll.items.core.ItemFactory}
 * — esta clase solo lee/escribe los datos, no construye apariencia.
 */
public class ItemInstanceService {

    private static final String ENTRY_SEPARATOR = ";";
    private static final String KEY_VALUE_SEPARATOR = ":";

    private final NamespacedKey idKey;
    private final NamespacedKey durabilityKey;
    private final NamespacedKey upgradeLevelKey;
    private final NamespacedKey skinIndexKey;
    private final NamespacedKey socketsKey;
    private final NamespacedKey customDataKey;

    public ItemInstanceService(Plugin plugin) {
        this.idKey = new NamespacedKey(plugin, "item-id");
        this.durabilityKey = new NamespacedKey(plugin, "item-durability");
        this.upgradeLevelKey = new NamespacedKey(plugin, "item-upgrade-level");
        this.skinIndexKey = new NamespacedKey(plugin, "item-skin-index");
        this.socketsKey = new NamespacedKey(plugin, "item-sockets");
        this.customDataKey = new NamespacedKey(plugin, "item-custom-data");
    }

    public Optional<String> getId(ItemStack item) {

        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }

        return Optional.ofNullable(item.getItemMeta().getPersistentDataContainer()
                .get(idKey, PersistentDataType.STRING));
    }

    public void setId(ItemMeta meta, String id) {
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
    }

    public int getDurability(ItemStack item, int fallback) {
        return getInt(item, durabilityKey, fallback);
    }

    public void setDurability(ItemMeta meta, int value) {
        meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER, value);
    }

    public int getUpgradeLevel(ItemStack item) {
        return getInt(item, upgradeLevelKey, 0);
    }

    public void setUpgradeLevel(ItemMeta meta, int level) {
        meta.getPersistentDataContainer().set(upgradeLevelKey, PersistentDataType.INTEGER, level);
    }

    public int getSkinIndex(ItemStack item) {
        return getInt(item, skinIndexKey, 0);
    }

    public void setSkinIndex(ItemMeta meta, int index) {
        meta.getPersistentDataContainer().set(skinIndexKey, PersistentDataType.INTEGER, index);
    }

    public Map<String, String> getSockets(ItemStack item) {

        Map<String, String> result = new LinkedHashMap<>();

        if (item == null || !item.hasItemMeta()) {
            return result;
        }

        String raw = item.getItemMeta().getPersistentDataContainer().get(socketsKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return result;
        }

        for (String entry : raw.split(ENTRY_SEPARATOR)) {

            String[] parts = entry.split(KEY_VALUE_SEPARATOR, 2);
            if (parts.length == 2) {
                result.put(parts[0], parts[1]);
            } else if (parts.length == 1 && !parts[0].isBlank()) {
                result.put(parts[0], "");
            }
        }

        return result;
    }

    public void setSockets(ItemMeta meta, Map<String, String> sockets) {

        StringBuilder raw = new StringBuilder();

        for (var entry : sockets.entrySet()) {
            if (!raw.isEmpty()) {
                raw.append(ENTRY_SEPARATOR);
            }
            raw.append(entry.getKey()).append(KEY_VALUE_SEPARATOR).append(entry.getValue());
        }

        meta.getPersistentDataContainer().set(socketsKey, PersistentDataType.STRING, raw.toString());
    }

    public Map<String, String> getCustomData(ItemStack item) {

        Map<String, String> result = new LinkedHashMap<>();

        if (item == null || !item.hasItemMeta()) {
            return result;
        }

        String raw = item.getItemMeta().getPersistentDataContainer().get(customDataKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return result;
        }

        for (String entry : raw.split(ENTRY_SEPARATOR)) {
            String[] parts = entry.split(KEY_VALUE_SEPARATOR, 2);
            if (parts.length == 2) {
                result.put(parts[0], parts[1]);
            }
        }

        return result;
    }

    public void setCustomData(ItemMeta meta, Map<String, String> data) {

        StringBuilder raw = new StringBuilder();

        for (var entry : data.entrySet()) {
            if (!raw.isEmpty()) {
                raw.append(ENTRY_SEPARATOR);
            }
            raw.append(entry.getKey()).append(KEY_VALUE_SEPARATOR).append(entry.getValue());
        }

        meta.getPersistentDataContainer().set(customDataKey, PersistentDataType.STRING, raw.toString());
    }

    private int getInt(ItemStack item, NamespacedKey key, int fallback) {

        if (item == null || !item.hasItemMeta()) {
            return fallback;
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        Integer value = container.get(key, PersistentDataType.INTEGER);

        return value != null ? value : fallback;
    }

}
