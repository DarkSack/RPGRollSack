package com.sack.rpgroll.machines.area;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.machines.quarry.Quarry;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Leñadora: recorre un cuadrado centrado en ella, a su altura, buscando la base de un árbol natural
 * (troncos con hojas no puestas a mano). Lo tala entero (con tope) y planta un brote de su tipo; con
 * la mejora de hojas, rompe también las hojas para sacar brotes, manzanas y palos.
 */
public class LumberKind extends AreaKind {

    private static final BlockFace[] LEAF_SIDES = {BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
            BlockFace.WEST};

    private int maxLogs;
    private int maxLeaves;
    private int height;

    @Override
    public String id() {
        return "lumber";
    }

    @Override
    public String file() {
        return "lumberjacks.yml";
    }

    @Override
    public String art() {
        return "lenadora";
    }

    @Override
    public Material defaultBlock() {
        return Material.FLETCHING_TABLE;
    }

    @Override
    public List<String> upgrades() {
        return List.of("speed", "area", "leaves");
    }

    @Override
    protected void loadExtra(FileConfiguration config, Consumer<String> warn) {
        maxLogs = Math.max(1, config.getInt("max-logs", 128));
        maxLeaves = Math.max(0, config.getInt("max-leaves", 256));
        height = Math.max(4, config.getInt("height", 32));
    }

    @Override
    public int work(AreaMachine machine, Block block, Player owner, int budget, AreaContext context) {
        machine.gauge(Math.min(1.0, machine.credit()));
        if (machine.credit() < 1.0) {
            machine.status(AreaStatus.WORKING);
            return 0;
        }
        World world = block.getWorld();
        int side = side(machine);
        int minX = Quarry.min(machine.x(), side);
        int minZ = Quarry.min(machine.z(), side);
        Predicate<int[]> inside = p -> p[0] >= minX && p[0] < minX + side && p[2] >= minZ && p[2] < minZ + side
                && p[1] >= machine.y() && p[1] <= machine.y() + height
                && world.isChunkLoaded(p[0] >> 4, p[2] >> 4);
        Predicate<int[]> isLog = p -> log(world.getBlockAt(p[0], p[1], p[2]).getType());
        int cells = side * side;
        for (int scans = 0; scans < settings().scansPerTick(); scans++) {
            int index = Math.floorMod(machine.cursor(), cells);
            machine.cursor(index + 1);
            int[] column = Quarry.column(machine.x(), machine.z(), side, index);
            int[] base = {column[0], machine.y(), column[1]};
            if (!inside.test(base)) {
                machine.status(AreaStatus.WAITING_CHUNK);
                return 0;
            }
            Block baseBlock = world.getBlockAt(base[0], base[1], base[2]);
            if (!log(baseBlock.getType()) || !Tag.DIRT.isTagged(baseBlock.getRelative(0, -1, 0).getType())) {
                continue;
            }
            List<int[]> logs = TreeScan.collect(base, isLog, inside, maxLogs);
            if (!natural(world, logs)) {
                continue;
            }
            machine.status(AreaStatus.WORKING);
            machine.credit(machine.credit() - 1.0);
            return chop(machine, world, baseBlock, logs, owner, context, inside);
        }
        machine.status(AreaStatus.IDLE);
        return 0;
    }

    /** Troncos de árbol: los pelados los pone un jugador. */
    private static boolean log(Material type) {
        return Tag.LOGS.isTagged(type) && !type.name().startsWith("STRIPPED_");
    }

    private static boolean naturalLeaves(Block block) {
        return block.getBlockData() instanceof Leaves leaves && !leaves.isPersistent();
    }

    /** Un árbol de verdad tiene hojas que no puso nadie pegadas a algún tronco. */
    private static boolean natural(World world, List<int[]> logs) {
        for (int[] p : logs) {
            Block log = world.getBlockAt(p[0], p[1], p[2]);
            for (BlockFace face : LEAF_SIDES) {
                if (naturalLeaves(log.getRelative(face))) {
                    return true;
                }
            }
        }
        return false;
    }

    private int chop(AreaMachine machine, World world, Block base, List<int[]> logs, Player owner, AreaContext context,
            Predicate<int[]> inside) {
        Material sapling = TreeScan.sapling(base.getType());
        int broken = 0;
        if (settings().value("leaves", machine) > 0) {
            // Las hojas antes que los troncos: luego ya no están conectadas.
            Predicate<int[]> leaf = p -> naturalLeaves(world.getBlockAt(p[0], p[1], p[2]));
            Set<Long> done = new HashSet<>();
            int left = maxLeaves;
            for (int[] p : logs) {
                for (int[] start : neighbours(p)) {
                    if (left <= 0) {
                        break;
                    }
                    if (!done.add(TreeScan.pack(start)) || !leaf.test(start) || !inside.test(start)) {
                        continue;
                    }
                    for (int[] l : TreeScan.collect(start, leaf, inside, left)) {
                        broken += take(machine, world.getBlockAt(l[0], l[1], l[2]), owner, new ItemStack(Material.AIR), context);
                        left--;
                    }
                }
            }
        }
        ItemStack axe = new ItemStack(Material.NETHERITE_AXE);
        for (int[] p : logs) {
            broken += take(machine, world.getBlockAt(p[0], p[1], p[2]), owner, axe, context);
        }
        if (sapling != null && base.getType().isAir() && Tag.DIRT.isTagged(base.getRelative(0, -1, 0).getType())) {
            base.setType(sapling, true);
        }
        return Math.max(1, broken);
    }

    private static int[][] neighbours(int[] p) {
        return new int[][] {{p[0], p[1] + 1, p[2]}, {p[0] + 1, p[1], p[2]}, {p[0] - 1, p[1], p[2]},
                {p[0], p[1], p[2] + 1}, {p[0], p[1], p[2] - 1}};
    }

    private static int take(AreaMachine machine, Block block, Player owner, ItemStack tool, AreaContext context) {
        List<ItemStack> drops = context.breakAs(owner, block, tool);
        if (drops == null) {
            return 0;
        }
        machine.buffer().addAll(drops);
        return 1;
    }

    @Override
    public String format(String upgrade, double value, LangManager lang) {
        if (upgrade.equals("leaves")) {
            return lang.raw(value > 0 ? "area.yes" : "area.no");
        }
        return super.format(upgrade, value, lang);
    }

    @Override
    public List<Component> lore(AreaMachine machine, LangManager lang) {
        return List.of(lang.component("area.lumber.status_next", "progress",
                (int) Math.round(Math.min(1.0, machine.credit()) * 100)));
    }
}
