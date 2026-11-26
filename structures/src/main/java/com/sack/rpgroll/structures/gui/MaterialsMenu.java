package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.build.ConstructionEngine.StartResult;
import com.sack.rpgroll.structures.deposit.DepositResult;
import com.sack.rpgroll.structures.deposit.MoneyService;
import com.sack.rpgroll.structures.project.Project;
import com.sack.rpgroll.structures.project.ProjectState;
import com.sack.rpgroll.structures.storage.MaterialRow;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Materiales de la obra de la mesa: verde completo, amarillo a medias, rojo nada. Clic izquierdo deposita uno,
 * derecho todo lo de ese tipo; abajo depositar todo, recuperar, construir (si READY) y recoger pendientes. 6 filas.
 */
public final class MaterialsMenu extends InventoryGUI {
    public static final int INFO = 4;
    public static final List<Integer> CONTENT = StructuresMenus.grid(1, 4);
    public static final int DEPOSITAR = 45;
    public static final int RECUPERAR = 47;
    public static final int VOLVER = 49;
    public static final int CONSTRUIR = 51;
    public static final int RECOGER = 53;

    /** Cuánto hay depositado respecto a lo pedido. */
    public enum Fill {
        FULL, PARTIAL, NONE;

        public String key() {
            return "menu.materials." + name().toLowerCase(Locale.ROOT);
        }
    }

    public static Fill fill(int have, int need) {
        return have >= need ? Fill.FULL : have > 0 ? Fill.PARTIAL : Fill.NONE;
    }

    public static String depositKey(DepositResult r) {
        return switch (r) {
            case OK -> "deposit.done";
            case NOTHING_TO_TAKE -> "deposit.nothing";
            case BUSY -> "deposit.busy";
            case WRONG_STATE -> "deposit.wrong-state";
            case ERROR -> "deposit.error";
        };
    }

    public static String startKey(StartResult r) {
        return switch (r) {
            case STARTED -> "build.started";
            case NOT_READY -> "build.not-ready";
            case LIMIT_SERVER -> "build.limit-server";
            case LIMIT_WORLD -> "build.limit-world";
            case LIMIT_PLAYER -> "build.limit-player";
            case NO_PERMISSION -> "build.no-permission";
            case PROTECTED -> "build.protected";
            case MODULE_DENIED -> "build.module-denied";
        };
    }

    /**
     * Lee si hay pendientes (base de datos) y abre el menú en el hilo principal, solo si el jugador sigue con
     * {@code from} abierto (el menú desde el que se pidió; null = siempre).
     */
    public static void show(Player player, StructuresPlugin plugin, UUID benchId, WorkbenchTier tier,
                            Inventory from) {
        plugin.projectRepository().pending(player.getUniqueId()).whenComplete((rows, err) ->
                StructuresMenus.sync(plugin, () -> {
                    if (err != null) {
                        plugin.getLogger().log(Level.WARNING, "No se pudieron leer los pendientes de "
                                + player.getName(), err);
                    }
                    if (player.isOnline() && StructuresMenus.isOpen(player, from)) {
                        new MaterialsMenu(player, plugin, benchId, tier, err == null && !rows.isEmpty()).open();
                    }
                }));
    }

    private final StructuresPlugin plugin;
    private final UUID benchId;
    private final WorkbenchTier tier;
    private final boolean hasPending;

    public MaterialsMenu(Player player, StructuresPlugin plugin, UUID benchId, WorkbenchTier tier,
                         boolean hasPending) {
        super(player, StructuresMenus.title(plugin, StructuresMenus.MATERIALES, tier, "materials"), 54);
        this.plugin = plugin;
        this.benchId = benchId;
        this.tier = tier;
        this.hasPending = hasPending;
    }

    private Optional<Project> project() {
        return plugin.projects().forBench(benchId);
    }

