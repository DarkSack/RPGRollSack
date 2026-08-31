package com.sack.rpgroll.traps.listener;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.traps.turret.PlacedTurret;
import com.sack.rpgroll.traps.turret.PlacedTurretManager;
import com.sack.rpgroll.traps.turret.TurretDefinition;
import com.sack.rpgroll.traps.turret.TurretEngine;
import com.sack.rpgroll.traps.turret.TurretItem;
import com.sack.rpgroll.traps.turret.TurretManager;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

/**
 * Deja colocar y retirar torretas como si fueran un bloque más.
 * <p>
 * Colocar el ítem registra la torreta con quien la puso como dueño; romper
 * el bloque la retira y devuelve el ítem, <b>solo</b> a ese dueño o a un
 * admin. Sin esa restricción cualquiera podría levantar la defensa ajena,
 * que es justo lo que una torreta debería impedir.
 */
public class TurretPlacementListener implements Listener {

    /** Un admin puede retirar torretas de cualquiera: hace falta para limpiar. */
    private static final String ADMIN_PERMISSION = "rpgrolltraps.admin.*";

    private final Plugin plugin;
    private final TurretManager turretManager;
    private final PlacedTurretManager placedTurretManager;
    private final TurretEngine turretEngine;
    private final LangManager lang;

    public TurretPlacementListener(Plugin plugin, TurretManager turretManager,
            PlacedTurretManager placedTurretManager, TurretEngine turretEngine, LangManager lang) {
        this.plugin = plugin;
        this.turretManager = turretManager;
        this.placedTurretManager = placedTurretManager;
        this.turretEngine = turretEngine;
        this.lang = lang;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {

        String turretId = TurretItem.turretIdOf(plugin, event.getItemInHand());

        if (turretId == null) {
            return;
        }

        Optional<TurretDefinition> definition = turretManager.get(turretId);

        if (definition.isEmpty()) {
            // El ítem sobrevivió a que se borrara su definición: mejor no
            // dejar un bloque que nunca va a disparar.
            event.setCancelled(true);
            lang.send(event.getPlayer(), "admin.turret.not_found", "id", turretId);
            return;
        }

        PlacedTurret placed = placedTurretManager.add(
                turretId, event.getBlock().getLocation(), event.getPlayer().getUniqueId());

        lang.send(event.getPlayer(), "admin.turret.placed_by_item",
                "id", turretId, "placementId", placed.placementId());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {

        Location broken = event.getBlock().getLocation();
        PlacedTurret placed = placedTurretManager.getAll().stream()
                .filter(candidate -> sameBlock(candidate, broken))
                .findFirst()
                .orElse(null);

        if (placed == null) {
            return;
        }

        Player player = event.getPlayer();

        if (!canRemove(player, placed)) {
            event.setCancelled(true);
            lang.send(player, "admin.turret.not_owner");
            return;
        }

        turretEngine.despawnVisual(placed.placementId());
        placedTurretManager.remove(placed.placementId());

        // Se devuelve el ítem para poder reubicarla, en vez de perderla.
        turretManager.get(placed.turretId()).ifPresent(definition -> {
            ItemStack item = TurretItem.create(plugin, definition, lang, 1);
            broken.getWorld().dropItemNaturally(broken.add(0.5, 0.5, 0.5), item);
        });

        lang.send(player, "admin.turret.removed_by_break", "placementId", placed.placementId());
    }

    private boolean sameBlock(PlacedTurret placed, Location location) {
        return placed.world().equals(location.getWorld().getName())
                && placed.x() == location.getBlockX()
                && placed.y() == location.getBlockY()
                && placed.z() == location.getBlockZ();
    }

    /** Sin dueño registrado (colocada por comando) solo la saca un admin. */
    private boolean canRemove(Player player, PlacedTurret placed) {
        return player.hasPermission(ADMIN_PERMISSION)
                || (placed.owner() != null && placed.owner().equals(player.getUniqueId()));
    }

}
