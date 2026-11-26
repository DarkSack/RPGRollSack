package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.build.BlockWriter;
import com.sack.rpgroll.structures.build.PlacementPlan;
import com.sack.rpgroll.structures.catalog.RotatedCache;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.deposit.MoneyService;
import com.sack.rpgroll.structures.project.Project;
import com.sack.rpgroll.structures.project.ProjectService;
import com.sack.rpgroll.structures.project.RepairMath;
import com.sack.rpgroll.structures.protect.AreaGuard;
import com.sack.rpgroll.structures.storage.Box;
import com.sack.rpgroll.structures.storage.BuildRecord;
import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import com.sack.rpgroll.structures.workbench.WorkbenchService;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Mis construcciones: primero las obras activas del jugador y luego las terminadas (estructura, mundo, coordenadas,
 * fecha), de 45 en 45. Clic en una terminada: repararla desde la mesa con la que se abrió el menú; clic derecho:
 * elegir a qué estructura mejorarla (las que declaran {@code upgrade_from:} su id), en este mismo menú. 6 filas.
 */
public final class BuildsMenu extends InventoryGUI {
    public static final int PAGE_SIZE = 45;
    public static final int ANTERIOR = 45;
    public static final int VOLVER = 49;
    public static final int SIGUIENTE = 53;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    /** Una fila: {@code active} para una obra en marcha, {@code done} para una terminada. */
    private record Entry(Project active, BuildRecord done) {
        String structure() {
            return active != null ? active.structure() : done.structure();
        }
    }

    /**
     * Lee las terminadas (base de datos) y abre el menú en el hilo principal, solo si el jugador sigue con
     * {@code from} abierto (null = siempre).
     */
    public static void show(Player player, StructuresPlugin plugin, UUID benchId, WorkbenchTier tier, int page,
                            Inventory from) {
        plugin.projectRepository().buildsOf(player.getUniqueId()).whenComplete((builds, err) ->
                StructuresMenus.sync(plugin, () -> {
                    if (err != null) {
                        plugin.getLogger().log(Level.WARNING, "No se pudieron leer las construcciones de "
                                + player.getName(), err);
                        plugin.lang().send(player, "menu.builds.error");
                        return;
                    }
                    if (player.isOnline() && StructuresMenus.isOpen(player, from)) {
                        new BuildsMenu(player, plugin, benchId, tier, page, builds).open();
                    }
                }));
    }

    private final StructuresPlugin plugin;
    private final UUID benchId;
    private final WorkbenchTier tier;
    private final List<Entry> all = new ArrayList<>();
    private final int page;
    /** Elegir mejora: la construcción y sus mejoras posibles (null en la lista normal). */
    private final BuildRecord upgrading;
    private final List<StructureDefinition> targets;

    private BuildsMenu(Player player, StructuresPlugin plugin, UUID benchId, WorkbenchTier tier, int page,
                       List<BuildRecord> builds) {
        super(player, StructuresMenus.title(plugin, StructuresMenus.CONSTRUCCIONES, tier, "builds"), 54);
        this.plugin = plugin;
        this.benchId = benchId;
        this.tier = tier;
        plugin.projects().ofPlayer(player.getUniqueId()).forEach(p -> all.add(new Entry(p, null)));
        builds.forEach(b -> all.add(new Entry(null, b)));
        this.page = Math.max(0, Math.min(page, StructuresMenus.pageCount(all.size(), PAGE_SIZE) - 1));
        this.upgrading = null;
        this.targets = List.of();
    }

    /** Elegir a qué mejorar {@code b}; {@code page} es la de la lista a la que se vuelve. */
    private BuildsMenu(Player player, StructuresPlugin plugin, UUID benchId, WorkbenchTier tier, int page,
                       BuildRecord b, List<StructureDefinition> targets) {
        super(player, StructuresMenus.title(plugin, StructuresMenus.CONSTRUCCIONES, tier, "builds"), 54);
        this.plugin = plugin;
        this.benchId = benchId;
        this.tier = tier;
        this.page = page;
        this.upgrading = b;
        // shortcut: más de 45 mejoras de una misma estructura no caben; sin paginar hasta que haga falta.
        this.targets = targets.subList(0, Math.min(PAGE_SIZE, targets.size()));
    }

