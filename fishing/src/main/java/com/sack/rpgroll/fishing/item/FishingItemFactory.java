package com.sack.rpgroll.fishing.item;

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

    public static ItemStack createRod(FishingRod rod) {

        Material material = parseMaterial(rod.material(), Material.FISHING_ROD);

        List<Component> lore = new ArrayList<>();

        if (!rod.description().isBlank()) {
            lore.add(ComponentUtils.parse(rod.description()).colorIfAbsent(NamedTextColor.GRAY));
            lore.add(Component.empty());
        }

        lore.add(Component.text(String.format(Locale.ROOT, "Precisión: +%.1f", rod.precision()), NamedTextColor.AQUA));
        lore.add(Component.text(String.format(Locale.ROOT, "Suerte: x%.2f", rod.luckBonus()), NamedTextColor.YELLOW));
        lore.add(Component.text(String.format(Locale.ROOT, "Resistencia: x%.2f", rod.resistance()), NamedTextColor.GREEN));
        lore.add(Component.text(String.format(Locale.ROOT, "Velocidad de recogida: x%.2f", rod.reelSpeed()),
                NamedTextColor.LIGHT_PURPLE));

        if (!rod.preferredCategories().isEmpty()) {
            lore.add(Component.text("Especies favoritas: " + String.join(", ", rod.preferredCategories()),
                    NamedTextColor.GOLD));
        }

        lore.add(Component.empty());
        lore.add(Component.text("Caña de RPGRoll-Fishing", NamedTextColor.DARK_GRAY));

        ItemStack item = new ItemBuilder(material)
                .setName(ComponentUtils.parse(rod.displayName()).colorIfAbsent(NamedTextColor.WHITE))
                .setLore(lore)
                .build();

        return tag(item, FishingItemKeys.ROD_ID, rod.id());
    }

    public static ItemStack createBait(Bait bait) {

        Material material = parseMaterial(bait.material(), Material.STRING);

        List<Component> lore = new ArrayList<>();

        if (!bait.description().isBlank()) {
            lore.add(ComponentUtils.parse(bait.description()).colorIfAbsent(NamedTextColor.GRAY));
            lore.add(Component.empty());
        }

        if (!bait.tags().isEmpty()) {
            lore.add(Component.text("Atrae: " + String.join(", ", bait.tags()), NamedTextColor.AQUA));
        }

        if (bait.qualityBonus() > 0) {
            lore.add(Component.text(String.format(Locale.ROOT, "Bono de calidad: +%.1f", bait.qualityBonus()),
                    NamedTextColor.YELLOW));
        }

        if (bait.legendaryWeightMultiplier() != 1.0) {
            lore.add(Component.text(
                    String.format(Locale.ROOT, "Chance de legendario: x%.1f", bait.legendaryWeightMultiplier()),
                    NamedTextColor.LIGHT_PURPLE));
        }

        lore.add(Component.empty());
        lore.add(Component.text("Sostenela en la mano secundaria mientras pescás", NamedTextColor.DARK_GRAY));

        ItemStack item = new ItemBuilder(material)
                .setName(ComponentUtils.parse(bait.displayName()).colorIfAbsent(NamedTextColor.WHITE))
                .setLore(lore)
                .build();

        return tag(item, FishingItemKeys.BAIT_ID, bait.id());
    }

    /** @return el ItemStack final de una captura resuelta, o null para {@code NOTHING}. */
    public static ItemStack createCatchItem(CatchResult result) {
        return switch (result.outcome()) {
            case FISH -> createFishItem(result);
            case TREASURE -> createTreasureItem(result.treasure());
            case JUNK -> createJunkItem(result.junk());
            case NOTHING -> null;
        };
    }

    private static ItemStack createFishItem(CatchResult result) {

        var species = result.species();
        Material material = parseMaterial(species.icon(), Material.COD);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(String.format(Locale.ROOT, "Peso: %.2f kg", result.weight()), NamedTextColor.GRAY));
        lore.add(Component.text(String.format(Locale.ROOT, "Longitud: %.1f cm", result.length()), NamedTextColor.GRAY));
        lore.add(Component.text("Calidad: " + result.quality(), NamedTextColor.YELLOW));
        lore.add(Component.text("Rareza: " + species.rarity(), NamedTextColor.LIGHT_PURPLE));
        lore.add(Component.text(String.format(Locale.ROOT, "Valor estimado: %.1f", result.price()), NamedTextColor.GOLD));

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

    private static ItemStack createTreasureItem(Treasure treasure) {

        Material material = parseMaterial(treasure.rewardMaterial(), Material.CHEST);

        List<Component> lore = new ArrayList<>();

        if (!treasure.description().isBlank()) {
            lore.add(ComponentUtils.parse(treasure.description()).colorIfAbsent(NamedTextColor.GRAY));
        }

        lore.add(Component.text("Tesoro: " + treasure.rarity(), NamedTextColor.GOLD));

        return new ItemBuilder(material, treasure.rewardAmount())
                .setName(ComponentUtils.parse(treasure.displayName()).colorIfAbsent(NamedTextColor.GOLD))
                .setLore(lore)
                .build();
    }

    private static ItemStack createJunkItem(Junk junk) {

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
