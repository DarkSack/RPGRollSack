package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.deposit.MoneyService;
import com.sack.rpgroll.structures.preview.Footprint;
import com.sack.rpgroll.structures.preview.PreviewService;
import com.sack.rpgroll.structures.preview.PreviewSession;
import com.sack.rpgroll.structures.project.Project;
import com.sack.rpgroll.structures.project.ProjectService;
import com.sack.rpgroll.structures.storage.Box;
import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Mandos de la vista previa: girar, mover de 1 en 1, modo, confirmar y cancelar. Para mirar se cierra el menú;
 * clic derecho con la mano vacía lo reabre ({@link PreviewReopenListener}). 3 filas.
 */
public final class PreviewMenu extends InventoryGUI {
    public static final int INFO = 4;
    public static final int X_MENOS = 10;
    public static final int X_MAS = 11;
    public static final int Y_MENOS = 12;
    public static final int Y_MAS = 13;
    public static final int Z_MENOS = 14;
    public static final int Z_MAS = 15;
    public static final int GIRAR = 16;
    public static final int CANCELAR = 18;
    public static final int MODO = 22;
    public static final int CONFIRMAR = 26;

    private final StructuresPlugin plugin;
    private final UUID benchId;
    private final WorkbenchTier tier;

    public PreviewMenu(Player player, StructuresPlugin plugin, UUID benchId, WorkbenchTier tier) {
        super(player, StructuresMenus.title(plugin, StructuresMenus.VISTA, tier, "preview"), 27);
        this.plugin = plugin;
        this.benchId = benchId;
        this.tier = tier;
    }

    @Override
    public void build() {
        StructuresMenus.VISTA.fill(inventory);
        var lang = plugin.lang();
        Optional<PreviewSession> session = plugin.preview().session(player);
        if (session.isEmpty()) {
            put(INFO, StructuresMenus.item(Material.BARRIER, lang.component("menu.preview.none"), List.of()));
            return;
        }
        PreviewSession s = session.get();
        Location o = s.minCorner();
        put(INFO, StructuresMenus.icon(plugin, s.def().icon(),
                Component.text(s.def().name(plugin.settings().language())), List.of(
                        lang.component("menu.preview.rotation", "degrees", s.rotation().ordinal() * 90),
                        lang.component("menu.preview.position", "x", o.getBlockX(), "y", o.getBlockY(), "z",
                                o.getBlockZ()),
                        lang.component("menu.preview.hint"))));
        button(X_MENOS, Material.RED_DYE, "menu.preview.x-minus");
        button(X_MAS, Material.RED_DYE, "menu.preview.x-plus");
        button(Y_MENOS, Material.LIME_DYE, "menu.preview.y-minus");
        button(Y_MAS, Material.LIME_DYE, "menu.preview.y-plus");
        button(Z_MENOS, Material.LIGHT_BLUE_DYE, "menu.preview.z-minus");
        button(Z_MAS, Material.LIGHT_BLUE_DYE, "menu.preview.z-plus");
        button(GIRAR, Material.COMPASS, "menu.preview.rotate");
        put(MODO, StructuresMenus.item(Material.SPYGLASS, lang.component("menu.preview.mode"),
                List.of(lang.component(s.mode() == PreviewSession.Mode.BLOCKS
                        ? "menu.preview.mode-blocks" : "menu.preview.mode-outline"))));
        put(CANCELAR, StructuresMenus.item(Material.RED_CONCRETE, lang.component("menu.preview.cancel"),
                List.of(lang.component("menu.preview.cancel-lore"))));
        String why = DetailMenu.moneyReason(s.def(), plugin.money().available());
        put(CONFIRMAR, StructuresMenus.item(why == null ? Material.LIME_CONCRETE : Material.BARRIER,
                lang.component("menu.preview.confirm"),
                List.of(lang.component(why == null ? "menu.preview.confirm-lore" : why))));
    }

    private void button(int slot, Material icon, String key) {
        put(slot, StructuresMenus.item(icon, plugin.lang().component(key), List.of()));
    }

    private void put(int slot, ItemStack item) {
        setItem(slot, StructuresMenus.VISTA.dress(slot, item));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        PreviewService preview = plugin.preview();
        Optional<PreviewSession> session = preview.session(player);
        if (session.isEmpty()) {
            close();
            return;
        }
        switch (event.getRawSlot()) {
            case X_MENOS -> preview.move(player, -1, 0, 0);
            case X_MAS -> preview.move(player, 1, 0, 0);
            case Y_MENOS -> preview.move(player, 0, -1, 0);
            case Y_MAS -> preview.move(player, 0, 1, 0);
            case Z_MENOS -> preview.move(player, 0, 0, -1);
            case Z_MAS -> preview.move(player, 0, 0, 1);
            case GIRAR -> preview.rotate(player);
            case MODO -> preview.toggleMode(player);
            case CANCELAR -> {
                preview.stop(player);
                new DetailMenu(player, plugin, benchId, tier, session.get().def()).open();
                return;
            }
            case CONFIRMAR -> {
                StructuresMenus.withAccess(player, plugin, benchId, null, this::confirm);
                return;
            }
            default -> {
                return;
            }
        }
        build(); // en el sitio: posición y giro al día sin reabrir
    }

