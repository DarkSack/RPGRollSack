package com.sack.rpgroll.machines.quarry;

import com.sack.rpgroll.common.block.MachineBreakEvent;
import com.sack.rpgroll.common.menu.PaymentItems;
import com.sack.rpgroll.machines.quarry.Quarry.Status;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Numeric;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Unlock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hace trabajar a las canteras, un poco cada tick.
 * <p>
 * Cada cantera junta "crédito" a su velocidad (bloques por segundo) y gasta uno por bloque
 * picado; además hay un tope de bloques por tick entre todas, y uno de bloques mirados por
 * cantera y tick (las capas de aire no cuestan crédito pero tampoco son gratis).
 * <p>
 * Solo trabaja con su dueño conectado (el bloque se rompe en su nombre, con un
 * {@link MachineBreakEvent}: protecciones y CoreProtect lo ven), con su chunk y el del bloque
 * cargados (nunca carga chunks) y con un cofre pegado con sitio. Lo que no cabe se queda en la
 * cantera y la para hasta que se vacía el cofre.
 * <p>
 * Los drops se calculan con un pico de netherita virtual con su fortuna o toque de seda. Si un
 * plugin suelta sus propios ítems al romper (las menas de RPGRoll-Items los tiran al suelo), se
 * recogen al vuelo: mientras se rompe un bloque, todo ítem que aparezca a su lado va a la cantera.
 */
public class QuarryWorker implements Listener, Runnable {

    private static final Map<Material, Optional<ItemStack>> SMELT_CACHE = new EnumMap<>(Material.class);

    private final QuarryService service;
    private List<CookingRecipe<?>> smelting = List.of();

    /** El bloque que se está rompiendo ahora mismo: sus ítems van a {@link #captured}. */
    private Location capturing;
    private final List<ItemStack> captured = new ArrayList<>();

    public QuarryWorker(QuarryService service) {
        this.service = service;
        reloadRecipes();
    }

