package com.sack.rpgroll.enchantments.effect;

import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Efectos de herramienta que rompen o cambian más de un bloque: minero de
 * vetas, leñador, martillo y azada amplia.
 * <p>
 * Cada bloque extra se rompe con {@link Player#breakBlock}, que lanza su
 * propio {@code BlockBreakEvent}: así lo respetan las protecciones
 * (GriefPrevention, WorldGuard), lo registra CoreProtect, gasta la
 * herramienta y aplica Fortuna como un golpe normal. Mientras un jugador está
 * rompiendo en área queda marcado en {@link #busy}, para que esos eventos
 * anidados no vuelvan a disparar otra área (sí disparan lo demás, como la
 * autofundición).
 */
public class BlockEffects {

    private static final BlockFace[] SIDES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    /** Usos que se le dejan siempre a la herramienta: un área nunca la rompe. */
    private static final int TOOL_RESERVE = 2;

    /** Hojas naturales que tiene que tocar un tronco para contar como árbol (y no como casa). */
    private static final int MIN_TREE_LEAVES = 4;

    private static final int MAX_LEAVES = 400;

    private static final Map<Material, Material> SEEDS = Map.of(
            Material.WHEAT, Material.WHEAT_SEEDS,
            Material.CARROTS, Material.CARROT,
            Material.POTATOES, Material.POTATO,
            Material.BEETROOTS, Material.BEETROOT_SEEDS,
            Material.NETHER_WART, Material.NETHER_WART);

    private final Plugin plugin;
    private final Set<UUID> busy = new HashSet<>();

    public BlockEffects(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isBusy(Player player) {
        return busy.contains(player.getUniqueId());
    }

    // ------------------------------------------------------------------ minero de vetas

    public void veinMine(EffectContext context, int max) {

        if (!(context.event() instanceof BlockBreakEvent event) || !context.inMainHand() || isBusy(context.player())) {
            return;
        }

        Block origin = event.getBlock();
        ItemStack tool = context.player().getInventory().getItemInMainHand();

        if (!isOre(origin) || !origin.isPreferredTool(tool)) {
            return;
        }

        String key = oreKey(origin);
        breakAll(context.player(), collect(origin, b -> isOre(b) && key.equals(oreKey(b)), max, b -> true));
    }

    /**
     * Mena vanilla, o una de RPGRoll-Items: un bloque musical con instrumento
     * de zombi, que no aparece en el mundo de forma natural.
     */
    static boolean isOre(Block block) {

        Material type = block.getType();

        if (type == Material.NOTE_BLOCK) {
            return block.getBlockData() instanceof NoteBlock note && note.getInstrument() == Instrument.ZOMBIE;
        }

        return type.name().endsWith("_ORE") || type == Material.ANCIENT_DEBRIS;
    }

    /** Las variantes de pizarra cuentan como la misma veta; las menas propias, por su estado exacto. */
    static String oreKey(Block block) {

        if (block.getType() == Material.NOTE_BLOCK) {
            return block.getBlockData().getAsString();
        }

        return block.getType().name().replace("DEEPSLATE_", "");
    }

    // ------------------------------------------------------------------ leñador

    public void treeFell(EffectContext context, int max, boolean leaves) {

        if (!(context.event() instanceof BlockBreakEvent event) || !context.inMainHand() || isBusy(context.player())) {
            return;
        }

        Block origin = event.getBlock();

        if (!Tag.LOGS.isTagged(origin.getType())) {
            return;
        }

        // Solo hacia arriba y cerca del tronco: talar no debe comerse el suelo de madera de al lado.
        List<Block> logs = collect(origin, b -> Tag.LOGS.isTagged(b.getType()), max,
                b -> b.getY() >= origin.getY()
                        && Math.abs(b.getX() - origin.getX()) <= 6 && Math.abs(b.getZ() - origin.getZ()) <= 6);

        List<Block> trunk = new ArrayList<>(logs);
        trunk.add(origin);

        if (countNaturalLeaves(trunk) < MIN_TREE_LEAVES) {
            return;
        }

        logs.sort(Comparator.comparingInt(Block::getY));
        breakAll(context.player(), logs);

        if (leaves) {
            decayLeaves(trunk);
        }
    }

    private int countNaturalLeaves(List<Block> trunk) {

        Set<Block> seen = new HashSet<>();

        for (Block log : trunk) {
            for (BlockFace face : SIDES) {
                Block side = log.getRelative(face);
                if (isNaturalLeaf(side)) {
                    seen.add(side);
                }
            }
        }

        return seen.size();
    }

    private static boolean isNaturalLeaf(Block block) {

        if (block.getBlockData() instanceof Leaves leaves) {
            return !leaves.isPersistent();
        }

        // Los "árboles" del Nether: verrugas y luces de hongo.
        return Tag.WART_BLOCKS.isTagged(block.getType()) || block.getType() == Material.SHROOMLIGHT;
    }

    /**
     * Las hojas que se quedan sin tronco se deshacen en vez de esperar al
     * decaimiento aleatorio. La distancia al tronco la recalcula el juego a lo
     * largo de varios ticks, así que se revisan en unas cuantas pasadas.
     */
    private void decayLeaves(List<Block> trunk) {

        Set<Block> leaves = new LinkedHashSet<>();
        Deque<Block> queue = new ArrayDeque<>(trunk);
        Set<Block> visited = new HashSet<>(trunk);

        while (!queue.isEmpty() && leaves.size() < MAX_LEAVES) {
            Block current = queue.poll();
            for (BlockFace face : SIDES) {
                Block next = current.getRelative(face);
                if (visited.add(next) && next.getBlockData() instanceof Leaves leaf && !leaf.isPersistent()
                        && next.getLocation().distanceSquared(trunk.get(0).getLocation()) <= 100) {
                    leaves.add(next);
                    queue.add(next);
                }
            }
        }

        if (leaves.isEmpty()) {
            return;
        }

        new BukkitRunnable() {
            int passes = 0;

            @Override
            public void run() {

                leaves.removeIf(block -> {
                    if (!(block.getBlockData() instanceof Leaves leaf) || leaf.isPersistent()) {
                        return true;
                    }
                    if (leaf.getDistance() < leaf.getMaximumDistance()) {
                        return false;
                    }
                    LeavesDecayEvent decay = new LeavesDecayEvent(block);
                    Bukkit.getPluginManager().callEvent(decay);
                    if (!decay.isCancelled()) {
                        block.breakNaturally();
                    }
                    return true;
                });

                if (leaves.isEmpty() || ++passes >= 10) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    // ------------------------------------------------------------------ martillo

    public void areaMine(EffectContext context, int radius) {

        if (!(context.event() instanceof BlockBreakEvent event) || !context.inMainHand() || isBusy(context.player())) {
            return;
        }

        Player player = context.player();
        Block origin = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!origin.isPreferredTool(tool)) {
            return;
        }

        BlockFace face = hitFace(player, origin);
        float hardness = origin.getType().getHardness();
        List<Block> blocks = new ArrayList<>();

        for (int u = -radius; u <= radius; u++) {
            for (int v = -radius; v <= radius; v++) {

                if (u == 0 && v == 0) {
                    continue;
                }

                Block block = switch (face) {
                    case UP, DOWN -> origin.getRelative(u, 0, v);
                    case NORTH, SOUTH -> origin.getRelative(u, v, 0);
                    default -> origin.getRelative(0, v, u);
                };

                Material type = block.getType();

                // Nada más duro que lo golpeado (no se lleva obsidiana con la piedra),
                // nada irrompible y nada con contenido (cofres, hornos...).
                if (type.isAir() || block.isLiquid() || type.getHardness() < 0
                        || type.getHardness() > hardness + 1.5f
                        || !block.isPreferredTool(tool) || block.getState() instanceof TileState) {
                    continue;
                }

                blocks.add(block);
            }
        }

        breakAll(player, blocks);
    }

    private static BlockFace hitFace(Player player, Block origin) {

        RayTraceResult hit = player.rayTraceBlocks(6);

        if (hit != null && origin.equals(hit.getHitBlock()) && hit.getHitBlockFace() != null) {
            return hit.getHitBlockFace();
        }

        float pitch = player.getLocation().getPitch();
        return Math.abs(pitch) > 45 ? BlockFace.UP : player.getFacing();
    }

    // ------------------------------------------------------------------ azada amplia

    public void tillArea(EffectContext context, int radius, boolean replant) {

        if (!context.inMainHand() || isBusy(context.player())
                || context.item() == null || !context.item().getType().name().endsWith("_HOE")) {
            return;
        }

        if (context.event() instanceof PlayerInteractEvent interact) {
            till(context.player(), interact, radius);
        } else if (context.event() instanceof BlockBreakEvent breakEvent) {
            harvest(context.player(), breakEvent.getBlock(), radius, replant);
        }
    }

    private void till(Player player, PlayerInteractEvent event, int radius) {

        Block origin = event.getClickedBlock();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || origin == null || event.getBlockFace() == BlockFace.DOWN || tilled(origin.getType()) == null
                || !origin.getRelative(BlockFace.UP).getType().isAir()) {
            return;
        }

        int done = 0;

        // El bloque golpeado lo labra el propio juego; aquí solo los de alrededor.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {

                if (dx == 0 && dz == 0) {
                    continue;
                }

                if (toolNearlyBroken(player.getInventory().getItemInMainHand())) {
                    return;
                }

                Block block = origin.getRelative(dx, 0, dz);
                Material result = tilled(block.getType());

                if (result == null || !block.getRelative(BlockFace.UP).getType().isAir()) {
                    continue;
                }

                if (placeAs(player, block, result)) {
                    player.damageItemStack(EquipmentSlot.HAND, 1);
                    done++;
                }
            }
        }

        if (done > 0) {
            origin.getWorld().playSound(origin.getLocation(), Sound.ITEM_HOE_TILL, 1f, 0.9f);
        }
    }

    private static Material tilled(Material type) {
        return switch (type) {
            case GRASS_BLOCK, DIRT, DIRT_PATH -> Material.FARMLAND;
            case COARSE_DIRT -> Material.DIRT;
            default -> null;
        };
    }

    /**
     * Cambia el bloque y lo anuncia como una colocación del jugador; si alguna
     * protección la cancela, se deshace. Es lo mismo que hace el servidor al
     * colocar un bloque normal.
     */
    private static boolean placeAs(Player player, Block block, Material type) {

        BlockState before = block.getState();
        block.setType(type);

        BlockPlaceEvent event = new BlockPlaceEvent(block, before, block.getRelative(BlockFace.DOWN),
                player.getInventory().getItemInMainHand(), player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled() || !event.canBuild()) {
            before.update(true, false);
            return false;
        }

        return true;
    }

    private void harvest(Player player, Block origin, int radius, boolean replant) {

        if (!isMatureCrop(origin)) {
            return;
        }

        List<Block> crops = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Block block = origin.getRelative(dx, 0, dz);
                if ((dx != 0 || dz != 0) && isMatureCrop(block)) {
                    crops.add(block);
                }
            }
        }

        List<Block> spots = new ArrayList<>(crops);
        spots.add(origin);
        List<Material> types = spots.stream().map(Block::getType).toList();

        breakAll(player, crops);

        if (!replant) {
            return;
        }

        // Un tick después: el juego ya rompió el del centro y soltó las semillas.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int i = 0; i < spots.size(); i++) {
                Block spot = spots.get(i);
                Material crop = types.get(i);
                if (spot.getType().isAir() && takeSeed(player, spot, SEEDS.get(crop))) {
                    spot.setType(crop);
                }
            }
        }, 1L);
    }

    private static boolean isMatureCrop(Block block) {
        return SEEDS.containsKey(block.getType())
                && block.getBlockData() instanceof Ageable age && age.getAge() == age.getMaximumAge();
    }

    /** Una semilla del inventario o, si no hay, de lo que acaba de caer al lado. */
    private static boolean takeSeed(Player player, Block spot, Material seed) {

        if (player.getInventory().removeItem(new ItemStack(seed, 1)).isEmpty()) {
            return true;
        }

        for (var entity : spot.getWorld().getNearbyEntities(spot.getLocation().add(0.5, 0.5, 0.5), 1.5, 1.5, 1.5)) {
            if (entity instanceof Item item && item.getItemStack().getType() == seed) {
                ItemStack stack = item.getItemStack();
                if (stack.getAmount() <= 1) {
                    item.remove();
                } else {
                    stack.setAmount(stack.getAmount() - 1);
                    item.setItemStack(stack);
                }
                return true;
            }
        }

        return false;
    }

    // ------------------------------------------------------------------ común

    /**
     * Los bloques conectados al origen (incluidas diagonales) que cumplen
     * {@code match}, sin el origen y como mucho {@code max}.
     */
    private static List<Block> collect(Block origin, Predicate<Block> match, int max, Predicate<Block> within) {

        List<Block> found = new ArrayList<>();
        Set<Block> visited = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();

        visited.add(origin);
        queue.add(origin);

        while (!queue.isEmpty() && found.size() < max) {

            Block current = queue.poll();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {

                        Block next = current.getRelative(dx, dy, dz);

                        if (!visited.add(next) || !within.test(next) || !match.test(next)) {
                            continue;
                        }

                        found.add(next);
                        queue.add(next);

                        if (found.size() >= max) {
                            return found;
                        }
                    }
                }
            }
        }

        return found;
    }

    private int breakAll(Player player, List<Block> blocks) {

        if (blocks.isEmpty()) {
            return 0;
        }

        UUID id = player.getUniqueId();
        busy.add(id);
        int broken = 0;

        try {
            for (Block block : blocks) {

                if (toolNearlyBroken(player.getInventory().getItemInMainHand())) {
                    break;
                }

                if (!block.getType().isAir() && player.breakBlock(block)) {
                    broken++;
                }
            }
        } finally {
            busy.remove(id);
        }

        return broken;
    }

    static boolean toolNearlyBroken(ItemStack tool) {

        if (tool == null || tool.getType().isAir()) {
            return true;
        }

        if (!(tool.getItemMeta() instanceof Damageable damageable) || damageable.isUnbreakable()) {
            return false;
        }

        int max = damageable.hasMaxDamage() ? damageable.getMaxDamage() : tool.getType().getMaxDurability();
        return max > 0 && max - damageable.getDamage() <= TOOL_RESERVE;
    }

}
