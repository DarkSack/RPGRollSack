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
import java.util.Locale;
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
        super(player, StructuresMenus.skin().title(StructuresMenus.background("centro", bench.tier()),
                plugin.menuTitle("center")), 54);
        this.plugin = plugin;
        this.benchId = bench.id();
        this.tier = bench.tier();
    }

    @Override
    public void build() {
        StructuresMenus.fill(inventory);
        Component soon = plugin.lang().component("menu.coming_soon");
        setItem(NIVEL, StructuresMenus.button(Material.NETHER_STAR, plugin.lang().component("menu.center.level",
                "tier", plugin.lang().raw("tier." + tier.name().toLowerCase(Locale.ROOT))), List.of()));
        setItem(CATALOGO, soonButton(Material.BOOKSHELF, "menu.center.catalog", soon));
        setItem(MIS_CONSTRUCCIONES, soonButton(Material.CHEST, "menu.center.builds", soon));
        setItem(MATERIALES, soonButton(Material.BARREL, "menu.center.materials", soon));
        setItem(OBRA, soonButton(Material.IRON_PICKAXE, "menu.center.project", soon));
        setItem(MEJORAS, button(Material.ANVIL, "menu.center.upgrades"));
        setItem(ACCESO, button(Material.PLAYER_HEAD, "menu.center.access"));
        setItem(AYUDA, button(Material.BOOK, "menu.center.help"));
        setItem(CERRAR, button(Material.BARRIER, "menu.close"));
    }

    private ItemStack soonButton(Material icon, String key, Component soon) {
        return StructuresMenus.button(icon, plugin.lang().component(key), List.of(soon));
    }

    private ItemStack button(Material icon, String key) {
        return StructuresMenus.button(icon, plugin.lang().component(key), List.of());
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
