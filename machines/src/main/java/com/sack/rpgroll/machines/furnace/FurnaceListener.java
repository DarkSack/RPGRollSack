package com.sack.rpgroll.machines.furnace;

import com.sack.rpgroll.machines.core.Displays;
import com.sack.rpgroll.machines.furnace.FurnaceSettings.FurnaceTier;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

/**
 * Hornos mejorados: cuecen más rápido, rinden más el combustible y a veces dan doble.
 * Mayús + clic derecho con la mano vacía abre las mejoras.
 */
public class FurnaceListener implements Listener {

    private final Plugin plugin;
    private final FurnaceService service;
    private final BiConsumer<Player, Block> openMenu;
    private final Map<String, Pending> pending = new HashMap<>();

    public FurnaceListener(Plugin plugin, FurnaceService service, BiConsumer<Player, Block> openMenu) {
        this.plugin = plugin;
        this.service = service;
        this.openMenu = openMenu;
    }

    // ---------------------------------------------------------------- cocción

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStart(FurnaceStartSmeltEvent event) {
        service.tier(event.getBlock()).ifPresent(tier ->
                event.setTotalCookTime(FurnaceMath.cookTime(event.getTotalCookTime(), tier.speed())));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(FurnaceBurnEvent event) {
        service.tier(event.getBlock()).ifPresent(tier ->
                event.setBurnTime(FurnaceMath.burnTime(event.getBurnTime(), tier.speed(), tier.fuel())));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSmelt(FurnaceSmeltEvent event) {

        var tier = service.tier(event.getBlock());
        if (tier.isEmpty() || tier.get().doubleChance() <= 0
                || !(event.getBlock().getState(false) instanceof Furnace furnace)) {
            return;
        }
        ItemStack result = event.getResult();
        ItemStack inSlot = furnace.getInventory().getResult();
        int already = inSlot == null || inSlot.getType().isAir() ? 0 : inSlot.getAmount();
        int amount = FurnaceMath.result(result.getAmount(), already, result.getMaxStackSize(),
                tier.get().doubleChance(), ThreadLocalRandom.current().nextDouble());
        if (amount != result.getAmount()) {
            result = result.clone();
            result.setAmount(amount);
            event.setResult(result);
        }
    }

    // ---------------------------------------------------------------- menú

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null || !player.isSneaking() || !player.getInventory().getItemInMainHand().getType().isAir()
                || !service.isFurnace(block) || !service.settings().allowed(block.getWorld().getName())
                || !player.hasPermission("rpgroll.machines.furnace.upgrade")) {
            return;
        }
        event.setCancelled(true);
        openMenu.accept(player, block);
    }

    // ---------------------------------------------------------------- colocar, romper, explotar

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {

        var id = service.itemTier(event.getItemInHand());
        if (id.isEmpty()) {
            return;
        }
        Block block = event.getBlockPlaced();
        FurnaceTier tier = service.settings().tier(id.get()).orElse(null);
        if (tier != null) {
            service.setTier(block, tier);
        }
    }

    /**
     * El horno sale con su nivel con cualquier herramienta: una mejora cara no se pierde por
     * romperlo a mano. No se toca {@code setDropItems}: en Paper eso también se come lo que hay
     * dentro. Se deja que vanilla lo suelte todo y en {@link #onDrop} se cambia el horno por el
     * que lleva el nivel; si vanilla no soltó el horno (sin pico), se suelta al tick siguiente.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {

        Block block = event.getBlock();
        var id = service.rawTier(block);
        if (id.isEmpty()) {
            return;
        }
        service.refreshFrame(block, null);
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        String key = Displays.key(block);
        Material kind = block.getType();
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        pending.put(key, new Pending(kind, id.get()));
        Bukkit.getScheduler().runTask(plugin, () -> {
            Pending left = pending.remove(key);
            if (left != null) {
                center.getWorld().dropItemNaturally(center, item(left));
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent event) {

        Pending waiting = pending.get(Displays.key(event.getBlock()));
        if (waiting == null) {
            return;
        }
        for (Item drop : event.getItems()) {
            ItemStack stack = drop.getItemStack();
            if (stack.getType() == waiting.kind() && service.itemTier(stack).isEmpty()) {
                ItemStack upgraded = item(waiting);
                upgraded.setAmount(stack.getAmount());
                drop.setItemStack(upgraded);
                pending.remove(Displays.key(event.getBlock()));
                return;
            }
        }
    }

    private ItemStack item(Pending pending) {
        return service.settings().tier(pending.tier())
                .map(tier -> service.item(pending.kind(), tier, 1))
                .orElseGet(() -> service.orphanItem(pending.kind(), pending.tier()));
    }

    private record Pending(Material kind, String tier) {
    }

    /** Un horno mejorado no salta por los aires: se queda donde está. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        protect(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent event) {
        protect(event.blockList());
    }

    private void protect(List<Block> blocks) {
        blocks.removeIf(block -> service.rawTier(block).isPresent());
    }
}
