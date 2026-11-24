package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** Cabezas de los jugadores de confianza, de 45 en 45. Clic para quitar; «Añadir» pide el nombre por chat. */
public final class AccessMenu extends InventoryGUI {
    public static final int PAGE_SIZE = 45;
    public static final int ANTERIOR = 45;
    public static final int VOLVER = 48;
    public static final int ANADIR = 50;
    public static final int SIGUIENTE = 53;

    /** Cuántas páginas hacen falta (mínimo 1, aunque no haya nadie). */
    public static int pageCount(int total) {
        return Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    /** Los elementos de una página (la página se acota a lo que existe). */
    public static <T> List<T> pageOf(List<T> all, int page) {
        int p = Math.max(0, Math.min(page, pageCount(all.size()) - 1));
        return all.subList(Math.min(all.size(), p * PAGE_SIZE), Math.min(all.size(), (p + 1) * PAGE_SIZE));
    }

    /** Lee los de confianza (base de datos) y abre el menú en el hilo principal. */
    public static void show(Player player, StructuresPlugin plugin, WorkbenchRecord bench, int page) {
        plugin.service().trusted(bench.id()).whenComplete((trusted, err) -> {
            if (err != null) {
                plugin.getLogger().log(Level.WARNING, "No se pudo leer el acceso de la mesa " + bench.id(), err);
                plugin.lang().send(player, "access.error");
                return;
            }
            new AccessMenu(player, plugin, bench, page, trusted).open();
        });
    }

    private final StructuresPlugin plugin;
    private final WorkbenchRecord bench;
    private final int page;
    private final List<UUID> trusted;

    public AccessMenu(Player player, StructuresPlugin plugin, WorkbenchRecord bench, int page, Set<UUID> trusted) {
        super(player, StructuresMenus.skin().title(StructuresMenus.background("acceso", bench.tier()),
                plugin.menuTitle("access")), 54);
        this.plugin = plugin;
        this.bench = bench;
        this.trusted = trusted.stream().sorted(Comparator.comparing(UUID::toString)).toList();
        this.page = Math.max(0, Math.min(page, pageCount(this.trusted.size()) - 1));
    }

    @Override
    public void build() {
        StructuresMenus.fill(inventory);
        var lang = plugin.lang();
        List<UUID> shown = pageOf(trusted, page);
        for (int i = 0; i < shown.size(); i++) {
            setItem(i, head(shown.get(i)));
        }
        if (page > 0) {
            setItem(ANTERIOR, StructuresMenus.button(Material.ARROW, lang.component("menu.previous"), List.of()));
        }
        if (page < pageCount(trusted.size()) - 1) {
            setItem(SIGUIENTE, StructuresMenus.button(Material.ARROW, lang.component("menu.next"), List.of()));
        }
        setItem(ANADIR, StructuresMenus.button(Material.LIME_DYE, lang.component("access.add"), List.of()));
        setItem(VOLVER, StructuresMenus.button(Material.BARRIER, lang.component("menu.back"), List.of()));
    }

    private ItemStack head(UUID id) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(op);
        String name = op.getName() != null ? op.getName() : id.toString();
        meta.displayName(plugin.lang().component("access.player", "player", name));
        meta.lore(List.of(plugin.lang().component("access.click-remove")));
        item.setItemMeta(meta);
        return StructuresMenus.skin().button(item);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot >= 0 && slot < PAGE_SIZE) {
            List<UUID> shown = pageOf(trusted, page);
            if (slot < shown.size()) {
                UUID target = shown.get(slot);
                plugin.service().untrust(player, bench.id(), target).whenComplete((ok, err) -> done(ok, err, "access.removed"));
            }
        } else if (slot == ANTERIOR && page > 0) {
            show(player, plugin, bench, page - 1);
        } else if (slot == SIGUIENTE && page < pageCount(trusted.size()) - 1) {
            show(player, plugin, bench, page + 1);
        } else if (slot == ANADIR) {
            close();
            plugin.prompts().promptKey(player, "access.prompt", name -> add(name.trim()));
        } else if (slot == VOLVER) {
            new CenterMenu(player, plugin, bench).open();
        }
    }

    private void add(String name) {
        // Solo jugadores que el servidor ya conoce: getOfflinePlayer(nombre) haría una consulta de red bloqueante.
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(name);
        if (target == null) {
            plugin.lang().send(player, "access.not-found", "player", name);
            show(player, plugin, bench, page);
        } else if (target.getUniqueId().equals(bench.owner())) {
            plugin.lang().send(player, "access.is-owner");
            show(player, plugin, bench, page);
        } else {
            plugin.service().trust(player, bench.id(), target.getUniqueId())
                    .whenComplete((ok, err) -> done(ok, err, "access.added"));
        }
    }

    private void done(Boolean ok, Throwable err, String okKey) {
        if (err != null) {
            plugin.getLogger().log(Level.WARNING, "Cambio de acceso fallido en la mesa " + bench.id(), err);
        }
        plugin.lang().send(player, err == null && Boolean.TRUE.equals(ok) ? okKey : "access.error");
        show(player, plugin, bench, page);
    }
}
