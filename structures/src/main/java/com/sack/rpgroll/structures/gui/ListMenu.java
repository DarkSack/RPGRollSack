package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.catalog.StructurePerms;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Estructuras de una categoría, de 28 en 28. Las de nivel mayor que la mesa, o sin permiso, bloqueadas. 6 filas. */
public final class ListMenu extends InventoryGUI {
    public static final int PAGE_SIZE = 28;
    public static final List<Integer> CONTENT = StructuresMenus.grid(1, 4);
    public static final int CATEGORIA = 4;
    public static final int ANTERIOR = 45;
    public static final int VOLVER = 49;
    public static final int SIGUIENTE = 53;

    public static final String PERM_BUILD = "rpgrollstructures.build";

    /**
     * Clave del motivo por el que no se puede elegir, o null si se puede. {@code bench} null = sin mesa (no hay
     * nivel con el que comparar).
     */
    public static String lockReason(WorkbenchTier needed, WorkbenchTier bench, boolean permitted) {
        if (!permitted) {
            return "catalog.locked-permission";
        }
        return bench != null && needed.level() > bench.level() ? "catalog.locked-tier" : null;
    }

    /** {@code rpgrollstructures.build} y el permiso de la estructura (permitido salvo negado). */
    public static boolean permitted(Player p, StructureDefinition def) {
        return p.hasPermission(PERM_BUILD) && StructurePerms.allowed(p, def.permission());
    }

    private final StructuresPlugin plugin;
    private final UUID benchId;
    private final WorkbenchTier tier;
    private final String category;
    private final List<StructureDefinition> all;
    private final int page;

    public ListMenu(Player player, StructuresPlugin plugin, UUID benchId, WorkbenchTier tier, String category,
                    int page) {
        super(player, StructuresMenus.title(plugin, StructuresMenus.LISTA, tier, "list"), 54);
        this.plugin = plugin;
        this.benchId = benchId;
        this.tier = tier;
        this.category = category;
        this.all = plugin.structures().inCategory(category);
        this.page = Math.max(0, Math.min(page, StructuresMenus.pageCount(all.size(), PAGE_SIZE) - 1));
    }

    private String reason(StructureDefinition def) {
        return lockReason(def.tier(), benchId == null ? null : tier, permitted(player, def));
    }

    @Override
    public void build() {
        StructuresMenus.fill(inventory);
        var lang = plugin.lang();
        String language = plugin.settings().language();
        plugin.categories().byId(category).ifPresent(c -> put(CATEGORIA, StructuresMenus.icon(plugin, c.icon(),
                lang.component("category." + c.id()), List.of())));
        List<StructureDefinition> shown = StructuresMenus.pageOf(all, page, PAGE_SIZE);
        for (int i = 0; i < shown.size(); i++) {
            StructureDefinition def = shown.get(i);
            List<Component> lore = new ArrayList<>();
            lore.add(lang.component("menu.list.tier", "tier", StructuresMenus.tierName(plugin, def.tier())));
            lore.add(lang.component("menu.list.size", "width", def.blocks().width(), "height",
                    def.blocks().height(), "length", def.blocks().length()));
            String why = reason(def);
            Component name = Component.text(def.name(language));
            if (why == null) {
                lore.add(lang.component("menu.list.open"));
                put(CONTENT.get(i), StructuresMenus.icon(plugin, def.icon(), name, lore));
            } else {
                lore.add(lang.component(why));
                put(CONTENT.get(i), StructuresMenus.item(Material.BARRIER, name, lore));
            }
        }
        if (page > 0) {
            put(ANTERIOR, StructuresMenus.item(Material.ARROW, lang.component("menu.previous"),
                    List.of(lang.component("menu.previous-lore"))));
        }
        if (page < StructuresMenus.pageCount(all.size(), PAGE_SIZE) - 1) {
            put(SIGUIENTE, StructuresMenus.item(Material.ARROW, lang.component("menu.next"),
                    List.of(lang.component("menu.next-lore"))));
        }
        put(VOLVER, StructuresMenus.item(Material.ARROW, lang.component("menu.back"),
                List.of(lang.component("menu.back-lore"))));
    }

    private void put(int slot, ItemStack item) {
        setItem(slot, StructuresMenus.LISTA.dress(slot, item));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        List<StructureDefinition> shown = StructuresMenus.pageOf(all, page, PAGE_SIZE);
        int i = CONTENT.indexOf(slot);
        if (i >= 0 && i < shown.size()) {
            StructureDefinition def = shown.get(i);
            String why = reason(def);
            if (why == null) {
                new DetailMenu(player, plugin, benchId, tier, def).open();
            } else {
                plugin.lang().send(player, why);
            }
        } else if (slot == ANTERIOR && page > 0) {
            new ListMenu(player, plugin, benchId, tier, category, page - 1).open();
        } else if (slot == SIGUIENTE && page < StructuresMenus.pageCount(all.size(), PAGE_SIZE) - 1) {
            new ListMenu(player, plugin, benchId, tier, category, page + 1).open();
        } else if (slot == VOLVER) {
            new CategoriesMenu(player, plugin, benchId, tier).open();
        }
    }
}
