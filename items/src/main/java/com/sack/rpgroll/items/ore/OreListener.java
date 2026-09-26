package com.sack.rpgroll.items.ore;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.items.ItemsPlugin;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.GameMode;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cómo se comporta una mena en el mundo.
 *
 * <ul>
 *   <li>Picarla tarda lo que dice su {@code hardness}: el cliente cree que es
 *       un bloque musical, así que mientras se pica se ajusta la velocidad de
 *       picado del jugador (atributo {@code block_break_speed}, que el cliente
 *       respeta y los anticheats conocen).</li>
 *   <li>Solo suelta su ítem con un pico de nivel suficiente; nunca el bloque.</li>
 *   <li>No suena, no se afina, no arde, y una explosión la rompe como mena.</li>
 *   <li>Un bloque musical colocado a mano no puede quedarse con un
 *       instrumento reservado a las menas.</li>
 * </ul>
 */
public class OreListener implements Listener {

    /** Dureza del bloque musical, que es lo que el cliente cree que pica. */
    private static final double NOTE_BLOCK_HARDNESS = 0.8;

    private final ItemsPlugin plugin;
    private final OreService service;
    private final LangManager lang;
    private final NamespacedKey modifierKey;

    public OreListener(ItemsPlugin plugin, OreService service, LangManager lang) {
        this.plugin = plugin;
        this.service = service;
        this.lang = lang;
        this.modifierKey = new NamespacedKey(plugin, "ore_dig");
    }

    // ---------------------------------------------------------------- velocidad de picado

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(BlockDamageEvent event) {

        Player player = event.getPlayer();
        var ore = service.at(event.getBlock());

        if (ore.isEmpty() || player.getGameMode() == GameMode.CREATIVE) {
            clear(player);
            return;
        }

        ItemStack tool = event.getItemInHand();
        Material type = tool == null ? Material.AIR : tool.getType();
        int efficiency = tool == null ? 0 : tool.getEnchantmentLevel(Enchantment.EFFICIENCY);

        // Lo que el cliente calcula para un bloque musical: solo el hacha es su herramienta.
        double current = OreService.isAxe(type) ? OreMath.withEfficiency(service.speed(tool), efficiency) : 1.0;

        boolean pickaxe = OreService.isPickaxe(type);
        boolean harvestable = pickaxe && service.tier(tool) >= ore.get().ore().requiredTier();
        double desired = pickaxe ? OreMath.withEfficiency(service.speed(tool), efficiency) : 1.0;

        double multiplier = OreMath.breakSpeedMultiplier(current, NOTE_BLOCK_HARDNESS, desired,
                ore.get().ore().hardness(), harvestable);

        apply(player, multiplier);
    }

    @EventHandler
    public void onAbort(BlockDamageAbortEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent event) {
        clear(event.getPlayer());
    }

    private void apply(Player player, double multiplier) {

        AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (attribute == null) {
            return;
        }

        attribute.removeModifier(modifierKey);
        attribute.addTransientModifier(new AttributeModifier(modifierKey, multiplier - 1.0,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }

    public void clear(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (attribute != null) {
            attribute.removeModifier(modifierKey);
        }
    }

    // ---------------------------------------------------------------- romperla

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        clear(player);

        var hit = service.at(event.getBlock());
        if (hit.isEmpty()) {
            return;
        }

        OreDefinition ore = hit.get().ore();
        event.setDropItems(false);
        event.setExpToDrop(0);

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!OreService.isPickaxe(tool.getType()) || service.tier(tool) < ore.requiredTier()) {
            player.sendActionBar(ComponentUtils.parse(lang.raw("ore.need_better_tool", "ore", ore.displayName(),
                    "tier", ore.requiredTier())));
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int amount = ore.dropMin() + random.nextInt(ore.dropMax() - ore.dropMin() + 1);
        boolean silk = tool.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0;

        if (ore.fortune() && !silk) {
            amount = OreMath.fortune(amount, tool.getEnchantmentLevel(Enchantment.FORTUNE), random);
        }

        drop(event.getBlock(), ore, amount);

        if (!silk && ore.xpMax() > 0) {
            event.setExpToDrop(ore.xpMin() + random.nextInt(ore.xpMax() - ore.xpMin() + 1));
        }
    }

    private void drop(Block block, OreDefinition ore, int amount) {

        Location center = block.getLocation().add(0.5, 0.5, 0.5);

        while (amount > 0) {
            int stack = Math.min(amount, 64);
            var item = plugin.create(ore.dropItem(), stack);
            if (item.isEmpty()) {
                plugin.getLogger().warning("✘ Mena '" + ore.id() + "': su drop '" + ore.dropItem() + "' no existe.");
                return;
            }
            block.getWorld().dropItemNaturally(center, item.get());
            amount -= item.get().getAmount();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        explode(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        explode(event.blockList());
    }

    /** Una explosión rompe la mena como tal (su ítem, sin Fortuna) en vez de soltar un bloque musical. */
    private void explode(List<Block> blocks) {
        for (Iterator<Block> it = blocks.iterator(); it.hasNext(); ) {
            Block block = it.next();
            var hit = service.at(block);
            if (hit.isPresent()) {
                it.remove();
                block.setType(Material.AIR, false);
                drop(block, hit.get().ore(), hit.get().ore().dropMin());
            }
        }
    }

    // ---------------------------------------------------------------- que siga siendo mena

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (service.at(event.getBlock()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onNote(NotePlayEvent event) {
        if (service.at(event.getBlock()).isPresent()) {
            event.setCancelled(true);
        }
    }

    /** Clic derecho afina un bloque musical: en una mena la convertiría en otra. */
    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                && service.at(event.getClickedBlock()).isPresent()) {
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {

        Block block = event.getBlockPlaced();
        if (block.getType() != Material.NOTE_BLOCK || !(block.getBlockData() instanceof NoteBlock note)) {
            return;
        }

        if (service.isReserved(note.getInstrument())) {
            note.setInstrument(Instrument.PIANO);
            block.setBlockData(note, false);
        }
    }

}
