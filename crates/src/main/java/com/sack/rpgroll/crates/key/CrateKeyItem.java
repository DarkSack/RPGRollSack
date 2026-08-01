package com.sack.rpgroll.crates.key;

import com.sack.rpgroll.crates.core.Crate;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Crea llaves de crate y verifica si un ItemStack es la llave correcta
 * para un Crate específico. La verificación usa un tag en el
 * PersistentDataContainer del ítem — no el nombre ni el lore, que el
 * jugador podría falsificar con un yunque o un anvil renamer.
 */
public class CrateKeyItem {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final NamespacedKey crateIdTag;

    public CrateKeyItem(Plugin plugin) {
        this.crateIdTag = new NamespacedKey(plugin, "crate-key-id");
    }

    public ItemStack create(Crate crate, int amount) {

        ItemStack item = new ItemStack(crate.keyMaterial(), amount);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(LEGACY.deserialize(crate.keyDisplayName()).decorate(TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        for (String line : crate.keyLore()) {
            lore.add(LEGACY.deserialize(line));
        }
        lore.add(Component.text("Click derecho sobre el crate para abrir.", NamedTextColor.GRAY));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(crateIdTag, PersistentDataType.STRING, crate.id());

        item.setItemMeta(meta);

        return item;
    }

    public Optional<String> getCrateId(ItemStack item) {

        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }

        String value = item.getItemMeta().getPersistentDataContainer().get(crateIdTag, PersistentDataType.STRING);

        return Optional.ofNullable(value);
    }

    public boolean isKeyFor(ItemStack item, String crateId) {
        return getCrateId(item).map(id -> id.equals(crateId)).orElse(false);
    }

}
