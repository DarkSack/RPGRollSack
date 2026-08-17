package com.sack.rpgroll.fishing.item;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.fishing.core.Bait;
import com.sack.rpgroll.fishing.core.FishingRod;
import com.sack.rpgroll.fishing.core.Junk;
import com.sack.rpgroll.fishing.core.Treasure;
import com.sack.rpgroll.fishing.engine.CatchResult;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Construye los ItemStacks de cañas y carnadas, etiquetados vía
 * PersistentDataContainer — mismo enfoque que MagicItemFactory. Una caña
 * de pescar vanilla sin etiquetar sigue funcionando (ver {@code
 * FishingRod#defaultRod()}), solo que sin ningún bono.
 */
public final class FishingItemFactory {


    private FishingItemFactory() {
    }

    public static ItemStack createRod(FishingRod rod, LangManager lang) {

        Material material = parseMaterial(rod.material(), Material.FISHING_ROD);

        List<Component> lore = new ArrayList<>();

        if (!rod.description().isBlank()) {
            lore.add(ComponentUtils.parse(rod.description()).colorIfAbsent(NamedTextColor.GRAY));
            lore.add(Component.empty());
        }

        lore.add(lang.component("item.rod.precision", "value", String.format(Locale.ROOT, "%.1f", rod.precision())));
        lore.add(lang.component("item.rod.luck", "value", String.format(Locale.ROOT, "%.2f", rod.luckBonus())));
        lore.add(lang.component("item.rod.resistance", "value", String.format(Locale.ROOT, "%.2f", rod.resistance())));
        lore.add(lang.component("item.rod.reel_speed", "value", String.format(Locale.ROOT, "%.2f", rod.reelSpeed())));

        if (!rod.preferredCategories().isEmpty()) {
            lore.add(lang.component("item.rod.preferred_categories", "value",
                    String.join(", ", rod.preferredCategories())));
        }

        lore.add(Component.empty());
        lore.add(lang.component("item.rod.footer"));

        ItemStack item = new ItemBuilder(material)
                .setName(ComponentUtils.parse(rod.displayName()).colorIfAbsent(NamedTextColor.WHITE))
                .setLore(lore)
                .build();

        return tag(item, FishingItemKeys.ROD_ID, rod.id());
    }

    public static ItemStack createBait(Bait bait, LangManager lang) {

        Material material = parseMaterial(bait.material(), Material.STRING);

        List<Component> lore = new ArrayList<>();

        if (!bait.description().isBlank()) {
            lore.add(ComponentUtils.parse(bait.description()).colorIfAbsent(NamedTextColor.GRAY));
            lore.add(Component.empty());
        }

        if (!bait.tags().isEmpty()) {
            lore.add(lang.component("item.bait.attracts", "value", String.join(", ", bait.tags())));
        }

        if (bait.qualityBonus() > 0) {
            lore.add(lang.component("item.bait.quality_bonus", "value",
                    String.format(Locale.ROOT, "%.1f", bait.qualityBonus())));
        }

        if (bait.legendaryWeightMultiplier() != 1.0) {
            lore.add(lang.component("item.bait.legendary_mult", "value",
                    String.format(Locale.ROOT, "%.1f", bait.legendaryWeightMultiplier())));
        }

        lore.add(Component.empty());
        lore.add(lang.component("item.bait.footer"));

        ItemStack item = new ItemBuilder(material)
                .setName(ComponentUtils.parse(bait.displayName()).colorIfAbsent(NamedTextColor.WHITE))
                .setLore(lore)
                .build();

        return tag(item, FishingItemKeys.BAIT_ID, bait.id());
    }

    /** @return el ItemStack final de una captura resuelta, o null para {@code NOTHING}. */
    public static ItemStack createCatchItem(CatchResult result, LangManager lang) {
        return switch (result.outcome()) {
            case FISH -> createFishItem(result, lang);
            case TREASURE -> createTreasureItem(result.treasure(), lang);
            case JUNK -> createJunkItem(result.junk(), lang);
            case NOTHING -> null;
        };
    }

    private static ItemStack createFishItem(CatchResult result, LangManager lang) {

        var species = result.species();
        Material material = parseMaterial(species.icon(), Material.COD);

        List<Component> lore = new ArrayList<>();
        lore.add(lang.component("item.catch.weight", "value", String.format(Locale.ROOT, "%.2f", result.weight())));
        lore.add(lang.component("item.catch.length", "value", String.format(Locale.ROOT, "%.1f", result.length())));
        lore.add(lang.component("item.catch.quality", "value", result.quality()));
        lore.add(lang.component("item.catch.rarity", "value", species.rarity()));
        lore.add(lang.component("item.catch.price", "value", String.format(Locale.ROOT, "%.1f", result.price())));

        var builder = new ItemBuilder(material)
                .setName(ComponentUtils.parse(species.displayName()).colorIfAbsent(NamedTextColor.WHITE))
                .setLore(lore);

        ItemStack item = builder.build();

        if (species.customModelData() > 0) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(species.customModelData());
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    private static ItemStack createTreasureItem(Treasure treasure, LangManager lang) {

        Material material = parseMaterial(treasure.rewardMaterial(), Material.CHEST);

        List<Component> lore = new ArrayList<>();

        if (!treasure.description().isBlank()) {
            lore.add(ComponentUtils.parse(treasure.description()).colorIfAbsent(NamedTextColor.GRAY));
        }

        lore.add(lang.component("item.treasure.rarity", "value", treasure.rarity()));

        return new ItemBuilder(material, treasure.rewardAmount())
                .setName(ComponentUtils.parse(treasure.displayName()).colorIfAbsent(NamedTextColor.GOLD))
                .setLore(lore)
                .build();
    }

    private static ItemStack createJunkItem(Junk junk, LangManager lang) {

        Material material = parseMaterial(junk.icon(), Material.LEATHER_BOOTS);

        List<Component> lore = new ArrayList<>();

        if (!junk.description().isBlank()) {
            lore.add(ComponentUtils.parse(junk.description()).colorIfAbsent(NamedTextColor.GRAY));
        }

        return new ItemBuilder(material)
                .setName(ComponentUtils.parse(junk.displayName()).colorIfAbsent(NamedTextColor.GRAY))
                .setLore(lore)
                .build();
    }

    public static String getRodId(ItemStack item) {
        return readTag(item, FishingItemKeys.ROD_ID);
    }

    public static String getBaitId(ItemStack item) {
        return readTag(item, FishingItemKeys.BAIT_ID);
    }

    private static ItemStack tag(ItemStack item, NamespacedKey key, String value) {

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
            item.setItemMeta(meta);
        }

        return item;
    }

    private static String readTag(ItemStack item, NamespacedKey key) {

        if (item == null || item.getType().isAir()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return null;
        }

        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private static Material parseMaterial(String raw, Material fallback) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

}
