package com.sack.rpgroll.extras.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * El comportamiento del ítem del menú: se entrega al entrar, cualquier clic
 * con él en la mano abre el menú, no se tira, no se saca del inventario y no
 * se pierde al morir.
 */
public class ServerMenuListener implements Listener {

    private final Plugin plugin;
    private final ServerMenu menu;

    public ServerMenuListener(Plugin plugin, ServerMenu menu) {
        this.plugin = plugin;
        this.menu = menu;
    }

    private boolean active() {
        return menu.config().enabled();
    }

    private boolean locked() {
        return active() && menu.config().locked();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {

        if (!active()) {
            // Si se apagó la función, no quedan brújulas huérfanas en los inventarios.
            menu.remove(event.getPlayer());
            return;
        }

        if (menu.config().giveOnJoin()) {
            menu.give(event.getPlayer());
        }
    }

    // ---------------------------------------------------------------- abrir

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {

        if (!active() || event.getAction() == Action.PHYSICAL || !menu.isMenuItem(event.getItem())) {
            return;
        }

        event.setCancelled(true);
        menu.openMain(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractEntity(PlayerInteractEntityEvent event) {

        ItemStack hand = event.getPlayer().getInventory().getItem(event.getHand());

        if (!active() || !menu.isMenuItem(hand)) {
            return;
        }

        event.setCancelled(true);
        menu.openMain(event.getPlayer());
    }

    // ---------------------------------------------------------------- no se va

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (locked() && menu.isMenuItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (locked() && (menu.isMenuItem(event.getMainHandItem()) || menu.isMenuItem(event.getOffHandItem()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {

        if (!locked() || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack hotbarSwap = event.getClick() == ClickType.NUMBER_KEY
                ? player.getInventory().getItem(event.getHotbarButton()) : null;
        boolean involved = menu.isMenuItem(event.getCurrentItem()) || menu.isMenuItem(event.getCursor())
                || menu.isMenuItem(hotbarSwap)
                || event.getClick() == ClickType.SWAP_OFFHAND && menu.isMenuItem(player.getInventory().getItemInOffHand());

        if (!involved) {
            return;
        }

        event.setCancelled(true);

        // Clic sobre la brújula en el propio inventario: abre el menú, como en la mano.
        if (menu.isMenuItem(event.getCurrentItem()) && event.getView().getTopInventory().getType() == InventoryType.CRAFTING) {
            plugin.getServer().getScheduler().runTask(plugin, () -> menu.openMain(player));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (locked() && menu.isMenuItem(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    // ---------------------------------------------------------------- muerte

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        if (active() && menu.config().keepOnDeath()) {
            event.getDrops().removeIf(menu::isMenuItem);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        if (active() && menu.config().keepOnDeath()) {
            // Un tick después: en el mismo tick el inventario aún se está reconstruyendo.
            plugin.getServer().getScheduler().runTask(plugin, () -> menu.give(event.getPlayer()));
        }
    }

}
