package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.gui.MenuSkin;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.config.WorkbenchSettings.Size;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Skin, ids de fondo y casillas de los menús de la mesa. El plan 8 dibuja los fondos con estos datos. */
public final class StructuresMenus {
    private StructuresMenus() {}

    /**
     * Casillas de un menú con fondo dibujado. {@code painted}: botones fijos cuyo dibujo va en el fondo (el ítem
     * es invisible y solo deja el tooltip). {@code visible}: ítems que se ven sobre un hueco dibujado (cambian o
     * dependen de la mesa). El resto de casillas es relleno o contenido. El generador de fondos copia esta tabla.
     */
    public record Layout(String id, int rows, List<Integer> painted, List<Integer> visible) {
        public boolean isPainted(int slot) {
            return painted.contains(slot);
        }

        /** El ítem tal como va en {@code slot}: invisible si su dibujo está en el fondo. */
        public ItemStack dress(int slot, ItemStack item) {
            return isPainted(slot) ? skin().button(item) : item;
        }
    }

    public static final Layout CENTRO = new Layout("centro", 6, List.of(49, 53),
            List.of(4, 20, 22, 24, 29, 31, 33));
    public static final Layout MEJORAS = new Layout("mejoras", 4, List.of(31),
            Stream.concat(Stream.of(4, 22), IntStream.rangeClosed(10, 16).boxed()).toList());
    public static final Layout ACCESO = new Layout("acceso", 6, List.of(48),
            Stream.concat(IntStream.range(0, 45).boxed(), Stream.of(45, 50, 53)).toList());
    public static final Layout AYUDA = new Layout("ayuda", 3, List.of(), List.of(13, 18, 22, 26));
    public static final Layout CONFIRMAR = new Layout("confirmar", 3, List.of(11, 15), List.of(13));

    public static final List<Layout> LAYOUTS = List.of(CENTRO, MEJORAS, ACCESO, AYUDA, CONFIRMAR);

    /** {@code centro_taller}, {@code centro_ingenio}, {@code centro_arcano}... */
    public static String background(String menu, WorkbenchTier tier) {
        return menu + "_" + family(tier);
    }

    public static String background(Layout menu, WorkbenchTier tier) {
        return background(menu.id(), tier);
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

    /** Sin cursiva: el cliente pone en cursiva todo nombre y lore propio que no diga lo contrario. */
    public static Component plain(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> plain(List<Component> lines) {
        return lines.stream().map(StructuresMenus::plain).toList();
    }

    /** Relleno sin recuadro de nombre (sin pack se veía un tooltip vacío, y su id con F3+H). */
    public static ItemStack filler() {
        ItemStack plain = ItemBuilder.createFiller();
        ItemMeta meta = plain.getItemMeta();
        meta.setHideTooltip(true);
        plain.setItemMeta(meta);
        return skin().filler(plain);
    }

    public static void fill(Inventory inv) {
        ItemStack filler = filler();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler.clone());
        }
    }

    /** Un ítem de menú con nombre y lore sin cursiva. Visible: para un botón dibujado, {@link Layout#dress}. */
    public static ItemStack item(Material icon, Component name, List<Component> lore) {
        return new ItemBuilder(icon).setName(plain(name)).setLore(plain(lore)).build();
    }

    /** Icono del nivel (casilla 4 del centro y de mejoras): tamaño máximo y nivel siguiente. */
    public static ItemStack tierIcon(StructuresPlugin plugin, WorkbenchTier tier) {
        var lang = plugin.lang();
        Size size = plugin.workbench().tiers().get(tier).maxSize();
        List<Component> lore = new ArrayList<>();
        lore.add(lang.component("menu.center.level-size", "width", size.width(), "height", size.height(),
                "length", size.length()));
        lore.add(tier.next()
                .map(n -> lang.component("menu.center.level-next", "tier", tierName(plugin, n)))
                .orElseGet(() -> lang.component("menu.center.level-max")));
        return item(Material.NETHER_STAR, lang.component("menu.center.level", "tier", tierName(plugin, tier)), lore);
    }

    public static String tierName(StructuresPlugin plugin, WorkbenchTier tier) {
        return plugin.lang().raw("tier." + tier.name().toLowerCase(Locale.ROOT));
    }
}
