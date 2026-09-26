package com.sack.rpgroll.extras.backpack;

import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Todo lo que hace una mochila en el mundo: abrirse con clic al aire o con
 * clic derecho en el inventario, ponerse en el suelo como cabeza (y abrirse
 * desde ahí), sobrevivir a explosiones y pistones, y no dejar meter en ella
 * otra mochila.
 */
public class BackpackListener implements Listener {

    private final Plugin plugin;
    private final BackpackService service;
    private final BackpackRecipes recipes;
    private final LangManager lang;

    public BackpackListener(Plugin plugin, BackpackService service, BackpackRecipes recipes, LangManager lang) {
        this.plugin = plugin;
        this.service = service;
        this.recipes = recipes;
        this.lang = lang;
    }

    private BackpackItems items() {
        return service.items();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        recipes.discover(event.getPlayer());
    }

    // ---------------------------------------------------------------- abrir

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {

        if (!service.enabled() || event.getAction() == Action.PHYSICAL) {
            return;
        }

        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();

        // Una mochila puesta en el suelo: clic derecho la abre (con shift y un bloque en la mano, se construye encima).
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && clicked != null && placedId(clicked).isPresent()) {
            if (event.getHand() != EquipmentSlot.HAND || event.useInteractedBlock() == Event.Result.DENY) {
                // La otra mano, o un plugin de protección que ya dijo que no.
                event.setCancelled(true);
                return;
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (player.isSneaking() && !hand.getType().isAir() && hand.getType().isBlock()) {
                return;
            }
            event.setCancelled(true);
            service.openBlock(player, clicked);
            return;
        }

        if (!items().isBackpack(event.getItem()) || event.getHand() == null) {
            return;
        }

        boolean air = event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_AIR;
        boolean blockWithoutPlacing = event.getAction() == Action.RIGHT_CLICK_BLOCK && !service.settings().allowPlace();

        if (air && service.settings().openInAir() || blockWithoutPlacing) {
            event.setCancelled(true);
            int slot = event.getHand() == EquipmentSlot.OFF_HAND ? 40 : player.getInventory().getHeldItemSlot();
            service.openItem(player, slot);
        } else if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            // Sin abrir al aire, igual no debe ponerse en la cabeza como casco.
            event.setCancelled(true);
        }
    }

    // ---------------------------------------------------------------- inventarios

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getView().getTopInventory().getHolder() instanceof BackpackHolder holder) {
            clickInBackpack(event, player, holder);
            return;
        }

        // Clic derecho sobre la mochila en el propio inventario: se abre, como una shulker.
        if (service.enabled() && service.settings().openFromInventory()
                && event.getView().getTopInventory().getType() == InventoryType.CRAFTING
                && event.getClick() == ClickType.RIGHT
                && event.getClickedInventory() == player.getInventory()
                && (event.getCursor() == null || event.getCursor().getType().isAir())
                && items().isBackpack(event.getCurrentItem())) {

            event.setCancelled(true);
            int slot = event.getSlot();
            plugin.getServer().getScheduler().runTask(plugin, () -> service.openItem(player, slot));
        }
    }

    private void clickInBackpack(InventoryClickEvent event, Player player, BackpackHolder holder) {

        // Doble clic junta ítems de todo el inventario, botones incluidos.
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        boolean top = event.getClickedInventory() == event.getView().getTopInventory();

        if (!top) {
            if (event.isShiftClick() && event.getCurrentItem() != null && !event.getCurrentItem().getType().isAir()) {
                event.setCancelled(true);
                if (service.isForbidden(event.getCurrentItem())) {
                    lang.send(player, "backpack.forbidden");
                    return;
                }
                event.setCurrentItem(service.moveIntoPage(holder, event.getCurrentItem()));
            }
            return;
        }

        int slot = event.getSlot();

        if (slot >= holder.buttonRowStart()) {
            event.setCancelled(true);
            button(holder, slot - holder.buttonRowStart());
            return;
        }

        if (!holder.isContentSlot(slot)) {
            event.setCancelled(true);
            return;
        }

        ItemStack incoming = switch (event.getClick()) {
            case NUMBER_KEY -> player.getInventory().getItem(event.getHotbarButton());
            case SWAP_OFFHAND -> player.getInventory().getItemInOffHand();
            default -> event.getCursor();
        };

        if (service.isForbidden(incoming)) {
            event.setCancelled(true);
            lang.send(player, "backpack.forbidden");
        }
    }

    private void button(BackpackHolder holder, int column) {

        BackpackSettings.Gui gui = service.settings().gui();

        if (column == gui.bindSlot()) {
            service.toggleBind(holder);
            return;
        }

        int page = gui.tabSlots().indexOf(column);
        if (page >= 0) {
            service.switchPage(holder, page);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {

        if (!(event.getView().getTopInventory().getHolder() instanceof BackpackHolder holder)) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();

        for (int raw : event.getRawSlots()) {
            if (raw < topSize && (!holder.isContentSlot(raw) || service.isForbidden(event.getOldCursor()))) {
                event.setCancelled(true);
                if (holder.isContentSlot(raw) && event.getWhoClicked() instanceof Player player) {
                    lang.send(player, "backpack.forbidden");
                }
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof BackpackHolder holder) {
            service.onClose(holder);
        }
    }

    // ---------------------------------------------------------------- en el suelo

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {

        ItemStack item = event.getItemInHand();

        if (!items().isBackpack(item)) {
            return;
        }

        if (!service.enabled() || !service.settings().allowPlace()
                || !(event.getBlockPlaced().getState() instanceof TileState state)) {
            event.setCancelled(true);
            return;
        }

        String tierId = items().tierId(item).orElseThrow();
        UUID id = items().id(item).orElseGet(UUID::randomUUID);

        items().mark(state.getPersistentDataContainer(), tierId, id, event.getPlayer().getUniqueId());
        state.update(true, false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {

        Block block = event.getBlock();

        if (!(block.getState() instanceof TileState state)) {
            return;
        }

        Optional<String> tierId = items().tierId(state.getPersistentDataContainer());
        Optional<UUID> id = items().id(state.getPersistentDataContainer());

        if (tierId.isEmpty() || id.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();

        if (!service.mayTouchPlaced(player, state)) {
            event.setCancelled(true);
            lang.send(player, "backpack.placed_denied");
            return;
        }

        BackpackData data = service.peek(id.get());

        if (!service.mayAccess(player, data)) {
            event.setCancelled(true);
            lang.send(player, "backpack.break_denied", "owner", data.ownerName());
            return;
        }

        Optional<BackpackTier> tier = service.settings().tier(tierId.get());

        if (tier.isEmpty()) {
            // Un nivel que se borró de la config: el bloque queda hasta que vuelva, para no perder el contenido.
            event.setCancelled(true);
            lang.send(player, "backpack.unknown_tier", "tier", tierId.get());
            return;
        }

        service.closeIfOpen(id.get());
        event.setDropItems(false);
        event.setExpToDrop(0);

        // También en creativo: el ítem es la llave del contenido, no se puede perder.
        ItemStack drop = service.itemFor(tier.get(), id.get());
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        protect(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        protect(event.blockList());
    }

    private void protect(List<Block> blocks) {
        blocks.removeIf(block -> placedId(block).isPresent());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> placedId(block).isPresent())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> placedId(block).isPresent())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent event) {
        if (placedId(event.getToBlock()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (placedId(event.getBlock()).isPresent()) {
            event.setCancelled(true);
        }
    }

    private Optional<UUID> placedId(Block block) {

        if (block.getType() != Material.PLAYER_HEAD && block.getType() != Material.PLAYER_WALL_HEAD) {
            return Optional.empty();
        }

        return block.getState() instanceof TileState state
                ? items().id(state.getPersistentDataContainer()) : Optional.empty();
    }

    // ---------------------------------------------------------------- crafteo

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {

        ItemStack[] matrix = event.getInventory().getMatrix();
        Optional<BackpackTier> tier = recipes.tierOf(event.getRecipe());

        if (tier.isPresent()) {
            event.getInventory().setResult(service.enabled() ? recipes.result(tier.get(), matrix) : null);
            return;
        }

        // Una mochila no sirve como cabeza cualquiera en otras recetas (estrellas de fuegos artificiales...).
        for (ItemStack stack : matrix) {
            if (items().isBackpack(stack)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

}
