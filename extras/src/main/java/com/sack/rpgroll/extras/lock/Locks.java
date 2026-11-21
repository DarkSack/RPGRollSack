package com.sack.rpgroll.extras.lock;

import com.sack.rpgroll.common.block.Protection;
import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.ConfigurationSection;
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
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cerraduras con código para cofres, barriles, puertas, trampillas y puertas de valla.
 * <p>
 * Con una llave en blanco, agachado + clic derecho en un bloque donde puedas construir: eliges un
 * código de 4 dígitos y la llave queda grabada con él (con una llave ya grabada, se usa su código).
 * Desde entonces abre su dueño, quien lleve una llave con ese código o quien lo teclee. Tras
 * {@code max-fails} fallos seguidos no se puede teclear en {@code penalty-seconds}. Nadie más que el
 * dueño (o {@link #ADMIN_PERMISSION}) lo rompe; ni las explosiones, ni los pistones, ni los zombis;
 * las tolvas no meten ni sacan. El dueño lo quita agachado + clic derecho con una llave.
 * <p>
 * Como el Reforzador, la cerradura de cada bloque va en el PDC del chunk ({@code uuid;código}).
 */
public final class Locks implements Listener {

    public static final String ADMIN_PERMISSION = "rpgrollextras.lock.admin";

    private final Plugin plugin;
    private final LangManager lang;
    private final NamespacedKey blankKey;
    private final NamespacedKey codeKey;
    private final NamespacedKey blankRecipe;
    private final NamespacedKey copyRecipe;
    private boolean enabled;
    private Attempts attempts = new Attempts(3, 60_000);

    public Locks(Plugin plugin, LangManager lang) {
        this.plugin = plugin;
        this.lang = lang;
        this.blankKey = new NamespacedKey(plugin, "lock_key_blank");
        this.codeKey = new NamespacedKey(plugin, "lock_key");
        this.blankRecipe = new NamespacedKey(plugin, "lock_key_blank");
        this.copyRecipe = new NamespacedKey(plugin, "lock_key_copy");
    }

    /** Lee la sección {@code locks} de config.yml (sin ella, encendido). */
    public void configure(ConfigurationSection section) {
        shutdown();
        enabled = section == null || section.getBoolean("enabled", true);
        attempts = new Attempts(section == null ? 3 : section.getInt("max-fails", 3),
                (section == null ? 60 : section.getLong("penalty-seconds", 60)) * 1000L);
        if (!enabled) {
            return;
        }
        // Una llave en blanco: un lingote de hierro y una pepita de oro.
        ShapelessRecipe blank = new ShapelessRecipe(blankRecipe, blankKey());
        blank.addIngredient(Material.IRON_INGOT);
        blank.addIngredient(Material.GOLD_NUGGET);
        Bukkit.addRecipe(blank, true);
        // Copia: una grabada y una en blanco. El resultado de verdad lo pone onPrepare (lleva el código).
        ShapelessRecipe copy = new ShapelessRecipe(copyRecipe, blankKey());
        copy.addIngredient(2, Material.TRIPWIRE_HOOK);
        Bukkit.addRecipe(copy, true);
        Bukkit.getOnlinePlayers().forEach(this::discover);
    }

    public void shutdown() {
        Bukkit.removeRecipe(blankRecipe);
        Bukkit.removeRecipe(copyRecipe);
    }

    private void discover(Player player) {
        player.discoverRecipes(List.of(blankRecipe, copyRecipe));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (enabled) {
            discover(event.getPlayer());
        }
    }

    // ----- Llaves -----

    public ItemStack blankKey() {
        ItemStack item = ItemStack.of(Material.TRIPWIRE_HOOK);
        var meta = item.getItemMeta();
        meta.displayName(lang.component("lock.blank_name"));
        meta.lore(List.of(lang.component("lock.blank_lore_1"), lang.component("lock.blank_lore_2")));
        meta.getPersistentDataContainer().set(blankKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    ItemStack engravedKey(String code, int amount) {
        ItemStack item = ItemStack.of(Material.TRIPWIRE_HOOK, amount);
        var meta = item.getItemMeta();
        meta.displayName(lang.component("lock.key_name", "code", code));
        meta.lore(List.of(lang.component("lock.key_lore_1"), lang.component("lock.key_lore_2")));
        meta.getPersistentDataContainer().set(codeKey, PersistentDataType.STRING, code);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    boolean isBlank(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(blankKey, PersistentDataType.BYTE);
    }

    /** El código grabado en la llave, o null si no es una llave grabada. */
    String codeOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String code = item.getItemMeta().getPersistentDataContainer().get(codeKey, PersistentDataType.STRING);
        return LockData.validCode(code) ? code : null;
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        ItemStack engraved = null;
        int blanks = 0;
        int ours = 0;
        int others = 0;
        for (ItemStack stack : event.getInventory().getMatrix()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            if (isBlank(stack)) {
                blanks++;
                ours++;
            } else if (codeOf(stack) != null) {
                engraved = stack;
                ours++;
            } else {
                others++;
            }
        }
        NamespacedKey recipe = event.getRecipe() instanceof Keyed keyed ? keyed.getKey() : null;
        if (copyRecipe.equals(recipe)) {
            boolean valid = engraved != null && blanks == 1 && ours == 2 && others == 0;
            event.getInventory().setResult(valid ? engravedKey(codeOf(engraved), 2) : null);
        } else if (ours > 0 && !blankRecipe.equals(recipe)) {
            // Una llave nuestra no es un gancho de cable trampa: no entra en recetas vanilla.
            event.getInventory().setResult(null);
        }
    }

    // ----- Registro por chunk -----

    static boolean lockable(Material m) {
        return m == Material.CHEST || m == Material.TRAPPED_CHEST || m == Material.BARREL
                || Tag.DOORS.isTagged(m) || Tag.TRAPDOORS.isTagged(m) || Tag.FENCE_GATES.isTagged(m);
    }

    private static int pack(Block b) {
        return (b.getX() & 15) | ((b.getZ() & 15) << 4) | ((b.getY() + 4096) << 8);
    }

    private NamespacedKey keyOf(Block b) {
        return new NamespacedKey(plugin, "lk" + pack(b));
    }

    /** La cerradura del bloque, o null. */
    LockData lockOf(Block b) {
        if (!b.getWorld().isChunkLoaded(b.getX() >> 4, b.getZ() >> 4)) {
            return null;
        }
        return LockData.decode(b.getChunk().getPersistentDataContainer().get(keyOf(b), PersistentDataType.STRING));
    }

    private void set(Block b, LockData lock) {
        var data = b.getChunk().getPersistentDataContainer();
        if (lock == null) {
            data.remove(keyOf(b));
        } else {
            data.set(keyOf(b), PersistentDataType.STRING, lock.encode());
        }
    }

    /** La otra mitad de una puerta (no de una trampilla: su "mitad" es solo arriba o abajo del hueco). */
    private static List<Block> doorHalves(Block b) {
        List<Block> blocks = new ArrayList<>(2);
        blocks.add(b);
        if (b.getBlockData() instanceof Door door) {
            blocks.add(b.getRelative(door.getHalf() == Door.Half.BOTTOM ? BlockFace.UP : BlockFace.DOWN));
        }
        return blocks;
    }

    /** El bloque, la otra mitad de la puerta y la otra mitad del cofre doble. */
    private static List<Block> parts(Block b) {
        List<Block> blocks = doorHalves(b);
        Block other = otherChestHalf(b);
        if (other != null) {
            blocks.add(other);
        }
        return blocks;
    }

    private static Block otherChestHalf(Block b) {
        if (b.getState() instanceof Chest chest && chest.getInventory().getHolder() instanceof DoubleChest dc) {
            for (InventoryHolder side : new InventoryHolder[] {dc.getLeftSide(), dc.getRightSide()}) {
                if (side instanceof Chest half && !half.getBlock().equals(b)) {
                    return half.getBlock();
                }
            }
        }
        return null;
    }

    private boolean canManage(Player player, LockData lock) {
        return player.getUniqueId().equals(lock.owner()) || player.hasPermission(ADMIN_PERMISSION);
    }

    private static String name(UUID owner) {
        String name = Bukkit.getOfflinePlayer(owner).getName();
        return name == null ? "?" : name;
    }

    // ----- Poner, quitar y abrir -----

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (!enabled || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || block == null || !lockable(block.getType())) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack hand = event.getItem();
        LockData lock = lockOf(block);
        // Otro plugin (el claim) ya dijo que no: se respeta, salvo para el dueño de la cerradura.
        boolean denied = event.useInteractedBlock() == Event.Result.DENY;

        if (player.isSneaking() && (isBlank(hand) || codeOf(hand) != null)) {
            event.setCancelled(true);
            if (lock == null) {
                startLock(player, block);
            } else if (canManage(player, lock)) {
                new LockMenuGUI(player, lang, () -> removeLock(player, block)).open();
            } else {
                player.sendActionBar(lang.component("lock.protected", "owner", name(lock.owner())));
            }
            return;
        }
        if (lock == null) {
            return;
        }
        if (canManage(player, lock) || lock.code().equals(codeOf(hand))
                || lock.code().equals(codeOf(player.getInventory().getItemInOffHand()))) {
            // Las de hierro no se abren a mano: lo hacemos nosotros.
            if (block.getType() == Material.IRON_DOOR || block.getType() == Material.IRON_TRAPDOOR) {
                event.setCancelled(true);
                toggle(block);
            }
            return;
        }
        event.setCancelled(true);
        if (denied) {
            return;
        }
        long wait = attempts.blockedFor(player.getUniqueId(), System.currentTimeMillis());
        if (wait > 0) {
            player.sendActionBar(lang.component("lock.blocked", "seconds", (wait + 999) / 1000));
            return;
        }
        new KeypadGUI(player, lang, true, code -> Bukkit.getScheduler().runTask(plugin, () -> tryCode(player, block, code)))
                .open();
    }

    private void tryCode(Player player, Block block, String code) {
        LockData lock = lockOf(block);
        if (lock == null || !player.isOnline()) {
            return;
        }
        if (!lock.code().equals(code)) {
            attempts.fail(player.getUniqueId(), System.currentTimeMillis());
            player.sendActionBar(lang.component("lock.wrong"));
            player.playSound(player, Sound.BLOCK_IRON_DOOR_CLOSE, 0.6f, 1.6f);
            return;
        }
        attempts.success(player.getUniqueId());
        // Hay que estar cerca: el teclado no abre a distancia.
        if (player.getLocation().distanceSquared(block.getLocation().add(0.5, 0.5, 0.5)) > 36) {
            return;
        }
        BlockState state = block.getState();
        if (state instanceof Chest chest) {
            player.openInventory(chest.getInventory());
        } else if (state instanceof Container container) {
            player.openInventory(container.getInventory());
        } else {
            toggle(block);
        }
        player.sendActionBar(lang.component("lock.opened"));
    }

    private void startLock(Player player, Block block) {
        // Solo donde podrías romperlo tú: no se cierra lo de otro para estorbarle.
        if (!Protection.canBreak(player, block)) {
            player.sendActionBar(lang.component("lock.not_here"));
            return;
        }
        String keyCode = codeOf(player.getInventory().getItemInMainHand());
        if (keyCode != null) {
            applyLock(block, new LockData(player.getUniqueId(), keyCode));
            player.sendActionBar(lang.component("lock.locked_with_key"));
            player.playSound(block.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 0.8f, 1.4f);
            return;
        }
        new KeypadGUI(player, lang, false, code -> Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (!player.isOnline() || !isBlank(hand) || !lockable(block.getType()) || lockOf(block) != null) {
                return;
            }
            applyLock(block, new LockData(player.getUniqueId(), code));
            // La llave en blanco de la mano pasa a llevar el código.
            hand.setAmount(hand.getAmount() - 1);
            player.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? engravedKey(code, 1) : hand);
            if (hand.getAmount() > 0) {
                player.getInventory().addItem(engravedKey(code, 1))
                        .values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
            }
            player.sendActionBar(lang.component("lock.locked", "code", code));
            player.playSound(block.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 0.8f, 1.4f);
        })).open();
    }

    private void applyLock(Block block, LockData lock) {
        for (Block part : parts(block)) {
            set(part, lock);
        }
    }

    private void removeLock(Player player, Block block) {
        LockData lock = lockOf(block);
        if (lock == null || !canManage(player, lock)) {
            return;
        }
        for (Block part : parts(block)) {
            set(part, null);
        }
        player.sendActionBar(lang.component("lock.removed"));
        player.playSound(block.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 0.8f, 1.4f);
    }

    private static void toggle(Block block) {
        if (!(block.getBlockData() instanceof Openable first)) {
            return;
        }
        boolean open = !first.isOpen();
        for (Block part : doorHalves(block)) {
            if (part.getBlockData() instanceof Openable data) {
                data.setOpen(open);
                part.setBlockData(data, false);
            }
        }
        block.getWorld().playSound(block.getLocation(), sound(block.getType(), open), 1f, 1f);
    }

    private static Sound sound(Material m, boolean open) {
        boolean iron = m == Material.IRON_DOOR || m == Material.IRON_TRAPDOOR;
        if (Tag.TRAPDOORS.isTagged(m)) {
            return iron ? (open ? Sound.BLOCK_IRON_TRAPDOOR_OPEN : Sound.BLOCK_IRON_TRAPDOOR_CLOSE)
                    : (open ? Sound.BLOCK_WOODEN_TRAPDOOR_OPEN : Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE);
        }
        if (Tag.FENCE_GATES.isTagged(m)) {
            return open ? Sound.BLOCK_FENCE_GATE_OPEN : Sound.BLOCK_FENCE_GATE_CLOSE;
        }
        return iron ? (open ? Sound.BLOCK_IRON_DOOR_OPEN : Sound.BLOCK_IRON_DOOR_CLOSE)
                : (open ? Sound.BLOCK_WOODEN_DOOR_OPEN : Sound.BLOCK_WOODEN_DOOR_CLOSE);
    }

    // ----- Romper y colocar -----

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {
        LockData lock = lockOf(event.getBlock());
        if (lock == null || canManage(event.getPlayer(), lock)) {
            return;
        }
        event.setCancelled(true);
        if (!Protection.testing()) {
            event.getPlayer().sendActionBar(lang.component("lock.no_break", "owner", name(lock.owner())));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBroken(BlockBreakEvent event) {
        if (lockOf(event.getBlock()) != null) {
            for (Block part : doorHalves(event.getBlock())) {
                set(part, null);
            }
        }
    }

    /**
     * Lo que se pone donde hubo un bloque cerrado no hereda la cerradura; y un cofre pegado a uno
     * cerrado (que lo haría doble) solo lo pone su dueño, que se lleva la cerradura a la otra mitad.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        for (Block part : doorHalves(placed)) {
            if (lockOf(part) != null) {
                set(part, null);
            }
        }
        Block other = otherChestHalf(placed);
        LockData lock = other == null ? null : lockOf(other);
        if (lock == null) {
            return;
        }
        if (canManage(event.getPlayer(), lock)) {
            set(placed, lock);
        } else {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(lang.component("lock.protected", "owner", name(lock.owner())));
        }
    }

    private boolean isLocked(Block b) {
        return lockable(b.getType()) && lockOf(b) != null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isLocked);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isLocked);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isLocked)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonBack(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isLocked)) {
            event.setCancelled(true);
        }
    }

    /** Zombis rompiendo puertas, endermans, withers... */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChange(EntityChangeBlockEvent event) {
        if (isLocked(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    // ----- Tolvas -----

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent event) {
        if (lockedInventory(event.getSource()) || lockedInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    private boolean lockedInventory(Inventory inventory) {
        Location location = inventory.getLocation();
        return location != null && location.getWorld() != null && isLocked(location.getBlock());
    }
}
