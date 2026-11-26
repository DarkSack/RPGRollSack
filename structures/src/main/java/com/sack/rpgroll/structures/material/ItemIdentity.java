package com.sack.rpgroll.structures.material;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;

/** La identidad real de un ítem de otro módulo: su id en el PDC o, si no, su modelo. */
public final class ItemIdentity {

    private static final List<NamespacedKey> KEYS = List.of(
            new NamespacedKey("rpgroll-items", "item-id"),
            new NamespacedKey("rpgroll", "item_id"),
            new NamespacedKey("rpgroll-furniture", "id"));

    private ItemIdentity() {
    }

    public static Optional<String> of(ItemStack stack) {
        if (stack == null) {
            return Optional.empty();
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (NamespacedKey key : KEYS) {
            String value;
            try {
                value = pdc.get(key, PersistentDataType.STRING);
            } catch (IllegalArgumentException e) {
                continue; // la clave existe con otro tipo: no es un id nuestro
            }
            if (value != null && !value.isBlank()) {
                return Optional.of(key.asString() + "=" + value);
            }
        }
        if (meta.hasItemModel() && meta.getItemModel() != null) {
            return Optional.of("model=" + meta.getItemModel().asString());
        }
        return Optional.empty();
    }
}