    @Override
    public void build() {
        StructuresMenus.MATERIALES.fill(inventory);
        var lang = plugin.lang();
        Optional<Project> project = project();
        if (project.isEmpty()) {
            put(INFO, StructuresMenus.item(Material.BARRIER, lang.component("menu.materials.no-project"), List.of()));
        } else {
            Project p = project.get();
            List<Component> info = new ArrayList<>(List.of(ProjectMenu.stateLine(plugin, p.state())));
            if (p.moneyCents() > 0) {
                info.add(lang.component("menu.materials.money-paid", "amount",
                        MoneyService.format(p.moneyCents())));
            }
            put(INFO, StructuresMenus.item(Material.BARREL, Component.text(ProjectMenu.structureName(plugin, p)),
                    info));
            List<Map.Entry<String, MaterialRow>> rows = new ArrayList<>(p.materials().entrySet());
            boolean open = p.state() == ProjectState.WAITING_MATERIALS;
            for (int i = 0; i < Math.min(rows.size(), CONTENT.size()); i++) {
                String key = rows.get(i).getKey();
                MaterialRow row = rows.get(i).getValue();
                List<Component> lore = new ArrayList<>();
                lore.add(lang.component(fill(row.deposited(), row.required()).key(), "have", row.deposited(),
                        "need", row.required()));
                if (open && row.deposited() < row.required()) {
                    lore.add(lang.component("menu.materials.click"));
                }
                put(CONTENT.get(i), StructuresMenus.icon(plugin, key, StructuresMenus.materialName(plugin, key), lore));
            }
            if (p.state() == ProjectState.READY) {
                put(CONSTRUIR, StructuresMenus.item(Material.IRON_PICKAXE, lang.component("menu.materials.build"),
                        List.of(lang.component("menu.materials.build-lore"))));
            }
        }
        put(DEPOSITAR, StructuresMenus.item(Material.HOPPER, lang.component("menu.materials.deposit-all"),
                List.of(lang.component("menu.materials.deposit-all-lore"))));
        put(RECUPERAR, StructuresMenus.item(Material.CHEST, lang.component("menu.materials.withdraw"),
                List.of(lang.component("menu.materials.withdraw-lore"))));
        if (hasPending) {
            put(RECOGER, StructuresMenus.item(Material.ENDER_CHEST, lang.component("menu.materials.claim"),
                    List.of(lang.component("menu.materials.claim-lore"))));
        }
        put(VOLVER, StructuresMenus.item(Material.ARROW, lang.component("menu.back"),
                List.of(lang.component("menu.back-lore"))));
    }

    private void put(int slot, ItemStack item) {
        setItem(slot, StructuresMenus.MATERIALES.dress(slot, item));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == VOLVER) {
            StructuresMenus.back(player, plugin, benchId);
            return;
        }
        boolean right = event.isRightClick();
        boolean acts = slot == RECOGER && hasPending || slot == DEPOSITAR || slot == RECUPERAR || slot == CONSTRUIR
                || CONTENT.contains(slot);
        if (acts) {
            // El acceso se mira en cada clic: pudieron quitárselo con el menú abierto.
            StructuresMenus.withAccess(player, plugin, benchId, inventory, () -> act(slot, right));
        }
    }

    /** Hilo principal, con acceso comprobado; la obra se busca de nuevo (pudo cambiar mientras). */
    private void act(int slot, boolean right) {
        var lang = plugin.lang();
        if (slot == RECOGER) {
            plugin.returns().claimAll(player).whenComplete((n, err) -> StructuresMenus.sync(plugin, () -> {
                if (err == null && n > 0) {
                    lang.send(player, "deposit.claimed", "amount", n);
                } else {
                    lang.send(player, "deposit.nothing-pending");
                }
                reopen();
            }));
            return;
        }
        Optional<Project> found = project();
        if (found.isEmpty()) {
            lang.send(player, "menu.materials.no-project");
            return;
        }
        Project p = found.get();
        int i = CONTENT.indexOf(slot);
        if (i >= 0 && i < p.materials().size()) {
            String key = new ArrayList<>(p.materials().keySet()).get(i);
            deposit(p, key, right ? Integer.MAX_VALUE : 1);
        } else if (slot == DEPOSITAR) {
            deposit(p, null, Integer.MAX_VALUE);
        } else if (slot == RECUPERAR) {
            withdraw(p);
        } else if (slot == CONSTRUIR && p.state() == ProjectState.READY) {
            StartResult r = plugin.engine().start(player, p);
            lang.send(player, r == StartResult.MODULE_DENIED && plugin.engine().deniedKey() != null
                    ? plugin.engine().deniedKey() : startKey(r));
            reopen();
        }
    }

    private void deposit(Project p, String onlyKey, int max) {
        plugin.deposits().deposit(player, p, onlyKey, max).whenComplete((r, err) -> StructuresMenus.sync(plugin, () -> {
            DepositResult result = err != null ? DepositResult.ERROR : r;
            boolean ready = result == DepositResult.OK && p.state() == ProjectState.READY;
            plugin.lang().send(player, ready ? "deposit.ready" : depositKey(result));
            reopen();
        }));
    }

    /** Recupera todo lo depositado: dueño de la obra, de la mesa o bypass; lo recibe el dueño de la obra. */
    private void withdraw(Project p) {
        var lang = plugin.lang();
        if (!ProjectMenu.mayManage(plugin, player, p)) {
            lang.send(player, "build.owner-only");
        } else if (p.busy()) {
            lang.send(player, "deposit.busy");
        } else if (p.state() != ProjectState.WAITING_MATERIALS && p.state() != ProjectState.READY) {
            lang.send(player, "deposit.wrong-state");
        } else {
            plugin.returns().withdraw(player, p).whenComplete((ok, err) -> StructuresMenus.sync(plugin, () -> {
                lang.send(player, err == null && ok ? "deposit.withdrawn" : "deposit.error");
                reopen();
            }));
        }
    }

    /** Solo si sigue con ESTE menú abierto (no se le cambia el que esté mirando ahora). */
    private void reopen() {
        show(player, plugin, benchId, tier, inventory);
    }
}
