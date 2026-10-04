package com.sack.rpgroll.machines.quarry;

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

/** Poner, abrir, romper y proteger canteras. */
public class QuarryListener implements Listener {

    private final QuarryService service;
    private final LangManager lang;
    private final BiConsumer<Player, Quarry> openMenu;

    public QuarryListener(QuarryService service, LangManager lang, BiConsumer<Player, Quarry> openMenu) {
        this.service = service;
        this.lang = lang;
        this.openMenu = openMenu;
    }

    // ---------------------------------------------------------------- poner

    /** Las comprobaciones, antes de que nadie la dé por puesta. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlaceCheck(BlockPlaceEvent event) {

        if (!service.isItem(event.getItemInHand())) {
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        QuarrySettings settings = service.settings();

        String problem = null;
        Object[] args = {};
        if (!settings.allowed(block.getWorld().getName())) {
            problem = "quarry.world_disabled";
        } else if (!player.hasPermission("rpgroll.machines.quarry.use")) {
            problem = "general.no_permission";
        } else if (!player.hasPermission("rpgroll.machines.bypass") && settings.maxPerPlayer() >= 0
                && service.store().count(player.getUniqueId()) >= settings.maxPerPlayer()) {
            problem = "quarry.limit";
            args = new Object[] {"max", settings.maxPerPlayer()};
        } else {
            Quarry probe = new Quarry(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(),
                    player.getUniqueId(), player.getName());
            service.copyUpgrades(event.getItemInHand(), probe);
            int side = service.side(probe);
            if (!service.areaAllowed(probe, block.getWorld(), side)) {
                problem = "quarry.need_claim";
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

        if (!service.isItem(event.getItemInHand())) {
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        Quarry quarry = new Quarry(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(),
                player.getUniqueId(), player.getName());
        service.copyUpgrades(event.getItemInHand(), quarry);
        service.store().add(quarry);
        service.refreshFrame(block);
        lang.send(player, "quarry.placed", "side", service.side(quarry));
    }

    // ---------------------------------------------------------------- abrir

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null || event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        var quarry = service.at(event.getClickedBlock());
        if (quarry.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        // Mayús con un bloque en la mano: quiere poner algo al lado, no abrir el menú.
        if (player.isSneaking() && !player.getInventory().getItemInMainHand().getType().isAir()) {
            return;
        }
        event.setCancelled(true);
        if (!quarry.get().owner().equals(player.getUniqueId()) && !player.hasPermission("rpgroll.machines.bypass")) {
            lang.send(player, "quarry.not_owner", "owner", quarry.get().ownerName());
            return;
        }
        openMenu.accept(player, quarry.get());
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
        Quarry quarry = found.get();
        Player player = event.getPlayer();
        if (!quarry.owner().equals(player.getUniqueId()) && !player.hasPermission("rpgroll.machines.bypass")) {
            event.setCancelled(true);
            lang.send(player, "quarry.not_owner", "owner", quarry.ownerName());
            return;
        }
        event.setDropItems(false);
        event.setExpToDrop(0);
        var center = block.getLocation().add(0.5, 0.5, 0.5);
        service.spill(quarry, center);
        if (player.getGameMode() != GameMode.CREATIVE) {
            block.getWorld().dropItemNaturally(center, service.item(quarry, 1));
        }
        service.removeFrame(block);
        service.store().remove(quarry);
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
