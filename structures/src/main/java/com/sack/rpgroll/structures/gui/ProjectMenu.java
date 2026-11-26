package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.build.ConstructionEngine;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.project.Project;
import com.sack.rpgroll.structures.project.ProjectState;
import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import com.sack.rpgroll.structures.workbench.WorkbenchService;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/** La obra de la mesa: progreso, saltados, tiempo restante, estado, pausar/reanudar y cancelar. 4 filas. */
public final class ProjectMenu extends InventoryGUI {
    public static final int INFO = 4;
    public static final int PROGRESO = 13;
    public static final int PAUSA = 20;
    public static final int MATERIALES = 22;
    public static final int CANCELAR = 24;
    public static final int VOLVER = 31;

    /** Nombre de la estructura de una obra en el idioma del servidor; el id si ya no está en el catálogo. */
    static String structureName(StructuresPlugin plugin, Project p) {
        return plugin.structures().get(p.structure()).map(d -> d.name(plugin.settings().language()))
                .orElse(p.structure());
    }

    static Component stateLine(StructuresPlugin plugin, ProjectState state) {
        return plugin.lang().component("menu.state.line", "state",
                plugin.lang().raw("menu.state." + state.name().toLowerCase(Locale.ROOT)));
    }

    /**
     * Recuperar y cancelar: el dueño de la obra, el de la mesa o bypass ({@code benchOwner} null = mesa desconocida).
     * Lo devuelto va siempre al dueño de la obra (ReturnService).
     */
    static boolean mayManage(UUID clicker, UUID projectOwner, UUID benchOwner, boolean bypass) {
        return bypass || clicker.equals(projectOwner) || clicker.equals(benchOwner);
    }

    static boolean mayManage(StructuresPlugin plugin, Player player, Project p) {
        return mayManage(player.getUniqueId(), p.owner(),
                plugin.service().byId(p.bench()).map(WorkbenchRecord::owner).orElse(null),
                player.hasPermission(WorkbenchService.PERM_BYPASS));
    }

    /**
     * Pide confirmación (con el % que se devuelve) y cancela la obra; lo devuelto va al dueño de la obra.
     * {@code accessBench}: mesa cuyo acceso se vuelve a mirar al confirmar (null desde el comando: ahí solo llega el
     * dueño de la obra, que siempre puede recuperar lo suyo). {@code back} corre después, en el hilo principal, si
     * sigue con la confirmación abierta; también si se dice que no.
     */
    public static void confirmCancel(Player player, StructuresPlugin plugin, Project project, WorkbenchTier tier,
                                     UUID accessBench, Runnable back) {
        var lang = plugin.lang();
        if (!mayManage(plugin, player, project)) {
            lang.send(player, "build.owner-only");
            return;
        }
        int percent = plugin.performance().cancelRefundPercent();
        ItemStack info = StructuresMenus.item(Material.TNT, lang.component("build.cancel-confirm",
                "structure", structureName(plugin, project)), List.of(lang.component("build.cancel-confirm-lore",
                "percent", percent)));
        ConfirmMenu[] menu = new ConfirmMenu[1];
        Runnable doCancel = () -> {
            if (project.busy()) {
                lang.send(player, "build.busy");
                back.run();
                return;
            }
            if (project.state() == ProjectState.COMPLETED || project.state() == ProjectState.CANCELLED) {
                lang.send(player, "build.wrong-state");
                back.run();
                return;
            }
            plugin.returns().cancel(player, project, percent).whenComplete((ok, err) ->
                    StructuresMenus.sync(plugin, () -> {
                        if (err != null) {
                            plugin.getLogger().log(Level.WARNING, "Cancelación fallida de " + project.id(), err);
                        }
                        boolean done = err == null && ok;
                        if (done) {
                            plugin.preview().invalidate(project.world()); // su caja ya no choca
                        }
                        lang.send(player, done ? "build.cancelled" : "build.cancel-error");
                        if (player.isOnline() && StructuresMenus.isOpen(player, menu[0].getInventory())) {
                            back.run();
                        }
                    }));
        };
        menu[0] = new ConfirmMenu(player, plugin, tier, info, accessBench == null ? doCancel
                : () -> StructuresMenus.withAccess(player, plugin, accessBench, menu[0].getInventory(), doCancel),
                back);
        menu[0].open();
    }

    private final StructuresPlugin plugin;
    private final UUID benchId;
    private final WorkbenchTier tier;

    public ProjectMenu(Player player, StructuresPlugin plugin, WorkbenchRecord bench) {
        super(player, StructuresMenus.title(plugin, StructuresMenus.OBRA, bench.tier(), "project"), 36);
        this.plugin = plugin;
        this.benchId = bench.id();
        this.tier = bench.tier();
    }

