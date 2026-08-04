package com.sack.rpgroll.items.socket;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;

/** Crea el ItemStack físico de una gema y lee su id — mismo patrón que CrateKeyItem. */
public class GemItem {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final NamespacedKey gemIdKey;

    public GemItem(Plugin plugin) {
        this.gemIdKey = new NamespacedKey(plugin, "gem-id");
    }

    public ItemStack create(Gem gem) {

        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(LEGACY.deserialize(gem.displayName()).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Gema: " + gem.type(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Click derecho en un socket para insertar.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)));

        meta.getPersistentDataContainer().set(gemIdKey, PersistentDataType.STRING, gem.id());

        item.setItemMeta(meta);
        return item;
    }

    public Optional<String> getGemId(ItemStack item) {

        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }

        return Optional.ofNullable(item.getItemMeta().getPersistentDataContainer()
                .get(gemIdKey, PersistentDataType.STRING));
    }

}
