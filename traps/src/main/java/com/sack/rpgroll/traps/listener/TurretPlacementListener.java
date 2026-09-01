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
import com.sack.rpgroll.traps.gui.turret.TurretAmmoGUI;
import com.sack.rpgroll.traps.turret.TurretAccess;
import com.sack.rpgroll.traps.turret.TurretTargeting;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
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

    private final Plugin plugin;
    private final TurretManager turretManager;
    private final PlacedTurretManager placedTurretManager;
    private final TurretEngine turretEngine;
    private final com.sack.rpgroll.traps.ammo.AmmoManager ammoManager;
    private final LangManager lang;

    public TurretPlacementListener(Plugin plugin, TurretManager turretManager,
            PlacedTurretManager placedTurretManager, TurretEngine turretEngine, com.sack.rpgroll.traps.ammo.AmmoManager ammoManager, LangManager lang) {
        this.plugin = plugin;
        this.turretManager = turretManager;
        this.placedTurretManager = placedTurretManager;
        this.turretEngine = turretEngine;
        this.ammoManager = ammoManager;
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

        // Arranca con lo que diga su definición; el dueño lo ajusta después
        // desde la GUI.
        PlacedTurret placed = placedTurretManager.add(
                turretId, event.getBlock().getLocation(), event.getPlayer().getUniqueId(),
                TurretTargeting.defaultsFor(definition.get()));

        lang.send(event.getPlayer(), "admin.turret.placed_by_item",
                "id", turretId, "placementId", placed.placementId());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
                || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        PlacedTurret placed = turretAt(event.getClickedBlock().getLocation());

        if (placed == null) {
            return;
        }

        // Es una torreta: nunca se coloca un bloque encima ni se abre otra cosa.
        event.setCancelled(true);

        Player player = event.getPlayer();

        if (!TurretAccess.canManage(player, placed)) {
            lang.send(player, "admin.turret.no_access");
            return;
        }

        new TurretAmmoGUI(plugin, placedTurretManager, ammoManager, lang, placed.placementId()).open(player);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {

        Location broken = event.getBlock().getLocation();
        PlacedTurret placed = turretAt(broken);

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

    private PlacedTurret turretAt(Location location) {
        return placedTurretManager.getAll().stream()
                .filter(candidate -> sameBlock(candidate, location))
                .findFirst()
                .orElse(null);
    }

    private boolean sameBlock(PlacedTurret placed, Location location) {
        return placed.world().equals(location.getWorld().getName())
                && placed.x() == location.getBlockX()
                && placed.y() == location.getBlockY()
                && placed.z() == location.getBlockZ();
    }

    /**
     * Retirar exige lo mismo que reabastecer: dueño, su team/guild, o admin.
     * Sin dueño registrado (colocada por comando) solo la saca un admin.
     */
    private boolean canRemove(Player player, PlacedTurret placed) {
        return TurretAccess.canManage(player, placed);
    }

}