    @Override
    public void build() {
        StructuresMenus.fill(inventory);
        var lang = plugin.lang();
        if (upgrading != null) {
            for (int i = 0; i < targets.size(); i++) {
                StructureDefinition d = targets.get(i);
                put(i, StructuresMenus.icon(plugin, d.icon(), Component.text(d.name(plugin.settings().language())),
                        List.of(lang.component("menu.builds.upgrade-to"))));
            }
            put(VOLVER, StructuresMenus.item(Material.BARRIER, lang.component("menu.back"),
                    List.of(lang.component("menu.back-lore"))));
            return;
        }
        List<Entry> shown = StructuresMenus.pageOf(all, page, PAGE_SIZE);
        for (int i = 0; i < shown.size(); i++) {
            put(i, entry(shown.get(i)));
        }
        if (all.isEmpty()) {
            put(22, StructuresMenus.item(Material.PAPER, lang.component("menu.builds.empty"), List.of()));
        }
        if (page > 0) {
            put(ANTERIOR, StructuresMenus.item(Material.ARROW, lang.component("menu.previous"),
                    List.of(lang.component("menu.previous-lore"))));
        }
        if (page < StructuresMenus.pageCount(all.size(), PAGE_SIZE) - 1) {
            put(SIGUIENTE, StructuresMenus.item(Material.ARROW, lang.component("menu.next"),
                    List.of(lang.component("menu.next-lore"))));
        }
        put(VOLVER, StructuresMenus.backOrClose(plugin, benchId));
    }

    private ItemStack entry(Entry e) {
        var lang = plugin.lang();
        String icon = plugin.structures().get(e.structure()).map(StructureDefinition::icon).orElse(null);
        String name = plugin.structures().get(e.structure()).map(d -> d.name(plugin.settings().language()))
                .orElse(e.structure());
        List<Component> lore = new ArrayList<>();
        if (e.active() != null) {
            Project p = e.active();
            lore.add(ProjectMenu.stateLine(plugin, p.state()));
            lore.add(lang.component("menu.project.progress", "percent", StructuresMenus.percent(p.cursor(), p.total())));
            lore.add(lang.component("menu.project.where", "world", p.world(), "x", p.x(), "y", p.y(), "z", p.z()));
            lore.add(lang.component("menu.builds.started", "date", DATE.format(Instant.ofEpochMilli(p.createdAt()))));
        } else {
            BuildRecord b = e.done();
            lore.add(lang.component("menu.project.where", "world", b.world(), "x", b.x(), "y", b.y(), "z", b.z()));
            lore.add(lang.component("menu.builds.date", "date", DATE.format(Instant.ofEpochMilli(b.completedAt()))));
            lore.add(lang.component("menu.builds.repair"));
            if (!plugin.structures().upgradesOf(b.structure()).isEmpty()) {
                lore.add(lang.component("menu.builds.upgrade"));
            }
        }
        return StructuresMenus.icon(plugin, icon, Component.text(name), lore);
    }

