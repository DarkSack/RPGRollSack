package com.sack.rpgroll.crates.lucky;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Los lucky blocks en el mundo: se colocan con su ítem (clic derecho), se
 * abren al romperlos y, mientras tanto, siguen siendo lo que son.
 * <p>
 * El ítem no es un bloque (Bedrock no sabría colocarlo), así que la
 * colocación la hace este listener y la anuncia con un
 * {@link BlockPlaceEvent}: las protecciones (GriefPrevention, WorldGuard) y
 * CoreProtect la ven como cualquier otra.
 */
public class LuckyListener implements Listener {

    /** El instrumento reservado a los lucky blocks. Las menas de RPGRoll-Items usan zombie. */
    public static final Instrument INSTRUMENT = Instrument.SKELETON;

    private final Plugin plugin;
    private final LuckyManager manager;
    private final LuckyItems items;
    private final LuckyStore store;
    private final LuckyExecutor executor;
    private final Set<String> disabledWorlds;

    public LuckyListener(Plugin plugin, LuckyManager manager, LuckyItems items, LuckyStore store,
            LuckyExecutor executor, Set<String> disabledWorlds) {
        this.plugin = plugin;
        this.manager = manager;
        this.items = items;
        this.store = store;
        this.executor = executor;
        this.disabledWorlds = disabledWorlds;
    }

    /** El lucky block colocado en {@code block}, si lo es. */
    public Optional<LuckyBlock> at(Block block) {
        if (block.getType() != Material.NOTE_BLOCK || !(block.getBlockData() instanceof NoteBlock note)
                || note.getInstrument() != INSTRUMENT || !store.contains(block)) {
            return Optional.empty();
        }
        return manager.byNote(note.getNote().getId());
    }

    // ---------------------------------------------------------------- colocar

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
                || event.getHand() == null) {
            return;
        }

        ItemStack hand = event.getItem();
        if (!items.isLucky(hand)) {
            return;
        }

        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();

        // Clic sobre un cofre, una puerta...: primero lo suyo, como con cualquier bloque.
        if (!player.isSneaking() && clicked.getType().isInteractable()
                && event.useInteractedBlock() != Event.Result.DENY) {
            return;
        }

        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);

        Optional<LuckyBlock> lucky = items.of(hand);
        if (lucky.isEmpty() || disabledWorlds.contains(clicked.getWorld().getName().toLowerCase())) {
            return;
        }

        Block target = clicked.isReplaceable() ? clicked : clicked.getRelative(event.getBlockFace());
        if (!target.isReplaceable() || target.getY() < target.getWorld().getMinHeight()
                || target.getY() >= target.getWorld().getMaxHeight()
                || !target.getWorld().getNearbyEntities(BoundingBox.of(target), e -> e instanceof LivingEntity)
                        .isEmpty()) {
            return;
        }

        if (place(player, target, clicked, hand, event.getHand(), lucky.get())) {
            if (player.getGameMode() != GameMode.CREATIVE) {
                hand.setAmount(hand.getAmount() - 1);
            }
            player.swingHand(event.getHand());
            target.getWorld().playSound(target.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_WOOD_PLACE, 1f, 0.9f);
        }
    }

    private boolean place(Player player, Block target, Block against, ItemStack hand, EquipmentSlot slot,
            LuckyBlock lucky) {

        BlockState before = target.getState();
        NoteBlock data = (NoteBlock) Material.NOTE_BLOCK.createBlockData();
        data.setInstrument(INSTRUMENT);
        data.setNote(new Note(lucky.note()));
        data.setPowered(false);
        target.setBlockData(data, false);

        BlockPlaceEvent placeEvent = new BlockPlaceEvent(target, before, against, hand, player, true, slot);
        Bukkit.getPluginManager().callEvent(placeEvent);

        if (placeEvent.isCancelled() || !placeEvent.canBuild()) {
            before.update(true, false);
            return false;
        }

        store.add(target);
        return true;
    }

    /** Un bloque musical normal nunca se queda con el instrumento de los lucky blocks (calavera encima). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {

        Block block = event.getBlockPlaced();
        if (block.getType() != Material.NOTE_BLOCK || !(block.getBlockData() instanceof NoteBlock note)
                || note.getInstrument() != INSTRUMENT || items.isLucky(event.getItemInHand())) {
            return;
        }

        note.setInstrument(Instrument.PIANO);
        block.setBlockData(note, false);
    }

    // ---------------------------------------------------------------- abrir

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {

        Block block = event.getBlock();
        Optional<LuckyBlock> lucky = at(block);
        if (lucky.isEmpty()) {
            return;
        }

        event.setDropItems(false);
        event.setExpToDrop(0);
        store.remove(block);

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        LuckyOutcome outcome = lucky.get().roll(ThreadLocalRandom.current());
        if (outcome == null) {
            return;
        }

        // Al tick siguiente, con el bloque ya roto: lo que aparezca no choca con él.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                executor.run(player, block, lucky.get(), outcome);
            }
        });
    }

    /** Para probar un resultado concreto (/lucky test): como si se hubiera roto {@code block}. */
    public void open(Player player, Block block, LuckyBlock lucky, LuckyOutcome outcome) {
        executor.run(player, block, lucky, outcome);
    }

    // ---------------------------------------------------------------- que siga siendo lo que es

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        explode(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        explode(event.blockList());
    }

    /** Una explosión no lo abre: lo devuelve como ítem, para no perderlo. */
    private void explode(List<Block> blocks) {
        for (Iterator<Block> it = blocks.iterator(); it.hasNext(); ) {
            Block block = it.next();
            Optional<LuckyBlock> lucky = at(block);
            if (lucky.isPresent()) {
                it.remove();
                store.remove(block);
                block.setType(Material.AIR, false);
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5),
                        items.create(lucky.get(), 1));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> at(block).isPresent())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> at(block).isPresent())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (at(event.getBlock()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onNote(NotePlayEvent event) {
        if (at(event.getBlock()).isPresent()) {
            event.setCancelled(true);
        }
    }

    /** Clic derecho afina un bloque musical: en un lucky block lo cambiaría de tipo. */
    @EventHandler(priority = EventPriority.LOW)
    public void onTune(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                && at(event.getClickedBlock()).isPresent()) {
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

}
