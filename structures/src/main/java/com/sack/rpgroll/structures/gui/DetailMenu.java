package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.catalog.MaterialSpec;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Ficha de una estructura: medidas, nivel, materiales, dinero, tiempo estimado y permiso. «Vista previa» y «Elegir»
 * abren la vista previa; sin mesa (comando) no hay ninguno de los dos. 6 filas.
 */
public final class DetailMenu extends InventoryGUI {
    public static final int ICONO = 4;
    public static final List<Integer> MATERIALES = StructuresMenus.grid(2, 2);
    public static final int VISTA_PREVIA = 47;
    public static final int VOLVER = 49;
    public static final int ELEGIR = 51;

    public static final String PERM_PREVIEW = "rpgrollstructures.preview";

    private final StructuresPlugin plugin;
    private final UUID benchId;
    private final WorkbenchTier tier;
    private final StructureDefinition def;

    public DetailMenu(Player player, StructuresPlugin plugin, UUID benchId, WorkbenchTier tier,
                      StructureDefinition def) {
        super(player, StructuresMenus.title(plugin, StructuresMenus.FICHA, tier, "detail"), 54);
        this.plugin = plugin;
        this.benchId = benchId;
        this.tier = tier;
        this.def = def;
    }

    @Override
    public void build() {
        StructuresMenus.fill(inventory);
        var lang = plugin.lang();
        List<Component> lore = new ArrayList<>();
        lore.add(lang.component("menu.detail.size", "width", def.blocks().width(), "height", def.blocks().height(),
                "length", def.blocks().length()));
        lore.add(lang.component("menu.detail.tier", "tier", StructuresMenus.tierName(plugin, def.tier())));
        lore.add(lang.component("menu.detail.blocks", "amount", def.volume()));
        if (def.money() > 0) {
            lore.add(lang.component("menu.detail.money", "amount", String.format(Locale.ROOT, "%.2f", def.money())));
        }
        lore.add(lang.component("menu.detail.time", "time", StructuresMenus.duration(
                StructuresMenus.estimateSeconds(def.volume(), def.blocksPerCycle(), def.ticksPerCycle()))));
        lore.add(lang.component("menu.detail.permission", "permission", def.permission()));
        String note = def.moduleNote(plugin.settings().language());
        if (note != null) {
            lore.add(lang.component("menu.detail.module-note", "note", note));
        }
        put(ICONO, StructuresMenus.icon(plugin, def.icon(), Component.text(def.name(plugin.settings().language())),
                lore));

        List<MaterialSpec> specs = def.materials();
        for (int i = 0; i < Math.min(specs.size(), MATERIALES.size()); i++) {
            MaterialSpec spec = specs.get(i);
            List<Component> ml = new ArrayList<>();
            ml.add(lang.component("menu.detail.material", "amount", spec.amount()));
            if (spec.anyOf().size() > 1) {
                ml.add(lang.component("menu.detail.any-of", "amount", spec.anyOf().size()));
            }
            if (spec.optional()) {
                ml.add(lang.component("menu.detail.optional"));
            }
            put(MATERIALES.get(i), StructuresMenus.icon(plugin, spec.key(),
                    StructuresMenus.materialName(plugin, spec.key()), ml));
        }

        if (benchId != null) {
            put(VISTA_PREVIA, StructuresMenus.item(Material.ENDER_EYE, lang.component("menu.detail.preview"),
                    List.of(lang.component(player.hasPermission(PERM_PREVIEW)
                            ? "menu.detail.preview-lore" : "catalog.no-preview-permission"))));
            String why = reason();
            put(ELEGIR, why == null
                    ? StructuresMenus.item(Material.LIME_CONCRETE, lang.component("menu.detail.choose"),
                            List.of(lang.component("menu.detail.choose-lore")))
                    : StructuresMenus.item(Material.BARRIER, lang.component("menu.detail.choose"),
                            List.of(lang.component(why))));
        }
        put(VOLVER, StructuresMenus.item(Material.ARROW, lang.component("menu.back"),
                List.of(lang.component("menu.back-lore"))));
    }

    private String reason() {
        return reason(player, def, tier, plugin.money().available());
    }

    /** Nivel y permisos; luego, una estructura con dinero sin Vault activo ({@code structure.needs-vault}). */
    static String reason(Player p, StructureDefinition def, WorkbenchTier benchTier, boolean vault) {
        String why = ListMenu.lockReason(def.tier(), benchTier, ListMenu.permitted(p, def));
        return why != null ? why : moneyReason(def, vault);
    }

    /** {@code structure.needs-vault} si la estructura cobra dinero y no hay Vault activo; si no, null. */
    static String moneyReason(StructureDefinition def, boolean vault) {
        return def.money() > 0 && !vault ? "structure.needs-vault" : null;
    }

    private void put(int slot, ItemStack item) {
        setItem(slot, StructuresMenus.FICHA.dress(slot, item));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == VOLVER) {
            new ListMenu(player, plugin, benchId, tier, def.category(), 0).open();
        } else if (benchId != null && (slot == VISTA_PREVIA || slot == ELEGIR)) {
            boolean choose = slot == ELEGIR;
            StructuresMenus.withAccess(player, plugin, benchId, inventory, () -> startPreview(choose));
        }
    }

    /**
     * Con acceso ya comprobado (withAccess). «Vista previa» deja mirar una estructura con dinero sin Vault (el
     * botón de confirmar dice el motivo); «Elegir» no.
     */
    private void startPreview(boolean choose) {
        var lang = plugin.lang();
        String why = choose ? reason() : ListMenu.lockReason(def.tier(), tier, ListMenu.permitted(player, def));
        if (why != null) {
            lang.send(player, why);
        } else if (!player.hasPermission(PERM_PREVIEW)) {
            lang.send(player, "catalog.no-preview-permission");
        } else if (plugin.projects().hasActive(benchId)) {
            lang.send(player, "catalog.bench-busy");
        } else {
            plugin.preview().start(player, benchId, def);
            new PreviewMenu(player, plugin, benchId, tier).open();
        }
    }
}
