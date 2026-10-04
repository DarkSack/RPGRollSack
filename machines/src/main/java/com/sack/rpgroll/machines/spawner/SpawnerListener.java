package com.sack.rpgroll.machines.spawner;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.machines.core.Displays;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Spawners: mayús + clic derecho con la mano vacía abre las mejoras, clic derecho con un
 * spawner del mismo mob lo apila, y con toque de seda (y permiso) se recogen con sus niveles.
 */
public class SpawnerListener implements Listener {

    private final SpawnerService service;
    private final LangManager lang;
    private final Displays displays;
    private final BiConsumer<Player, Block> openMenu;

    public SpawnerListener(SpawnerService service, LangManager lang, Displays displays,
            BiConsumer<Player, Block> openMenu) {
        this.service = service;
        this.lang = lang;
        this.displays = displays;
        this.openMenu = openMenu;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SPAWNER
                || !service.settings().allowed(block.getWorld().getName())) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().isAir()) {
            if (player.isSneaking() && player.hasPermission("rpgroll.machines.spawner.upgrade")) {
                event.setCancelled(true);
                openMenu.accept(player, block);
            }
            return;
        }

        var spawner = service.spawner(block);
        if (spawner.isEmpty()) {
            return;
        }

        // Un huevo cambiaría el mob de toda la pila: con mejoras o apilado, no.
        if (hand.getType().name().endsWith("_SPAWN_EGG") && service.data(spawner.get()).upgraded()) {
            event.setCancelled(true);
            lang.send(player, "spawner.no_egg");
            return;
        }

        var held = service.held(hand);
        if (held.isEmpty() || !service.settings().stack()) {
            return;
        }
        // Clic derecho con un spawner en la mano sobre otro: se apila (en vez de colocarlo al lado).
        event.setCancelled(true);
        if (!player.hasPermission("rpgroll.machines.spawner.upgrade")) {
            return;
        }
        CreatureSpawner target = spawner.get();
        if (held.get().mob() == null || held.get().mob() != target.getSpawnedType()) {
            lang.send(player, "spawner.stack_other_mob");
            return;
        }
        SpawnerData data = service.data(target);
        int added = held.get().data().stack();
        if (data.stack() + added > service.settings().maxStack()) {
            lang.send(player, "spawner.stack_full", "max", service.settings().maxStack());
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
            hand.setAmount(hand.getAmount() - 1);
        }
        SpawnerData stacked = data.withStack(data.stack() + added);
        service.apply(block, null, stacked);
        player.playSound(block.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.2f);
        lang.send(player, "spawner.stacked", "stack", stacked.stack(), "max", service.settings().maxStack());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        service.held(event.getItemInHand()).ifPresent(held ->
                service.apply(event.getBlockPlaced(), held.mob(), held.data()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {

        Block block = event.getBlock();
        var spawner = service.spawner(block);
        if (spawner.isEmpty()) {
            return;
        }
        displays.remove(block, Displays.HOLOGRAM);

        Player player = event.getPlayer();
        SpawnerSettings settings = service.settings();
        if (player.getGameMode() == GameMode.CREATIVE || !settings.mine() || !settings.allowed(block.getWorld().getName())
                || !player.hasPermission("rpgroll.machines.spawner.mine")) {
            return;
        }
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (settings.mineNeedsSilk() && tool.getEnchantmentLevel(Enchantment.SILK_TOUCH) <= 0) {
            return;
        }
        // Recogido: sin experiencia (si no, se podría picar y volver a poner para farmearla).
        event.setExpToDrop(0);
        SpawnerData data = service.data(spawner.get());
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5),
                service.item(spawner.get().getSpawnedType(), data, 1));
    }

    /** Un spawner mejorado o apilado aguanta las explosiones. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        protect(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent event) {
        protect(event.blockList());
    }

    private void protect(List<Block> blocks) {
        blocks.removeIf(block -> service.spawner(block).map(s -> service.data(s).upgraded()).orElse(false));
    }
}
