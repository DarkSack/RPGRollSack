package com.sack.rpgroll.enchantments.station;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Un bloque de estación (Infusor, Súper encantador): un bloque vanilla colocado desde un ítem marcado.
 * Su posición se guarda en el PDC del chunk; clic derecho abre la estación en vez de lo vanilla, y al
 * romperlo suelta el ítem marcado. Las explosiones y los pistones no lo mueven ni lo rompen.
 * <p>
 * Con un modelo del resource pack ({@code model} en config.yml) el bloque colocado pasa a ser una
 * barrera con un {@link ItemDisplay} encima: se ve solo el modelo, se choca con él y se rompe con
 * clic izquierdo, como los muebles de RPGRoll-Furniture (con las protecciones de por medio).
 */
public final class StationBlocks implements Listener {

    private final Material block;
    private final NamespacedKey itemKey;
    private final NamespacedKey chunkKey;
    private final Supplier<ItemStack> item;
    private final Consumer<Player> open;
    private final Plugin plugin;
    private NamespacedKey model;

    public StationBlocks(Plugin plugin, String id, Material block, Supplier<ItemStack> item, Consumer<Player> open) {
        this.block = block;
        this.itemKey = new NamespacedKey(plugin, id);
        this.chunkKey = new NamespacedKey(plugin, id + "_blocks");
        this.item = item;
        this.open = open;
        this.plugin = plugin;
    }

    /** El modelo del resource pack ({@code namespace:id}); vacío o inválido, el bloque vanilla tal cual. */
    public void model(String id) {
        model = id == null || id.isBlank() ? null : NamespacedKey.fromString(id.trim().toLowerCase(java.util.Locale.ROOT));
    }

    public boolean hasModel() {
        return model != null;
    }

    /** Pone el modelo en el ítem de la estación (si hay). */
    public void decorate(ItemMeta meta) {
        if (model != null) {
            meta.setItemModel(model);
        }
    }

    public NamespacedKey itemKey() {
        return itemKey;
    }

    public boolean isItem(ItemStack stack) {
        return stack != null && stack.getType() == block && stack.hasItemMeta()
                && stack.getItemMeta().getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }

    static int pack(Block b) {
        return (b.getX() & 15) | ((b.getZ() & 15) << 4) | ((b.getY() + 4096) << 8);
    }

    private int[] stored(Chunk chunk) {
        int[] values = chunk.getPersistentDataContainer().get(chunkKey, PersistentDataType.INTEGER_ARRAY);
        return values == null ? new int[0] : values;
    }

    public boolean isStation(Block b) {
        if (b.getType() != block && b.getType() != Material.BARRIER) {
            return false;
        }
        int key = pack(b);
        return Arrays.stream(stored(b.getChunk())).anyMatch(v -> v == key);
    }

    private void mark(Block b, boolean add) {
        Chunk chunk = b.getChunk();
        int key = pack(b);
        int[] current = Arrays.stream(stored(chunk)).filter(v -> v != key).toArray();
        if (add) {
            current = Arrays.copyOf(current, current.length + 1);
            current[current.length - 1] = key;
        }
        if (current.length == 0) {
            chunk.getPersistentDataContainer().remove(chunkKey);
        } else {
            chunk.getPersistentDataContainer().set(chunkKey, PersistentDataType.INTEGER_ARRAY, current);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!isItem(event.getItemInHand())) {
            return;
        }
        Block b = event.getBlockPlaced();
        mark(b, true);
        NamespacedKey current = model;
        if (current != null) {
            // Al tick siguiente: cambiar el bloque dentro del propio evento de colocar no siempre se queda.
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (b.getType() == block && isStation(b)) {
                    b.setType(Material.BARRIER, false);
                    spawnDisplay(b, current);
                }
            });
        }
    }

    private void spawnDisplay(Block b, NamespacedKey id) {
        removeDisplay(b);
        ItemStack shown = ItemStack.of(Material.PAPER);
        ItemMeta meta = shown.getItemMeta();
        meta.setItemModel(id);
        shown.setItemMeta(meta);
        b.getWorld().spawn(b.getLocation().add(0.5, 0.5, 0.5), ItemDisplay.class, display -> {
            display.setItemStack(shown);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.getPersistentDataContainer().set(chunkKey, PersistentDataType.INTEGER, pack(b));
            display.setPersistent(true);
        });
    }

    private void removeDisplay(Block b) {
        int key = pack(b);
        b.getWorld().getNearbyEntitiesByType(ItemDisplay.class, b.getLocation().add(0.5, 0.5, 0.5), 0.6).stream()
                .filter(d -> Integer.valueOf(key).equals(d.getPersistentDataContainer().get(chunkKey, PersistentDataType.INTEGER)))
                .forEach(ItemDisplay::remove);
    }

    /** La barrera no se pica en supervivencia: clic izquierdo la rompe si las protecciones lo dejan. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onHit(PlayerInteractEvent event) {
        Block b = event.getClickedBlock();
        if (event.getAction() != Action.LEFT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || b == null
                || b.getType() != Material.BARRIER || event.getPlayer().getGameMode() == GameMode.CREATIVE || !isStation(b)) {
            return;
        }
        event.setCancelled(true);
        if (new BlockBreakEvent(b, event.getPlayer()).callEvent()) {
            b.setType(Material.AIR);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null || event.getPlayer().isSneaking() || !isStation(event.getClickedBlock())) {
            return;
        }
        event.setCancelled(true);
        open.accept(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        if (!isStation(b)) {
            return;
        }
        mark(b, false);
        removeDisplay(b);
        event.setDropItems(false);
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), item.get());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isStation);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isStation);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isStation)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonBack(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isStation)) {
            event.setCancelled(true);
        }
    }
}
