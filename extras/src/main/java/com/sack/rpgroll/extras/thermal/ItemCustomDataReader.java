package com.sack.rpgroll.extras.thermal;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lee el custom-data genérico de RPGRoll-Items directamente del PDC del
 * ítem — SIN depender de las clases de ese módulo (cero acoplamiento),
 * replicando exactamente el mismo namespace/clave/formato que
 * {@code ItemInstanceService#getCustomData} ya usa internamente. Si
 * RPGRoll-Items no está instalado, esto simplemente nunca encuentra el
 * PDC key y devuelve un mapa vacío — no falla.
 */
public final class ItemCustomDataReader {

    private static final NamespacedKey CUSTOM_DATA_KEY = new NamespacedKey("rpgroll-items", "item-custom-data");
    private static final String ENTRY_SEPARATOR = ";";
    private static final String KEY_VALUE_SEPARATOR = ":";

    private ItemCustomDataReader() {
    }

    public static Map<String, String> read(ItemStack item) {

        Map<String, String> result = new LinkedHashMap<>();

        if (item == null || !item.hasItemMeta()) {
            return result;
        }

        String raw = item.getItemMeta().getPersistentDataContainer().get(CUSTOM_DATA_KEY, PersistentDataType.STRING);

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

}
