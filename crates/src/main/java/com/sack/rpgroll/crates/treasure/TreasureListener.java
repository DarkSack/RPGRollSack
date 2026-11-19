package com.sack.rpgroll.crates.treasure;

import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Los tesoros en juego: leer el mapa, la pista, cavar hasta el tesoro y abrir su cofre.
 */
public class TreasureListener implements Listener {

    /** Puntos que se prueban al leer un mapa antes de rendirse. */
    private static final int TRIES = 16;
    /** Agua por encima del fondo que se acepta (un tesoro bajo el mar profundo no se puede cavar). */
    private static final int MAX_WATER = 6;

    private final Plugin plugin;
    private final LangManager lang;
    private final TreasureManager manager;
    private final TreasureMaps maps;
    private final TreasureChests chests;
    private final int markRadius;
    private final Set<UUID> reading = new HashSet<>();
    private BukkitTask hintTask;

    public TreasureListener(Plugin plugin, LangManager lang, TreasureManager manager, TreasureMaps maps,
                            TreasureChests chests, int markRadius) {
        this.plugin = plugin;
        this.lang = lang;
        this.manager = manager;
        this.maps = maps;
        this.chests = chests;
        this.markRadius = markRadius;
    }

    public void startHints(long intervalTicks) {
        hintTask = Bukkit.getScheduler().runTaskTimer(plugin, this::hints, intervalTicks, Math.max(5, intervalTicks));
    }

