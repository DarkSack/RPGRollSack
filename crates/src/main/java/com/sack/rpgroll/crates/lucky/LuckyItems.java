package com.sack.rpgroll.crates.lucky;

import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Optional;

/** El lucky block como ítem: lleva su id en el PDC y el modelo de su tipo. */
public class LuckyItems {

    private final NamespacedKey key;
    private final LuckyManager manager;

    public LuckyItems(Plugin plugin, LuckyManager manager) {
        this.key = new NamespacedKey(plugin, "lucky-block");
        this.manager = manager;
    }

    public ItemStack create(LuckyBlock block, int amount) {

        Material material = Material.matchMaterial(block.material());
        if (material == null || !material.isItem() || material.isAir()) {
            material = Material.PAPER;
        }

        ItemStack item = new ItemStack(material, Math.max(1, Math.min(amount, material.getMaxStackSize())));
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtils.parse(block.displayName()).decoration(TextDecoration.ITALIC, false));
        meta.lore(block.lore().stream()
                .map(line -> ComponentUtils.parse(line).decoration(TextDecoration.ITALIC, false))
                .toList());
        if (block.itemModel() != null) {
            NamespacedKey model = NamespacedKey.fromString(block.itemModel().toLowerCase(Locale.ROOT));
            if (model != null) {
                meta.setItemModel(model);
            }
        }
        if (block.glow()) {
            meta.setEnchantmentGlintOverride(true);
        }
        meta.setMaxStackSize(64);
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, block.id());
        item.setItemMeta(meta);
        return item;
    }

    public Optional<LuckyBlock> of(ItemStack item) {

        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }

        String id = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return id == null ? Optional.empty() : manager.get(id);
    }

    public boolean isLucky(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

}
