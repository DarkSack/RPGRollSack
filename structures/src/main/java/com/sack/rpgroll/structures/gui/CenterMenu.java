package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import com.sack.rpgroll.structures.workbench.AccessRules;
import com.sack.rpgroll.structures.workbench.WorkbenchService;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Menú principal de la mesa (6 filas). Las casillas son constantes: el plan 8 las copia al generador de fondos. */
public final class CenterMenu extends InventoryGUI {
    public static final int NIVEL = 4;
    public static final int CATALOGO = 20;
    public static final int MIS_CONSTRUCCIONES = 22;
    public static final int MATERIALES = 24;
    public static final int OBRA = 29;
    public static final int MEJORAS = 31;
    public static final int ACCESO = 33;
    public static final int AYUDA = 49;
    public static final int CERRAR = 53;

    /** Todas las casillas ocupadas, para comprobar que no se repiten. */
    public static final List<Integer> SLOTS = List.of(NIVEL, CATALOGO, MIS_CONSTRUCCIONES, MATERIALES, OBRA,
            MEJORAS, ACCESO, AYUDA, CERRAR);

    private final StructuresPlugin plugin;
    private final UUID benchId;
    private final WorkbenchTier tier;

    public CenterMenu(Player player, StructuresPlugin plugin, WorkbenchRecord bench) {
        super(player, StructuresMenus.skin().title(StructuresMenus.background(StructuresMenus.CENTRO, bench.tier()),
                plugin.menuTitle("center")), 54);
        this.plugin = plugin;
        this.benchId = bench.id();
        this.tier = bench.tier();
    }

    @Override
    public void build() {
        StructuresMenus.fill(inventory);
        var lang = plugin.lang();
        Component soon = lang.component("menu.coming_soon");
        put(NIVEL, StructuresMenus.tierIcon(plugin, tier));
        put(CATALOGO, StructuresMenus.item(Material.BOOKSHELF, lang.component("menu.center.catalog"),
                List.of(lang.component("menu.center.catalog-lore"), soon)));
        put(MIS_CONSTRUCCIONES, StructuresMenus.item(Material.CHEST, lang.component("menu.center.builds"),
                List.of(lang.component("menu.center.builds-lore"), soon)));
        put(MATERIALES, StructuresMenus.item(Material.BARREL, lang.component("menu.center.materials"),
                List.of(lang.component("menu.center.materials-lore"), soon)));
        put(OBRA, StructuresMenus.item(Material.IRON_PICKAXE, lang.component("menu.center.project"),
                List.of(lang.component("menu.center.project-lore"), soon)));
        put(MEJORAS, StructuresMenus.item(Material.ANVIL, lang.component("menu.center.upgrades"),
                List.of(lang.component("menu.center.upgrades-lore"))));
        put(ACCESO, StructuresMenus.item(Material.PLAYER_HEAD, lang.component("menu.center.access"),
                List.of(lang.component("menu.center.access-lore"))));
        put(AYUDA, StructuresMenus.item(Material.BOOK, lang.component("menu.center.help"),
                List.of(lang.component("menu.center.help-lore"))));
        put(CERRAR, StructuresMenus.item(Material.BARRIER, lang.component("menu.close"),
                List.of(lang.component("menu.close-lore"))));
    }

    private void put(int slot, ItemStack item) {
        setItem(slot, StructuresMenus.CENTRO.dress(slot, item));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        Optional<WorkbenchRecord> bench = plugin.service().byId(benchId);
        if (bench.isEmpty()) {
            close();
            return;
        }
        boolean manage = AccessRules.canManage(player.getUniqueId(), bench.get(), Set.of(),
                player.hasPermission(WorkbenchService.PERM_BYPASS));
        switch (slot) {
            case MEJORAS -> {
                if (manage) {
                    new UpgradeMenu(player, plugin, bench.get()).open();
                } else {
                    plugin.lang().send(player, "menu.manager-only");
                }
            }
            case ACCESO -> {
                if (manage) {
                    AccessMenu.show(player, plugin, bench.get(), 0);
                } else {
                    plugin.lang().send(player, "menu.manager-only");
                }
            }
            case AYUDA -> new HelpMenu(player, plugin, bench.get(), 0).open();
            case CERRAR -> close();
            default -> { }
        }
    }
}