    private void put(int slot, ItemStack item) {
        setItem(slot, StructuresMenus.CONSTRUCCIONES.dress(slot, item));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (upgrading != null) {
            if (slot >= 0 && slot < targets.size()) {
                upgrade(upgrading, targets.get(slot));
            } else if (slot == VOLVER) {
                show(player, plugin, benchId, tier, page, inventory);
            }
            return;
        }
        List<Entry> shown = StructuresMenus.pageOf(all, page, PAGE_SIZE);
        if (slot >= 0 && slot < shown.size()) {
            BuildRecord done = shown.get(slot).done();
            if (done != null && event.isRightClick()) {
                List<StructureDefinition> up = plugin.structures().upgradesOf(done.structure());
                if (up.isEmpty()) {
                    plugin.lang().send(player, "build-upgrade.none");
                } else {
                    new BuildsMenu(player, plugin, benchId, tier, page, done, up).open();
                }
            } else if (done != null) {
                repair(done);
            }
        } else if (slot == ANTERIOR && page > 0) {
            show(player, plugin, benchId, tier, page - 1, inventory);
        } else if (slot == SIGUIENTE && page < StructuresMenus.pageCount(all.size(), PAGE_SIZE) - 1) {
            show(player, plugin, benchId, tier, page + 1, inventory);
        } else if (slot == VOLVER) {
            StructuresMenus.back(player, plugin, benchId);
        }
    }

    /**
     * Hilo principal. Reparar desde la mesa con la que se abrió el menú: conocida, del dueño, libre y de nivel
     * suficiente. Con todos los chunks de la caja cargados, cuenta lo que falta en varios ticks ({@code RepairScans},
     * una lectura a la vez por jugador); si hay algo que reparar y no falta todo, pide confirmación con lo que falta y
     * el dinero.
     */
    private void repair(BuildRecord b) {
        var lang = plugin.lang();
        Optional<WorkbenchRecord> bench = benchId == null ? Optional.empty() : plugin.service().byId(benchId);
        if (bench.isEmpty() || bench.get().orphan() || !bench.get().owner().equals(player.getUniqueId())) {
            lang.send(player, "repair.needs-bench");
            return;
        }
        if (plugin.projects().hasActive(benchId)) {
            lang.send(player, "catalog.bench-busy");
            return;
        }
        Optional<StructureDefinition> def = plugin.structures().get(b.structure());
        if (def.isEmpty()) {
            lang.send(player, "repair.missing-structure");
            return;
        }
        String why = ListMenu.lockReason(def.get().tier(), bench.get().tier(), ListMenu.permitted(player, def.get()));
        if (why == null) {
            why = DetailMenu.moneyReason(def.get(), plugin.money().available());
        }
        if (why == null && !RepairMath.matches(def.get(), b)) {
            why = ProjectService.RepairException.CHANGED;
        }
        BlockWriter writer = BlockWriter.bukkit();
        if (why == null && !RepairMath.loaded(writer, b.world(), b.box())) {
            why = "repair.not-loaded";
        }
        if (why != null) {
            lang.send(player, why);
            return;
        }
        Set<Material> replace = def.get().placement() == null ? Set.of() : def.get().placement().replace();
        Optional<CompletableFuture<RepairMath.Result>> scan = plugin.repairScans().start(player.getUniqueId(),
                new PlacementPlan(RotatedCache.get(def.get(), b.rotation())), replace, b.world(), b.x(), b.y(), b.z());
        if (scan.isEmpty()) {
            lang.send(player, "repair.scanning"); // un clic más mientras se lee: no se lee otra vez
            return;
        }
        // RepairScans termina en su tick, en el hilo principal.
        scan.get().whenComplete((r, err) -> {
            if (!player.isOnline()) {
                return;
            }
            if (err != null) {
                if (err instanceof ProjectService.RepairException e) {
                    lang.send(player, e.key());
                } else {
                    plugin.getLogger().log(Level.WARNING, "No se pudo revisar la construcción " + b.id(), err);
                    lang.send(player, "catalog.create-error");
                }
            } else if (r.missing() <= 0 || r.missing() >= r.total()) {
                doRepair(def.get(), b, r); // nada que reparar o desaparecida: el motivo, sin confirmar
            } else if (StructuresMenus.isOpen(player, inventory)) {
                confirm(def.get(), b, r); // solo si sigue en este menú
            }
        });
    }

