package com.sack.rpgroll.machines.area;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.machines.quarry.Quarry;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Granja: recorre un cuadrado centrado en ella, a su altura, cosecha lo maduro y lo vuelve a plantar
 * con una semilla de lo cosechado; lo demás va al cofre. Con abono, hace crecer lo que aún no está.
 */
public class FarmKind extends AreaKind {

    /** Por máquina: {maduros, cultivos} de la vuelta en curso y de la última entera. */
    private final Map<String, int[]> laps = new HashMap<>();

    @Override
    public String id() {
        return "farm";
    }

    @Override
    public String file() {
        return "farms.yml";
    }

    @Override
    public String art() {
        return "granja";
    }

    @Override
    public Material defaultBlock() {
        return Material.MOSS_BLOCK;
    }

    @Override
    public List<String> upgrades() {
        return List.of("speed", "area", "fertilizer");
    }

    @Override
    public int work(AreaMachine machine, Block block, Player owner, int budget, AreaContext context) {
        World world = block.getWorld();
        int side = side(machine);
        int cells = side * side;
        int[] lap = laps.computeIfAbsent(machine.key(), key -> new int[4]);
        machine.status(AreaStatus.WORKING);
        fertilize(machine, world, side, context);
        int used = 0;
        for (int scans = 0; scans < settings().scansPerTick(); scans++) {
            int index = Math.floorMod(machine.cursor(), cells);
            int[] column = Quarry.column(machine.x(), machine.z(), side, index);
            if (!world.isChunkLoaded(column[0] >> 4, column[1] >> 4)) {
                machine.status(AreaStatus.WAITING_CHUNK);
                break;
            }
            Block target = world.getBlockAt(column[0], machine.y(), column[1]);
            if (Crops.ripe(target)) {
                if (machine.credit() < 1.0 || used >= budget) {
                    break; // se cosecha en cuanto haya crédito: el cursor se queda aquí
                }
                if (harvest(machine, target, owner, context)) {
                    machine.credit(machine.credit() - 1.0);
                    used++;
                }
                lap[0]++;
                lap[1]++;
            } else if (Crops.growing(target)) {
                lap[1]++;
            }
            machine.cursor(index + 1);
            if (index + 1 >= cells) {
                lap[2] = lap[0];
                lap[3] = lap[1];
                lap[0] = 0;
                lap[1] = 0;
                machine.gauge(lap[3] == 0 ? 0 : (double) lap[2] / lap[3]);
            }
        }
        return used;
    }

    /** Abono: {@code fertilizer} veces por segundo, un cultivo al azar del área crece un poco. */
    private void fertilize(AreaMachine machine, World world, int side, AreaContext context) {
        double perSecond = settings().value("fertilizer", machine);
        // Solo con crédito: es cuando el trabajador ya comprobó que el área sigue en el claim.
        if (perSecond <= 0 || machine.credit() < 1.0 || context.random().nextDouble() >= perSecond / 20.0) {
            return;
        }
        int[] column = Quarry.column(machine.x(), machine.z(), side, context.random().nextInt(side * side));
        if (!world.isChunkLoaded(column[0] >> 4, column[1] >> 4)) {
            return;
        }
        Block target = world.getBlockAt(column[0], machine.y(), column[1]);
        if (Crops.growing(target)) {
            target.applyBoneMeal(BlockFace.UP);
        }
    }

    private boolean harvest(AreaMachine machine, Block target, Player owner, AreaContext context) {
        Material type = target.getType();
        BlockData before = target.getBlockData().clone();
        List<ItemStack> drops = context.breakAs(owner, target, new ItemStack(Material.AIR));
        if (drops == null) {
            return false;
        }
        if (Crops.replant(type)) {
            Material seed = Crops.seed(type);
            if (seed == null || take(drops, seed)) {
                if (before instanceof Ageable ageable) {
                    ageable.setAge(Crops.replantAge(type));
                }
                target.setBlockData(before, true);
            }
        }
        machine.buffer().addAll(drops);
        return true;
    }

    /** Quita una unidad de {@code material} de los drops; false si no había. */
    private static boolean take(List<ItemStack> drops, Material material) {
        for (Iterator<ItemStack> it = drops.iterator(); it.hasNext(); ) {
            ItemStack drop = it.next();
            if (drop.getType() == material) {
                if (drop.getAmount() <= 1) {
                    it.remove();
                } else {
                    drop.setAmount(drop.getAmount() - 1);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Component> lore(AreaMachine machine, LangManager lang) {
        int[] lap = laps.getOrDefault(machine.key(), new int[4]);
        return List.of(lang.component("area.farm.status_crops", "ripe", lap[2], "crops", lap[3]));
    }
}