    private Optional<Project> project() {
        return plugin.projects().forBench(benchId);
    }

    @Override
    public void build() {
        StructuresMenus.OBRA.fill(inventory);
        var lang = plugin.lang();
        Optional<Project> found = project();
        if (found.isEmpty()) {
            put(INFO, StructuresMenus.item(Material.BARRIER, lang.component("menu.project.none"),
                    List.of(lang.component("menu.project.none-lore"))));
        } else {
            Project p = found.get();
            Optional<StructureDefinition> def = plugin.structures().get(p.structure());
            put(INFO, StructuresMenus.icon(plugin, def.map(StructureDefinition::icon).orElse(null),
                    Component.text(structureName(plugin, p)), List.of(stateLine(plugin, p.state()),
                            lang.component("menu.project.where", "world", p.world(), "x", p.x(), "y", p.y(),
                                    "z", p.z()))));
            List<Component> lore = new ArrayList<>();
            lore.add(lang.component("menu.project.blocks", "placed", p.placed(), "total", p.total()));
            lore.add(lang.component("menu.project.skipped", "amount", p.skipped()));
            def.ifPresent(d -> lore.add(lang.component("menu.project.remaining", "time", StructuresMenus.duration(
                    StructuresMenus.estimateSeconds(p.total() - p.cursor(), d.blocksPerCycle(), d.ticksPerCycle())))));
            if (p.state() == ProjectState.PAUSED && p.error() != null) {
                lore.add(lang.component(p.error())); // motivo de la pausa: clave de idioma
            } else if (p.error() != null) {
                lore.add(lang.component("menu.project.error", "error", p.error()));
            }
            put(PROGRESO, StructuresMenus.item(Material.EXPERIENCE_BOTTLE, lang.component("menu.project.progress",
                    "percent", StructuresMenus.percent(p.cursor(), p.total())), lore));
            if (p.state() == ProjectState.BUILDING) {
                put(PAUSA, StructuresMenus.item(Material.CLOCK, lang.component("menu.project.pause"),
                        List.of(lang.component("menu.project.pause-lore"))));
            } else if (p.state() == ProjectState.PAUSED) {
                put(PAUSA, StructuresMenus.item(Material.LIME_DYE, lang.component("menu.project.resume"),
                        List.of(lang.component("menu.project.resume-lore"))));
            }
        }
        put(MATERIALES, StructuresMenus.item(Material.BARREL, lang.component("menu.center.materials"),
                List.of(lang.component("menu.center.materials-lore"))));
        put(CANCELAR, StructuresMenus.item(Material.TNT, lang.component("menu.project.cancel"),
                List.of(lang.component("menu.project.cancel-lore"))));
        put(VOLVER, StructuresMenus.item(Material.ARROW, lang.component("menu.back"),
                List.of(lang.component("menu.back-lore"))));
    }

    private void put(int slot, ItemStack item) {
        setItem(slot, StructuresMenus.OBRA.dress(slot, item));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == VOLVER) {
            StructuresMenus.back(player, plugin, benchId);
            return;
        }
        if (slot == MATERIALES) {
            MaterialsMenu.show(player, plugin, benchId, tier, inventory);
            return;
        }
        if (slot == PAUSA || slot == CANCELAR) {
            // El acceso se mira en cada clic: pudieron quitárselo con el menú abierto.
            StructuresMenus.withAccess(player, plugin, benchId, inventory, () -> act(slot));
        }
    }

    /** Hilo principal, con acceso comprobado; la obra se busca de nuevo. */
    private void act(int slot) {
        var lang = plugin.lang();
        Optional<Project> found = project();
        if (found.isEmpty()) {
            lang.send(player, "menu.project.none");
            return;
        }
        Project p = found.get();
        if (slot == PAUSA) {
            if (p.busy()) {
                lang.send(player, "build.busy");
            } else if (p.state() == ProjectState.BUILDING) {
                plugin.engine().pause(p);
                lang.send(player, "build.paused");
            } else if (p.state() == ProjectState.PAUSED) {
                ConstructionEngine.StartResult r = plugin.engine().resume(p,
                        player.hasPermission(WorkbenchService.PERM_BYPASS));
                lang.send(player, switch (r) {
                    case STARTED -> "build.resumed";
                    case PROTECTED -> "build.protected";
                    default -> "build.wrong-state";
                });
            }
            build();
        } else if (slot == CANCELAR) {
            confirmCancel(player, plugin, p, tier, benchId, () -> plugin.service().byId(benchId)
                    .ifPresentOrElse(b -> new ProjectMenu(player, plugin, b).open(), player::closeInventory));
        }
    }
}
