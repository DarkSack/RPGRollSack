package com.sack.rpgroll.ranching.item;

import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.health.Medicine;
import com.sack.rpgroll.ranching.core.health.Vaccine;
import com.sack.rpgroll.ranching.core.nutrition.Feed;

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

/** Construye los ItemStacks de alimentos/medicinas/vacunas, etiquetados vía PersistentDataContainer. */
public final class RanchingItemFactory {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private RanchingItemFactory() {
    }

    public static ItemStack createFeed(Feed feed) {

        Material material = parseMaterial(feed.icon(), Material.WHEAT);

        List<Component> lore = new ArrayList<>();

        if (!feed.description().isBlank()) {
            lore.add(LEGACY.deserialize(feed.description()).colorIfAbsent(NamedTextColor.GRAY));
            lore.add(Component.empty());
        }

        lore.add(Component.text("Calidad: " + feed.quality(), NamedTextColor.YELLOW));
        lore.add(Component.text(String.format(Locale.ROOT, "Nutrición: +%.1f", feed.nutritionValue()), NamedTextColor.AQUA));

        if (feed.healthBonus() > 0) {
            lore.add(Component.text(String.format(Locale.ROOT, "Salud: +%.1f", feed.healthBonus()), NamedTextColor.RED));
        }

        if (feed.happinessBonus() > 0) {
            lore.add(Component.text(String.format(Locale.ROOT, "Felicidad: +%.1f", feed.happinessBonus()),
                    NamedTextColor.LIGHT_PURPLE));
        }

        if (!feed.tags().isEmpty()) {
            lore.add(Component.text("Tags: " + String.join(", ", feed.tags()), NamedTextColor.DARK_GRAY));
        }

        lore.add(Component.empty());
        lore.add(Component.text("Click derecho en un animal para alimentarlo", NamedTextColor.DARK_GRAY));

        ItemStack item = new ItemBuilder(material)
                .setName(LEGACY.deserialize(feed.displayName()).colorIfAbsent(NamedTextColor.WHITE))
                .setLore(lore)
                .build();

        return tag(item, RanchingItemKeys.FEED_ID, feed.id());
    }

    public static ItemStack createMedicine(Medicine medicine) {

        Material material = parseMaterial(medicine.icon(), Material.POTION);

        List<Component> lore = new ArrayList<>();

        if (!medicine.description().isBlank()) {
            lore.add(LEGACY.deserialize(medicine.description()).colorIfAbsent(NamedTextColor.GRAY));
            lore.add(Component.empty());
        }

        lore.add(Component.text("Tipo: " + medicine.type(), NamedTextColor.AQUA));

        if (!medicine.curesDiseaseIds().isEmpty()) {
            lore.add(Component.text("Trata: " + String.join(", ", medicine.curesDiseaseIds()), NamedTextColor.GREEN));
        }

        lore.add(Component.empty());
        lore.add(Component.text("Click derecho en un animal para tratarlo", NamedTextColor.DARK_GRAY));

        ItemStack item = new ItemBuilder(material)
                .setName(LEGACY.deserialize(medicine.displayName()).colorIfAbsent(NamedTextColor.WHITE))
                .setLore(lore)
                .build();

        return tag(item, RanchingItemKeys.MEDICINE_ID, medicine.id());
    }

    public static ItemStack createVaccine(Vaccine vaccine) {

        Material material = parseMaterial(vaccine.icon(), Material.POTION);

        List<Component> lore = new ArrayList<>();

        if (!vaccine.description().isBlank()) {
            lore.add(LEGACY.deserialize(vaccine.description()).colorIfAbsent(NamedTextColor.GRAY));
            lore.add(Component.empty());
        }

        if (!vaccine.preventsDiseaseIds().isEmpty()) {
            lore.add(Component.text("Previene: " + String.join(", ", vaccine.preventsDiseaseIds()), NamedTextColor.GREEN));
        }

        lore.add(Component.text(vaccine.isPermanent() ? "Inmunidad permanente" : "Inmunidad temporal", NamedTextColor.AQUA));
        lore.add(Component.empty());
        lore.add(Component.text("Click derecho en un animal para vacunarlo", NamedTextColor.DARK_GRAY));

        ItemStack item = new ItemBuilder(material)
                .setName(LEGACY.deserialize(vaccine.displayName()).colorIfAbsent(NamedTextColor.WHITE))
                .setLore(lore)
                .build();

        return tag(item, RanchingItemKeys.VACCINE_ID, vaccine.id());
    }

    public static String getFeedId(ItemStack item) {
        return readTag(item, RanchingItemKeys.FEED_ID);
    }

    public static String getMedicineId(ItemStack item) {
        return readTag(item, RanchingItemKeys.MEDICINE_ID);
    }

    public static String getVaccineId(ItemStack item) {
        return readTag(item, RanchingItemKeys.VACCINE_ID);
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