    /** Al apagar: se cierran los cofres abiertos (su contenido cae al suelo) y se para la pista. */
    public void shutdown() {
        if (hintTask != null) {
            hintTask.cancel();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof TreasureChests.Holder) {
                player.closeInventory();
            }
        }
    }

    // ---------------------------------------------------------------- leer el mapa

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {

        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        // Clic en un cofre de tesoro: se abre (vaya lo que vaya en la mano).
        Block clicked = event.getClickedBlock();
        if (clicked != null && clicked.getType() == Material.BARRIER) {
            Optional<ItemDisplay> display = chests.displayAt(clicked);
            if (display.isPresent()) {
                event.setCancelled(true);
                chests.open(event.getPlayer(), clicked, display.get());
                return;
            }
        }

        ItemStack item = event.getItem();
        Optional<String> id = maps.treasureId(item);
        if (id.isEmpty()) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        Optional<Treasure> treasure = manager.get(id.get());
        if (treasure.isEmpty()) {
            lang.send(player, "treasure.unknown");
            return;
        }
        Optional<TreasureMaps.Target> target = maps.target(item);
        if (target.isPresent()) {
            sendHint(player, target.get(), true);
            return;
        }
        locate(player, item, treasure.get());
    }

    private void locate(Player player, ItemStack map, Treasure treasure) {

        World world = player.getWorld();
        Treasure.Placement placement = treasure.placement();
        boolean worldAllowed = placement.worlds().isEmpty()
                ? world.getEnvironment() == World.Environment.NORMAL
                : placement.worlds().stream().anyMatch(name -> name.equalsIgnoreCase(world.getName()));
        if (!worldAllowed) {
            lang.send(player, "treasure.wrong_world");
            return;
        }
        if (!reading.add(player.getUniqueId())) {
            return;
        }
        lang.send(player, "treasure.reading");
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.9f);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location origin = player.getLocation();
        List<int[]> candidates = new ArrayList<>();
        for (int i = 0; i < TRIES; i++) {
            TreasureSpot.pick(origin.getBlockX(), origin.getBlockZ(), placement, random, (x, z) -> true, 1)
                    .ifPresent(candidates::add);
        }
        tryCandidate(player, map, treasure, world, candidates, 0);
    }

    /** Carga cada candidato sin bloquear el servidor y se queda con el primero que vale. */
    private void tryCandidate(Player player, ItemStack map, Treasure treasure, World world, List<int[]> candidates,
                              int index) {

        if (index >= candidates.size()) {
            reading.remove(player.getUniqueId());
            if (player.isOnline()) {
                lang.send(player, "treasure.no_spot");
            }
            return;
        }

        int x = candidates.get(index)[0];
        int z = candidates.get(index)[1];
        CompletableFuture<?> chunk = world.getChunkAtAsync(x >> 4, z >> 4);
        chunk.whenComplete((loaded, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            Optional<TreasureMaps.Target> target = error == null ? check(world, treasure, x, z) : Optional.empty();
            if (target.isEmpty()) {
                tryCandidate(player, map, treasure, world, candidates, index + 1);
                return;
            }
            reading.remove(player.getUniqueId());
            if (!player.isOnline()) {
                return;
            }
            // El jugador pudo mover el mapa mientras tanto: se busca el mismo ítem en su inventario.
            ItemStack current = findUnreadMap(player, treasure.id());
            if (current == null) {
                return;
            }
            maps.setTarget(current, target.get());
            sendHint(player, target.get(), true);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.6f);
        }));
    }

    private Optional<TreasureMaps.Target> check(World world, Treasure treasure, int x, int z) {

        int floor = world.getHighestBlockYAt(x, z, HeightMap.OCEAN_FLOOR);
        int top = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING);
        if (top - floor > MAX_WATER) {
            return Optional.empty();
        }
        if (!treasure.placement().biomes().isEmpty()) {
            String biome = world.getBiome(x, floor, z).getKey().toString().toLowerCase(Locale.ROOT);
            if (!treasure.placement().biomes().contains(biome)) {
                return Optional.empty();
            }
        }
        Material surface = world.getBlockAt(x, floor, z).getType();
        if (surface == Material.LAVA || surface == Material.BEDROCK) {
            return Optional.empty();
        }
        int y = floor - treasure.placement().depth();
        if (y <= world.getMinHeight() + 1) {
            return Optional.empty();
        }
        return Optional.of(new TreasureMaps.Target(world.getName(), x, y, z));
    }

    private ItemStack findUnreadMap(Player player, String treasureId) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (isUnread(hand, treasureId)) {
            return hand;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (isUnread(item, treasureId)) {
                return item;
            }
        }
        return null;
    }

    private boolean isUnread(ItemStack item, String treasureId) {
        return maps.treasureId(item).filter(treasureId::equals).isPresent() && maps.target(item).isEmpty();
    }

    // ---------------------------------------------------------------- la pista

    private void hints() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (maps.treasureId(hand).isEmpty()) {
                continue;
            }
            maps.target(hand).ifPresent(target -> sendHint(player, target, false));
        }
    }

    private void sendHint(Player player, TreasureMaps.Target target, boolean chat) {

        if (!player.getWorld().getName().equals(target.world())) {
            player.sendActionBar(lang.component("treasure.other_world", "world", target.world()));
            return;
        }

        Location here = player.getLocation();
        int dx = target.x() - here.getBlockX();
        int dz = target.z() - here.getBlockZ();
        int distance = TreasureHint.distance(dx, dz);
        World world = player.getWorld();

        if (distance <= markRadius) {
            int surface = world.getHighestBlockYAt(target.x(), target.z(), HeightMap.OCEAN_FLOOR);
            int depth = Math.max(0, surface - target.y());
            player.sendActionBar(lang.component("treasure.hint_here", "depth", depth));
            Location mark = new Location(world, target.x() + 0.5, surface + 1.15, target.z() + 0.5);
            player.spawnParticle(Particle.WAX_OFF, mark, 12, 0.35, 0.05, 0.35, 0);
            player.spawnParticle(Particle.END_ROD, mark.clone().add(0, 0.6, 0), 2, 0.05, 0.4, 0.05, 0);
            return;
        }

        String direction = lang.raw("treasure.dir." + TreasureHint.direction(dx, dz));
        if (chat) {
            lang.send(player, "treasure.located", "distance", distance, "direction", direction);
        }
        player.sendActionBar(lang.component("treasure.hint", "distance", distance, "direction", direction));
    }

    // ---------------------------------------------------------------- cavar

    /** MONITOR e ignoreCancelled: si una protección no deja romper el bloque, no sale nada. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {

        Block block = event.getBlock();

        // Un admin en creativo que rompe la barrera de un cofre lo quita entero.
        if (block.getType() == Material.BARRIER) {
            chests.displayAt(block).ifPresent(display -> display.remove());
            return;
        }

        Player player = event.getPlayer();
        for (ItemStack item : player.getInventory().getContents()) {
            Optional<TreasureMaps.Target> target = maps.target(item);
            if (target.isEmpty() || !reached(block, target.get())) {
                continue;
            }
            Optional<Treasure> treasure = maps.treasureId(item).flatMap(manager::get);
            if (treasure.isEmpty()) {
                continue;
            }
            item.setAmount(item.getAmount() - 1);
            Treasure found = treasure.get();
            // Al tick siguiente el bloque ya está roto y el cofre ocupa su sitio.
            Bukkit.getScheduler().runTask(plugin, () -> chests.spawn(player, found, block));
            return;
        }
    }

    /** Vale cualquier bloque de la columna del tesoro a su profundidad o por debajo (por si allí hay una cueva). */
    static boolean reached(Block block, TreasureMaps.Target target) {
        return block.getWorld().getName().equals(target.world()) && block.getX() == target.x()
                && block.getZ() == target.z() && block.getY() <= target.y() && block.getY() >= target.y() - 6;
    }

    // ---------------------------------------------------------------- cerrar el cofre

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof TreasureChests.Holder holder) {
            HumanEntity who = event.getPlayer();
            chests.close(holder);
            if (who instanceof Player player) {
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1f, 0.8f);
            }
        }
    }

}
