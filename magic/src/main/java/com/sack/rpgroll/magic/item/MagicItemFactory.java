package com.sack.rpgroll.magic.item;

import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.Grimoire;
import com.sack.rpgroll.magic.core.SpellCatalyst;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Construye los ItemStacks de catalizadores y grimorios, etiquetados vía
 * PersistentDataContainer — mismo enfoque que usa Items (ItemFactory) para
 * reconocer sus propios ítems custom en eventos de interacción.
 */
public final class MagicItemFactory {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private MagicItemFactory() {
    }

    public static ItemStack createCatalyst(SpellCatalyst catalyst) {

        Material material = parseMaterial(catalyst.material(), Material.BLAZE_ROD);

        List<Component> lore = new ArrayList<>();

        if (!catalyst.description().isBlank()) {
            lore.add(LEGACY.deserialize(catalyst.description()).colorIfAbsent(NamedTextColor.GRAY));
            lore.add(Component.empty());
        }

        if (catalyst.powerMultiplier() != 1.0) {
            lore.add(Component.text(String.format(Locale.ROOT, "Poder: x%.2f", catalyst.powerMultiplier()),
                    NamedTextColor.YELLOW));
        }

        if (catalyst.costMultiplier() != 1.0) {
            lore.add(Component.text(String.format(Locale.ROOT, "Costo de maná: x%.2f", catalyst.costMultiplier()),
                    NamedTextColor.AQUA));
        }

        if (catalyst.rangeMultiplier() != 1.0) {
            lore.add(Component.text(String.format(Locale.ROOT, "Alcance: x%.2f", catalyst.rangeMultiplier()),
                    NamedTextColor.LIGHT_PURPLE));
        }

        lore.add(Component.empty());
        lore.add(Component.text("Catalizador de RPGRoll-Magic", NamedTextColor.DARK_GRAY));

        ItemStack item = new ItemBuilder(material)
                .setName(LEGACY.deserialize(catalyst.displayName()).colorIfAbsent(NamedTextColor.WHITE))
                .setLore(lore)
                .build();

        return tag(item, MagicItemKeys.CATALYST_ID, catalyst.id());
    }

    public static ItemStack createGrimoire(Grimoire grimoire) {

        Material material = parseMaterial(grimoire.icon(), Material.WRITTEN_BOOK);

        List<Component> lore = new ArrayList<>();

        if (!grimoire.description().isBlank()) {
            lore.add(LEGACY.deserialize(grimoire.description()).colorIfAbsent(NamedTextColor.GRAY));
            lore.add(Component.empty());
        }

        if (grimoire.requiredLevel() > 0) {
            lore.add(Component.text("Nivel requerido: " + grimoire.requiredLevel(), NamedTextColor.YELLOW));
        }

        lore.add(Component.text(grimoire.spellIds().size() + " hechizo(s)", NamedTextColor.AQUA));
        lore.add(Component.empty());
        lore.add(Component.text("Click derecho para aprender — se consume al usarse", NamedTextColor.DARK_GRAY));

        ItemStack item = new ItemBuilder(material)
                .setName(LEGACY.deserialize(grimoire.displayName()).colorIfAbsent(NamedTextColor.LIGHT_PURPLE))
                .setLore(lore)
                .build();

        return tag(item, MagicItemKeys.GRIMOIRE_ID, grimoire.id());
    }

    public static String getCatalystId(ItemStack item) {
        return readTag(item, MagicItemKeys.CATALYST_ID);
    }

    public static String getGrimoireId(ItemStack item) {
        return readTag(item, MagicItemKeys.GRIMOIRE_ID);
    }

    private static ItemStack tag(ItemStack item, org.bukkit.NamespacedKey key, String value) {

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
            item.setItemMeta(meta);
        }

        return item;
    }

    private static String readTag(ItemStack item, org.bukkit.NamespacedKey key) {

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
