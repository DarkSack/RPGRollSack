package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.catalog.StructurePerms;
import com.sack.rpgroll.structures.config.CategorySettings.Category;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Catálogo: una casilla por categoría con estructuras. Sin {@code .category.<cat>} se ve bloqueada. 6 filas. */
public final class CategoriesMenu extends InventoryGUI {
    public static final List<Integer> CONTENT = StructuresMenus.grid(1, 3);
    public static final int VOLVER = 49;

    public static final String PERM_CATEGORY = "rpgrollstructures.category.";

    private final StructuresPlugin plugin;
    /** Null si se abrió con /estructuras abrir: catálogo para mirar, sin elegir. */
    private final UUID benchId;
    private final WorkbenchTier tier;
    private final List<Category> shown;

    public CategoriesMenu(Player player, StructuresPlugin plugin, UUID benchId, WorkbenchTier tier) {
        super(player, StructuresMenus.title(plugin, StructuresMenus.CATEGORIAS, tier, "categories"), 54);
        this.plugin = plugin;
        this.benchId = benchId;
        this.tier = tier;
        this.shown = plugin.categories().categories().stream()
                .filter(c -> !plugin.structures().inCategory(c.id()).isEmpty())
                .limit(CONTENT.size()).toList();
    }

    public static boolean allowed(Player p, String category) {
        return StructurePerms.allowed(p, PERM_CATEGORY + category);
    }

    @Override
    public void build() {
        StructuresMenus.CATEGORIAS.fill(inventory);
        var lang = plugin.lang();
        for (int i = 0; i < shown.size(); i++) {
            Category c = shown.get(i);
            Component name = lang.component("category." + c.id());
            List<Component> lore = new ArrayList<>();
            lore.add(lang.component("menu.categories.count", "amount",
                    plugin.structures().inCategory(c.id()).size()));
            if (allowed(player, c.id())) {
                lore.add(lang.component("menu.categories.open"));
                put(CONTENT.get(i), StructuresMenus.icon(plugin, c.icon(), name, lore));
            } else {
                lore.add(lang.component("catalog.locked-category"));
                put(CONTENT.get(i), StructuresMenus.item(Material.BARRIER, name, lore));
            }
        }
        if (shown.isEmpty()) {
            put(CONTENT.get(CONTENT.size() / 2), StructuresMenus.item(Material.PAPER,
                    lang.component("catalog.empty"), List.of()));
        }
        put(VOLVER, StructuresMenus.backOrClose(plugin, benchId));
    }

    private void put(int slot, ItemStack item) {
        setItem(slot, StructuresMenus.CATEGORIAS.dress(slot, item));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        int i = CONTENT.indexOf(slot);
        if (i >= 0 && i < shown.size()) {
            String cat = shown.get(i).id();
            if (allowed(player, cat)) {
                new ListMenu(player, plugin, benchId, tier, cat, 0).open();
            } else {
                plugin.lang().send(player, "catalog.locked-category");
            }
        } else if (slot == VOLVER) {
            StructuresMenus.back(player, plugin, benchId);
        }
    }
}
