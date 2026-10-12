package com.sack.rpgroll.traps.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El modelo (item_model) de cada torreta, munición y trampa: la clave {@code model:} de su YAML
 * ({@code model.item-model:} en las torretas, donde {@code model} ya es la sección del cuerpo), p. ej.
 * {@code model: rpgroll_traps:ammo/basic_bolt}. Lo rellenan los parsers y lo lee quien crea el ítem.
 * Sin modelo, se ve su material vanilla (o su {@code custom-model-data}, si lo tiene).
 * <p>
 * Va aparte de los records para no tocar a todos los que los construyen (editores, tests); los
 * DefinitionWriter conservan la clave al guardar desde el editor. Mismo patrón que Fishing y Ranching.
 */
public final class ItemModels {

    private static final Map<String, NamespacedKey> MODELS = new ConcurrentHashMap<>();

    private ItemModels() {
    }

    /**
     * @param kind turret, ammo o trap
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
