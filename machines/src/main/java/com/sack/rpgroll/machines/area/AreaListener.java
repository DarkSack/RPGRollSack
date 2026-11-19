package com.sack.rpgroll.machines.area;

import com.sack.rpgroll.common.block.MachineBreakEvent;
import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;
import java.util.function.BiConsumer;

/** Poner, abrir, romper y proteger las máquinas de área. */
public class AreaListener implements Listener {

    private final AreaService service;
    private final LangManager lang;
    private final BiConsumer<Player, AreaMachine> openMenu;

    public AreaListener(AreaService service, LangManager lang, BiConsumer<Player, AreaMachine> openMenu) {
        this.service = service;
        this.lang = lang;
        this.openMenu = openMenu;
    }

    // ---------------------------------------------------------------- poner

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlaceCheck(BlockPlaceEvent event) {
        var found = service.kind(event.getItemInHand());
        if (found.isEmpty()) {
            return;
        }
        AreaKind kind = found.get();
        AreaSettings settings = kind.settings();
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        String problem = null;
        Object[] args = {};
        if (!settings.allowed(block.getWorld().getName())) {
            problem = "area.world_disabled";
        } else if (!player.hasPermission("rpgroll.machines." + kind.id() + ".use")) {
            problem = "general.no_permission";
        } else if (!player.hasPermission("rpgroll.machines.bypass") && settings.maxPerPlayer() >= 0
                && service.store().count(player.getUniqueId(), kind.id()) >= settings.maxPerPlayer()) {
            problem = "area.limit";
            args = new Object[] {"max", settings.maxPerPlayer(), "machine", lang.raw("area." + kind.id() + ".name")};
        } else if (kind.placeProblem(block) != null) {
            problem = kind.placeProblem(block);
        } else {
            AreaMachine probe = machine(kind, block, player);
            service.copyUpgrades(event.getItemInHand(), kind, probe);
            int side = kind.side(probe);
            if (!service.areaAllowed(probe, block.getWorld(), side)) {
                problem = "area.need_claim";
                args = new Object[] {"side", side};
            }
        }
        if (problem != null) {
            event.setCancelled(true);
            lang.send(player, problem, args);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        var found = service.kind(event.getItemInHand());
        if (found.isEmpty()) {
            return;
        }
        AreaKind kind = found.get();
        Block block = event.getBlockPlaced();
        AreaMachine machine = machine(kind, block, event.getPlayer());
        service.copyUpgrades(event.getItemInHand(), kind, machine);
        service.store().add(machine);
        service.refreshFrame(block, kind);
        lang.send(event.getPlayer(), "area." + kind.id() + ".placed", "side", kind.side(machine));
    }

    private static AreaMachine machine(AreaKind kind, Block block, Player player) {
        return new AreaMachine(kind.id(), block.getWorld().getName(), block.getX(), block.getY(), block.getZ(),
                player.getUniqueId(), player.getName());
    }

    // ---------------------------------------------------------------- abrir

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null || event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        var machine = service.at(event.getClickedBlock());
        if (machine.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        // Mayús con un bloque en la mano: quiere poner algo al lado, no abrir el menú.
        if (player.isSneaking() && !player.getInventory().getItemInMainHand().getType().isAir()) {
            return;
        }
        event.setCancelled(true);
        if (!machine.get().owner().equals(player.getUniqueId()) && !player.hasPermission("rpgroll.machines.bypass")) {
            lang.send(player, "area.not_owner", "owner", machine.get().ownerName());
            return;
        }
        openMenu.accept(player, machine.get());
    }

    // ---------------------------------------------------------------- romper

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        var found = service.at(block);
        if (found.isEmpty()) {
            return;
        }
        if (event instanceof MachineBreakEvent) {
            event.setCancelled(true);
            return;
        }
        AreaMachine machine = found.get();
        Player player = event.getPlayer();
        if (!machine.owner().equals(player.getUniqueId()) && !player.hasPermission("rpgroll.machines.bypass")) {
            event.setCancelled(true);
            lang.send(player, "area.not_owner", "owner", machine.ownerName());
            return;
        }
        event.setDropItems(false);
        event.setExpToDrop(0);
        var center = block.getLocation().add(0.5, 0.5, 0.5);
        service.spill(machine, center);
        var kind = service.kind(machine.kind());
        if (player.getGameMode() != GameMode.CREATIVE && kind.isPresent()) {
            block.getWorld().dropItemNaturally(center, service.item(kind.get(), machine, 1));
        }
        service.removeFrame(block);
        service.store().remove(machine);
    }

    // ---------------------------------------------------------------- proteger

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        protect(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent event) {
        protect(event.blockList());
    }

    private void protect(List<Block> blocks) {
        blocks.removeIf(block -> service.at(block).isPresent());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> service.at(block).isPresent())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPiston(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> service.at(block).isPresent())) {
            event.setCancelled(true);
        }
    }
}