    /**
     * Clave del motivo por el que no se puede confirmar (nivel, permisos de construir, vista, categoría y Vault para
     * una estructura con dinero), o null.
     */
    static String confirmReason(Player p, StructureDefinition def, WorkbenchTier benchTier, boolean vault) {
        String why = ListMenu.lockReason(def.tier(), benchTier, ListMenu.permitted(p, def));
        if (why == null && !p.hasPermission(DetailMenu.PERM_PREVIEW)) {
            why = "catalog.no-preview-permission";
        }
        if (why == null && !CategoriesMenu.allowed(p, def.category())) {
            why = "catalog.locked-category";
        }
        return why != null ? why : DetailMenu.moneyReason(def, vault);
    }

    /**
     * Con acceso ya comprobado. Vuelve a mirar permisos y nivel, refresca las cajas de la base de datos (una obra
     * recién creada por otro no se cuela) y solo entonces hace una validación nueva y crea la obra.
     */
    private void confirm() {
        var lang = plugin.lang();
        Optional<PreviewSession> session = plugin.preview().session(player);
        Optional<WorkbenchRecord> bench = plugin.service().byId(benchId);
        if (session.isEmpty() || bench.isEmpty()) {
            plugin.preview().stop(player);
            close();
            return;
        }
        PreviewSession s = session.get();
        String why = confirmReason(player, s.def(), bench.get().tier(), plugin.money().available());
        if (why != null) {
            lang.send(player, why);
            return;
        }
        if (plugin.projects().hasActive(benchId)) {
            lang.send(player, "catalog.bench-busy");
            return;
        }
        plugin.preview().refresh(s.minCorner().getWorld().getName()).whenComplete((v, err) ->
                StructuresMenus.sync(plugin, () -> {
                    if (err != null) {
                        plugin.getLogger().log(Level.WARNING, "No se pudieron leer las cajas al confirmar", err);
                        lang.send(player, "catalog.create-error");
                    } else if (player.isOnline() && plugin.preview().session(player).orElse(null) == s) {
                        create(s); // la misma vista: no otra abierta mientras se leía
                    }
                }));
    }

    /** Validación completa (varios ticks en las estructuras grandes) y, si sigue la misma vista, la obra. */
    private void create(PreviewSession s) {
        var lang = plugin.lang();
        plugin.preview().fullCheck(player).whenComplete((check, err) -> StructuresMenus.sync(plugin, () -> {
            if (err != null) {
                plugin.getLogger().log(Level.WARNING, "Validación de la vista previa con error", err);
                lang.send(player, "catalog.create-error");
            } else if (player.isOnline() && plugin.preview().session(player).orElse(null) == s) {
                if (!check.result().ok()) {
                    lang.send(player, check.result().reason());
                } else if (!plugin.preview().unchanged(player, check)) {
                    lang.send(player, "preview.moved"); // se movió entre el fin de la validación y ahora
                } else {
                    createChecked(s, check);
                }
            }
        }));
    }

    /**
     * Crea la obra en el sitio y con el giro que se validaron (no los de ahora; {@code unchanged} ya miró que son los
     * mismos). Antes, el {@code precheck} del módulo de la estructura. El dinero se cobra aquí.
     */
    private void createChecked(PreviewSession s, PreviewService.Checked check) {
        var lang = plugin.lang();
        Location origin = check.origin();
        Footprint f = s.footprint();
        Optional<String> denied = plugin.modules().precheck(s.def(), player.getUniqueId(),
                origin.getWorld().getName(), new Box(f.minX(), f.minY(), f.minZ(), f.maxX(), f.maxY(), f.maxZ()));
        if (denied.isPresent()) {
            lang.send(player, denied.get());
            return;
        }
        CompletableFuture<Project> created;
        try {
            created = plugin.projects().create(benchId, s.def(), player, origin, check.rotation());
        } catch (ProjectService.ChargeException e) {
            switch (e.result()) {
                case NOT_ENOUGH -> lang.send(player, "structure.no-money", "amount",
                        MoneyService.format(MoneyService.cents(s.def().money())));
                case UNAVAILABLE -> lang.send(player, "structure.needs-vault");
                default -> lang.send(player, "structure.money-error");
            }
            return;
        } catch (ProjectService.OverlapException e) {
            lang.send(player, "preview.overlaps-project"); // otra confirmación ganó la zona; la vista sigue
            return;
        } catch (IllegalStateException e) {
            lang.send(player, "catalog.bench-busy"); // otra obra (o un segundo clic) se adelantó
            return;
        }
        created.whenComplete((project, err) -> StructuresMenus.sync(plugin, () -> {
            if (err != null) {
                lang.send(player, "catalog.create-error"); // ProjectService ya lo apuntó en el log
                return;
            }
            plugin.preview().stop(player);
            plugin.preview().invalidate(origin.getWorld().getName());
            lang.send(player, "catalog.created", "structure", s.def().name(plugin.settings().language()));
            if (project.moneyCents() > 0) {
                lang.send(player, "structure.charged", "amount", MoneyService.format(project.moneyCents()));
            }
            if (player.isOnline()) {
                MaterialsMenu.show(player, plugin, benchId, tier, inventory); // solo si sigue en la vista
            }
        }));
    }
}
