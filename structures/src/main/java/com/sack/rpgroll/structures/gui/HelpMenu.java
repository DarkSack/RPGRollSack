package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/** Ayuda: una página de texto por pantalla, de las claves {@code help.page.<n>} del lang. 3 filas. */
public final class HelpMenu extends InventoryGUI {
    /** Páginas de ayuda del lang ({@code help.page.1} a {@code help.page.N}). */
    public static final int PAGES = 3;
    public static final int ANTERIOR = 18;
    public static final int TEXTO = 13;
    public static final int VOLVER = 22;
    public static final int SIGUIENTE = 26;

    private final StructuresPlugin plugin;
    /** Null si se abrió con /estructuras, sin mesa: no hay a dónde volver. */
    private final UUID benchId;
    private final WorkbenchTier tier;
    private final int page;

    public HelpMenu(Player player, StructuresPlugin plugin, WorkbenchRecord bench, int page) {
        this(player, plugin, bench.id(), bench.tier(), page);
    }

    /** Ayuda suelta (comando): fondo del nivel 1 y sin botón de volver a una mesa. */
    public HelpMenu(Player player, StructuresPlugin plugin, int page) {
        this(player, plugin, null, WorkbenchTier.CONSTRUCTOR, page);
    }

    private HelpMenu(Player player, StructuresPlugin plugin, UUID benchId, WorkbenchTier tier, int page) {
        super(player, StructuresMenus.skin().title(StructuresMenus.background(StructuresMenus.AYUDA, tier),
                plugin.menuTitle("help")), 27);
        this.plugin = plugin;
        this.benchId = benchId;
        this.tier = tier;
        this.page = Math.max(0, Math.min(page, PAGES - 1));
    }

    @Override
    public void build() {
        StructuresMenus.fill(inventory);
        var lang = plugin.lang();
        put(TEXTO, StructuresMenus.item(Material.BOOK, lang.component("help.page-title",
                "page", String.valueOf(page + 1), "pages", String.valueOf(PAGES)),
                ItemBuilder.toLoreLines(lang.raw("help.page." + (page + 1)))));
        if (page > 0) {
            put(ANTERIOR, StructuresMenus.item(Material.ARROW, lang.component("menu.previous"),
                    List.of(lang.component("menu.previous-lore"))));
        }
        if (page < PAGES - 1) {
            put(SIGUIENTE, StructuresMenus.item(Material.ARROW, lang.component("menu.next"),
                    List.of(lang.component("menu.next-lore"))));
        }
        // Cambia entre volver y cerrar: por eso se ve (no va dibujado en el fondo).
        put(VOLVER, benchId == null
                ? StructuresMenus.item(Material.BARRIER, lang.component("menu.close"),
                        List.of(lang.component("menu.close-lore")))
                : StructuresMenus.item(Material.BARRIER, lang.component("menu.back"),
                        List.of(lang.component("menu.back-lore"))));
    }

    private void put(int slot, ItemStack item) {
        setItem(slot, StructuresMenus.AYUDA.dress(slot, item));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == ANTERIOR && page > 0) {
            new HelpMenu(player, plugin, benchId, tier, page - 1).open();
        } else if (slot == SIGUIENTE && page < PAGES - 1) {
            new HelpMenu(player, plugin, benchId, tier, page + 1).open();
        } else if (slot == VOLVER) {
            var bench = benchId == null ? java.util.Optional.<WorkbenchRecord>empty() : plugin.service().byId(benchId);
            if (bench.isPresent()) {
                new CenterMenu(player, plugin, bench.get()).open();
            } else {
                close();
            }
        }
    }
}