    /** Confirmación con lo que falta y el dinero; al aceptar, {@link #doRepair}. */
    private void confirm(StructureDefinition def, BuildRecord b, RepairMath.Result r) {
        var lang = plugin.lang();
        long cents = RepairMath.money(MoneyService.cents(def.money()), r.missing(), r.total());
        List<Component> lore = new ArrayList<>();
        lore.add(lang.component("repair.confirm-lore", "missing", r.missing(), "total", r.total()));
        if (cents > 0) {
            lore.add(lang.component("repair.confirm-money", "amount", MoneyService.format(cents)));
        }
        new ConfirmMenu(player, plugin, tier, StructuresMenus.item(Material.ANVIL, lang.component("repair.confirm",
                "structure", def.name(plugin.settings().language())), lore), () -> doRepair(def, b, r),
                () -> show(player, plugin, benchId, tier, page, null)).open();
    }

    /** Crea la obra de reparación (o da el motivo) y, si se crea, abre sus materiales. */
    private void doRepair(StructureDefinition def, BuildRecord b, RepairMath.Result r) {
        var lang = plugin.lang();
        CompletableFuture<Project> created;
        try {
            created = plugin.projects().repair(benchId, def, b, r, player, plugin.modules()::remove);
        } catch (ProjectService.RepairException e) {
            lang.send(player, e.key());
            return;
        } catch (ProjectService.ChargeException e) {
            switch (e.result()) {
                case NOT_ENOUGH -> lang.send(player, "structure.no-money", "amount", MoneyService.format(
                        RepairMath.money(MoneyService.cents(def.money()), r.missing(), r.total())));
                case UNAVAILABLE -> lang.send(player, "structure.needs-vault");
                default -> lang.send(player, "structure.money-error");
            }
            return;
        } catch (ProjectService.OverlapException e) {
            lang.send(player, "repair.overlaps"); // otra obra (o reparación) ocupa la zona
            return;
        } catch (IllegalStateException e) {
            lang.send(player, "catalog.bench-busy");
            return;
        }
        Inventory open = player.getOpenInventory().getTopInventory();
        created.whenComplete((project, err) -> StructuresMenus.sync(plugin, () -> {
            if (err != null) {
                lang.send(player, "catalog.create-error"); // ProjectService ya lo apuntó en el log
                return;
            }
            lang.send(player, "repair.created", "structure", def.name(plugin.settings().language()), "missing",
                    r.missing());
            if (project.moneyCents() > 0) {
                lang.send(player, "structure.charged", "amount", MoneyService.format(project.moneyCents()));
            }
            if (player.isOnline()) {
                MaterialsMenu.show(player, plugin, benchId, tier, open); // solo si sigue en el mismo menú
            }
        }));
    }

    /** La mesa del menú, si es conocida, no huérfana y del jugador. */
    private Optional<WorkbenchRecord> ownBench() {
        Optional<WorkbenchRecord> bench = benchId == null ? Optional.empty() : plugin.service().byId(benchId);
        return bench.filter(w -> !w.orphan() && w.owner().equals(player.getUniqueId()));
    }

