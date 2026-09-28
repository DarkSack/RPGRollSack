package com.sack.rpgroll.fishing.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El modelo (item_model) de cada ítem del plugin: la clave {@code model:} de su YAML, p. ej.
 * {@code model: rpgroll_fishing:species/koi}. Lo rellenan los parsers al cargar cada fichero y lo
 * lee {@link FishingItemFactory} al crear el ítem. Sin modelo, el ítem se ve como su material
 * vanilla (o con su {@code custom-model-data}, si lo tiene).
 * <p>
 * Va aparte de los records para no tocar a todos los que los construyen (editores, tests); los
 * DefinitionWriter conservan la clave al guardar desde el editor.
 */
public final class ItemModels {

    private static final Map<String, NamespacedKey> MODELS = new ConcurrentHashMap<>();

    private ItemModels() {
    }

    /**
     * @param kind species, rod, bait o junk
     * @param raw  el valor de {@code model:}; vacío o inválido quita el modelo
     */
    public static void register(String kind, String id, String raw) {

        String key = key(kind, id);
        NamespacedKey model = raw == null || raw.isBlank() ? null
                : NamespacedKey.fromString(raw.trim().toLowerCase(Locale.ROOT));

        if (model == null) {
            MODELS.remove(key);
        } else {
            MODELS.put(key, model);
        }
    }

    public static NamespacedKey get(String kind, String id) {
        return MODELS.get(key(kind, id));
    }

    public static ItemStack apply(ItemStack item, String kind, String id) {

        NamespacedKey model = get(kind, id);

        if (model == null || item == null) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setItemModel(model);
            item.setItemMeta(meta);
        }

        return item;
    }

    private static String key(String kind, String id) {
        return kind + ":" + (id == null ? "" : id.toLowerCase(Locale.ROOT));
    }
}
