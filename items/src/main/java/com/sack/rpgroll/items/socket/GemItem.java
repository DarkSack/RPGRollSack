package com.sack.rpgroll.items.socket;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;

import net.kyori.adventure.text.format.TextDecoration;

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


    private final NamespacedKey gemIdKey;
    private final LangManager langManager;

    public GemItem(Plugin plugin, LangManager langManager) {
        this.gemIdKey = new NamespacedKey(plugin, "gem-id");
        this.langManager = langManager;
    }

    public ItemStack create(Gem gem) {

        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtils.parse(gem.displayName()).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                langManager.component("gem.lore_type", "type", gem.type()).decoration(TextDecoration.ITALIC, false),
                langManager.component("gem.lore_instructions").decoration(TextDecoration.ITALIC, false)));

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