    /**
     * Hilo principal. Mejorar {@code b} a {@code target} desde la mesa del menú (libre y de nivel suficiente): la
     * estructura original tiene que seguir en el catálogo tal como se construyó (si no, no se sabe qué bloques son
     * suyos), y el módulo de la nueva tiene que dejar construir en la caja nueva. Pide confirmación con el precio.
     */
    private void upgrade(BuildRecord b, StructureDefinition target) {
        var lang = plugin.lang();
        Optional<WorkbenchRecord> bench = ownBench();
        if (bench.isEmpty()) {
            lang.send(player, "build-upgrade.needs-bench");
            return;
        }
        if (plugin.projects().hasActive(benchId)) {
            lang.send(player, "catalog.bench-busy");
            return;
        }
        Optional<StructureDefinition> old = plugin.structures().get(b.structure());
        String why = old.isEmpty() || !RepairMath.matches(old.get(), b) ? "build-upgrade.changed" : null;
        if (why == null) {
            why = ListMenu.lockReason(target.tier(), bench.get().tier(), ListMenu.permitted(player, target));
        }
        if (why == null) {
            why = DetailMenu.moneyReason(target, plugin.money().available());
        }
        if (why == null) {
            // La caja nueva no pasó por la vista previa: altura del mundo, protección y módulo, antes de cobrar.
            int[] shift = ProjectService.upgradeShift(b.rotation(), old.get().blocks().width(),
                    old.get().blocks().length(), target.blocks().width(), target.blocks().length());
            int w = b.rotation().rotatedWidth(target.blocks().width(), target.blocks().length());
            int l = b.rotation().rotatedLength(target.blocks().width(), target.blocks().length());
            Box box = new Box(b.x() + shift[0], b.y(), b.z() + shift[1], b.x() + shift[0] + w - 1,
                    b.y() + target.blocks().height() - 1, b.z() + shift[1] + l - 1);
            World world = Bukkit.getWorld(b.world());
            if (world == null || box.minY() < world.getMinHeight() || box.maxY() >= world.getMaxHeight()) {
                why = "preview.out-of-world";
            } else if (!player.hasPermission(WorkbenchService.PERM_BYPASS)) {
                why = plugin.guard().check(b.owner(), b.world(), box).map(AreaGuard.Denial::langKey).orElse(null);
            }
            if (why == null) {
                why = plugin.modules().precheck(target, b.owner(), b.world(), box).orElse(null);
            }
        }
        if (why != null) {
            lang.send(player, why);
            return;
        }
        String name = target.name(plugin.settings().language());
        long cents = MoneyService.cents(target.money());
        List<Component> lore = new ArrayList<>();
        lore.add(lang.component("build-upgrade.confirm-lore"));
        if (cents > 0) {
            lore.add(lang.component("repair.confirm-money", "amount", MoneyService.format(cents)));
        }
        new ConfirmMenu(player, plugin, tier, StructuresMenus.item(Material.SMITHING_TABLE,
                lang.component("build-upgrade.confirm", "structure", name), lore), () -> doUpgrade(b, target, old.get()),
                () -> show(player, plugin, benchId, tier, page, null)).open();
    }

    /** Crea la obra de mejora (o da el motivo) y, si se crea, abre sus materiales. */
    private void doUpgrade(BuildRecord b, StructureDefinition target, StructureDefinition original) {
        var lang = plugin.lang();
        String name = target.name(plugin.settings().language());
        CompletableFuture<Project> created;
        try {
            created = plugin.projects().createUpgrade(benchId, target, b, original, player);
        } catch (ProjectService.ChargeException e) {
            switch (e.result()) {
                case NOT_ENOUGH -> lang.send(player, "structure.no-money", "amount",
                        MoneyService.format(MoneyService.cents(target.money())));
                case UNAVAILABLE -> lang.send(player, "structure.needs-vault");
                default -> lang.send(player, "structure.money-error");
            }
            return;
        } catch (ProjectService.OverlapException e) {
            lang.send(player, "build-upgrade.overlaps");
            return;
        } catch (IllegalStateException e) {
            lang.send(player, "catalog.bench-busy");
            return;
        }
        Inventory open = player.getOpenInventory().getTopInventory();
        created.whenComplete((project, err) -> StructuresMenus.sync(plugin, () -> {
            if (err != null) {
                // El choque con otra construcción terminada llega aquí (se mira en la base de datos).
                Throwable root = err instanceof CompletionException && err.getCause() != null ? err.getCause() : err;
                lang.send(player, root instanceof ProjectService.OverlapException ? "build-upgrade.overlaps"
                        : root instanceof ProjectService.RepairException e ? e.key() : "catalog.create-error");
                return;
            }
            lang.send(player, "build-upgrade.created", "structure", name);
            if (project.moneyCents() > 0) {
                lang.send(player, "structure.charged", "amount", MoneyService.format(project.moneyCents()));
            }
            if (player.isOnline()) {
                MaterialsMenu.show(player, plugin, benchId, tier, open); // solo si sigue en el mismo menú
            }
        }));
    }
}