    /** Las recetas de horno del servidor (vanilla y de plugins) para el autofundido. */
    public void reloadRecipes() {
        List<CookingRecipe<?>> found = new ArrayList<>();
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            if (iterator.next() instanceof FurnaceRecipe recipe) {
                found.add(recipe);
            }
        }
        smelting = List.copyOf(found);
        SMELT_CACHE.clear();
    }

    @Override
    public void run() {

        QuarrySettings settings = service.settings();
        if (!settings.enabled()) {
            return;
        }
        int budget = settings.blocksPerTick();
        for (Quarry quarry : List.copyOf(service.store().all())) {
            if (budget <= 0) {
                break;
            }
            budget -= work(quarry, budget, settings);
        }
    }

    /** Un tick de una cantera. Devuelve cuántos bloques picó. */
    private int work(Quarry quarry, int budget, QuarrySettings settings) {

        World world = Bukkit.getWorld(quarry.world());
        if (world == null || !world.isChunkLoaded(quarry.x() >> 4, quarry.z() >> 4)) {
            return 0;
        }
        Block block = world.getBlockAt(quarry.x(), quarry.y(), quarry.z());
        if (block.getType() != settings.block()) {
            // Ya no está (la quitó algo que no avisa: WorldEdit, un comando...). Se olvida.
            service.spill(quarry, block.getLocation().add(0.5, 0.5, 0.5));
            service.removeFrame(block);
            service.store().remove(quarry);
            return 0;
        }
        if (!quarry.enabled()) {
            quarry.status(Status.OFF);
            return 0;
        }
        if (quarry.finished()) {
            quarry.status(Status.FINISHED);
            return 0;
        }
        Player owner = Bukkit.getPlayer(quarry.owner());
        if (owner == null) {
            quarry.status(Status.NO_OWNER);
            return 0;
        }
        quarry.ownerName(owner.getName());
        Optional<Inventory> output = service.output(block);
        if (output.isEmpty()) {
            quarry.status(Status.NO_CHEST);
            return 0;
        }
        if (!flush(quarry, output.get())) {
            quarry.status(Status.FULL);
            return 0;
        }

        double speed = settings.track(Numeric.SPEED).value(quarry.level(Numeric.SPEED));
        quarry.credit(Math.min(quarry.credit() + speed / 20.0, Math.max(1.0, speed)));

        int side = service.side(quarry);
        int minHeight = world.getMinHeight();
        int mined = 0;
        int scans = 0;
        quarry.status(Status.MINING);

        while (quarry.credit() >= 1.0 && mined < budget && scans < settings.scansPerTick()) {
            scans++;
            int[] column = Quarry.column(quarry.x(), quarry.z(), side, quarry.cursorIndex());
            if (quarry.cursorY() < minHeight) {
                quarry.finished(true);
                quarry.status(Status.FINISHED);
                service.store().dirty();
                break;
            }
            if (!world.isChunkLoaded(column[0] >> 4, column[1] >> 4)) {
                quarry.status(Status.WAITING_CHUNK);
                break;
            }
            Block target = world.getBlockAt(column[0], quarry.cursorY(), column[1]);
            if (!skip(target, settings)) {
                mine(quarry, target, owner, settings);
                quarry.credit(quarry.credit() - 1.0);
                mined++;
            }
            int[] next = Quarry.advance(quarry.cursorY(), quarry.cursorIndex(), side, minHeight);
            if (next == null) {
                quarry.cursor(minHeight - 1, 0);
                quarry.finished(true);
                quarry.status(Status.FINISHED);
            } else {
                quarry.cursor(next[0], next[1]);
            }
            if (!quarry.buffer().isEmpty() && !flush(quarry, output.get())) {
                quarry.status(Status.FULL);
                break;
            }
        }
        if (scans > 0) {
            service.store().dirty();
        }
        return mined;
    }

    private static boolean skip(Block block, QuarrySettings settings) {
        Material type = block.getType();
        return type.isAir() || block.isLiquid() || type.getHardness() < 0 || settings.skip().contains(type)
                || type == settings.block() || block.getState(false) instanceof TileState;
    }

    private void mine(Quarry quarry, Block target, Player owner, QuarrySettings settings) {

        ItemStack tool = new ItemStack(Material.NETHERITE_PICKAXE);
        if (quarry.active(Unlock.SILK_TOUCH)) {
            tool.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
        } else {
            int fortune = (int) settings.track(Numeric.FORTUNE).value(quarry.level(Numeric.FORTUNE));
            if (fortune > 0) {
                tool.addUnsafeEnchantment(Enchantment.FORTUNE, fortune);
            }
        }
        int tier = (int) settings.track(Numeric.TIER).value(quarry.level(Numeric.TIER));

        List<ItemStack> drops = new ArrayList<>();
        capturing = target.getLocation().add(0.5, 0.5, 0.5);
        captured.clear();
        try {
            MachineBreakEvent event = new MachineBreakEvent(target, owner, tool, tier);
            if (!event.callEvent()) {
                return;
            }
            if (event.isDropItems()) {
                drops.addAll(target.getDrops(tool, owner));
            }
            target.setType(Material.AIR, true);
            drops.addAll(captured);
        } finally {
            capturing = null;
            captured.clear();
        }

        for (ItemStack drop : drops) {
            ItemStack processed = process(quarry, drop, settings);
            if (processed != null) {
                quarry.buffer().add(processed);
            }
        }
    }

    /** Autofundido y filtro de basura. null = se descarta. */
    private ItemStack process(Quarry quarry, ItemStack drop, QuarrySettings settings) {

        if (drop == null || drop.getType().isAir() || drop.getAmount() <= 0) {
            return null;
        }
        if (quarry.active(Unlock.FILTER) && settings.junk().contains(drop.getType()) && PaymentItems.isPlain(drop)) {
            return null;
        }
        if (quarry.active(Unlock.SMELT)) {
            Optional<ItemStack> smelted = smelt(drop);
            if (smelted.isPresent()) {
                ItemStack result = smelted.get().clone();
                result.setAmount(result.getAmount() * drop.getAmount());
                return result;
            }
        }
        return drop;
    }

    private Optional<ItemStack> smelt(ItemStack drop) {
        if (PaymentItems.isPlain(drop)) {
            return SMELT_CACHE.computeIfAbsent(drop.getType(), type -> find(new ItemStack(type)));
        }
        return find(drop);
    }

    private Optional<ItemStack> find(ItemStack input) {
        for (CookingRecipe<?> recipe : smelting) {
            if (recipe.getInputChoice().test(input)) {
                return Optional.of(recipe.getResult());
            }
        }
        return Optional.empty();
    }

    /** Mete lo guardado en el cofre. true si quedó todo dentro. */
    private boolean flush(Quarry quarry, Inventory output) {
        if (quarry.buffer().isEmpty()) {
            return true;
        }
        List<ItemStack> left = new ArrayList<>();
        for (ItemStack item : quarry.buffer()) {
            left.addAll(output.addItem(item.clone()).values());
        }
        quarry.buffer().clear();
        quarry.buffer().addAll(left);
        service.store().dirty();
        return left.isEmpty();
    }

    /** Los ítems que suelta el bloque que se está rompiendo (o lo que se cae con él) van a la cantera. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (capturing == null || !event.getLocation().getWorld().equals(capturing.getWorld())
                || event.getLocation().distanceSquared(capturing) > 4.0) {
            return;
        }
        captured.add(event.getEntity().getItemStack().clone());
        event.setCancelled(true);
    }
}
