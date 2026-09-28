package com.sack.rpgroll.ranching.item;

import com.sack.rpgroll.ranching.core.production.ProductQuality;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El modelo (item_model) de cada ítem del plugin.
 * <p>
 * Piensos, medicinas y vacunas lo llevan en la clave {@code model:} de su YAML (lo rellenan los
 * parsers). Los productos (leche, lana, huevos...) no tienen YAML propio: salen de
 * {@code product-models:} del config.yml, con un modelo por tipo y, si se quiere, otro por calidad.
 * Sin modelo, el ítem se ve como su material vanilla.
 * <p>
 * Va aparte de los records para no tocar a todos los que los construyen (editores, tests); los
 * DefinitionWriter conservan la clave al guardar desde el editor.
 */
public final class ItemModels {

    private static final String PRODUCT = "product";
    private static final Map<String, NamespacedKey> MODELS = new ConcurrentHashMap<>();

    private ItemModels() {
    }

    /**
     * @param kind feed, medicine o vaccine
     * @param raw  el valor de {@code model:}; vacío o inválido quita el modelo
     */
    public static void register(String kind, String id, String raw) {

        String key = key(kind, id);
        NamespacedKey model = parse(raw);

        if (model == null) {
            MODELS.remove(key);
        } else {
            MODELS.put(key, model);
        }
    }

    /**
     * Lee {@code product-models:}. Cada tipo de producto es un modelo, o una sección con
     * {@code default} y una clave por calidad (COMMON, GOOD, PREMIUM, ORGANIC, LEGENDARY).
     */
    public static void loadProducts(ConfigurationSection section) {

        MODELS.keySet().removeIf(k -> k.startsWith(PRODUCT + ":"));

        if (section == null) {
            return;
        }

        for (String type : section.getKeys(false)) {

            if (!section.isConfigurationSection(type)) {
                register(PRODUCT, type, section.getString(type));
                continue;
            }

            ConfigurationSection byQuality = section.getConfigurationSection(type);
            for (String quality : byQuality.getKeys(false)) {
                String id = quality.equalsIgnoreCase("default") ? type : type + "/" + quality;
                register(PRODUCT, id, byQuality.getString(quality));
            }
        }
    }

    public static ItemStack apply(ItemStack item, String kind, String id) {
        return set(item, MODELS.get(key(kind, id)));
    }

    /** El modelo de esa calidad si lo hay; si no, el del tipo de producto. */
    public static ItemStack applyProduct(ItemStack item, String productType, ProductQuality quality) {

        NamespacedKey model = quality == null ? null : MODELS.get(key(PRODUCT, productType + "/" + quality.name()));

        return set(item, model != null ? model : MODELS.get(key(PRODUCT, productType)));
    }

    private static ItemStack set(ItemStack item, NamespacedKey model) {

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

    private static NamespacedKey parse(String raw) {
        return raw == null || raw.isBlank() ? null : NamespacedKey.fromString(raw.trim().toLowerCase(Locale.ROOT));
    }

    private static String key(String kind, String id) {
        return kind + ":" + (id == null ? "" : id.toLowerCase(Locale.ROOT));
    }
}
