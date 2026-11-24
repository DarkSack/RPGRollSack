package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.gui.MenuSkin;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Skin e ids de fondo de los menús de la mesa. El plan 8 dibuja los fondos con estos ids. */
public final class StructuresMenus {
    private StructuresMenus() {}

    /** {@code centro_taller}, {@code centro_ingenio}, {@code centro_arcano}... */
    public static String background(String menu, WorkbenchTier tier) {
        return menu + "_" + family(tier);
    }

    public static String family(WorkbenchTier tier) {
        return switch (tier) {
            case CONSTRUCTOR -> "taller";
            case INGENIERO -> "ingenio";
            case ARQUITECTO -> "arcano";
        };
    }

    public static MenuSkin skin() {
        return MenuSkin.get(StructuresPlugin.MENU_MODULE);
    }

    public static ItemStack filler() {
        return skin().filler(ItemBuilder.createFiller());
    }

    public static void fill(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler());
        }
    }

    /** Botón de los que no cambian: invisible con fondo dibujado, normal sin él. */
    public static ItemStack button(Material icon, Component name, List<Component> lore) {
        return skin().button(new ItemBuilder(icon).setName(name).setLore(lore).build());
    }
}
