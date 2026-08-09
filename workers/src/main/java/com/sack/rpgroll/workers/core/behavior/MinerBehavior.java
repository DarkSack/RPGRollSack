package com.sack.rpgroll.workers.core.behavior;

import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.worker.Worker;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import java.util.Locale;
import java.util.Set;

/**
 * El minero busca la veta más cercana dentro de {@code searchRadius},
 * camina hacia ella, y la rompe de un golpe al llegar (sin modelar
 * durabilidad de herramienta ni tiempo de rotura real — una
 * simplificación deliberada, igual de válida que reusar vanilla en otros
 * lados de este proyecto pero acá no hay un evento vanilla equivalente
 * que reusar porque nadie está rompiendo el bloque de verdad). Evita
 * lava adyacente antes de romper.
 */
public class MinerBehavior implements ProfessionBehavior {

    private static final Set<Material> ORES = Set.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE);

    private final double searchRadius;

    public MinerBehavior(double searchRadius) {
        this.searchRadius = searchRadius;
    }

    @Override
    public void work(Worker worker, LivingEntity entity, Profession profession) {

        if (worker.isInventoryFull()) {
            return;
        }

        Location origin = entity.getLocation();
        Block target = findNearestOre(origin);

        if (target == null) {
            return;
        }

        if (origin.distance(target.getLocation()) > 2.5) {
            MovementUtil.moveToward(entity, target.getLocation());
            return;
        }

        if (isNextToLava(target)) {
            return;
        }

        Material minedType = target.getType();
        target.setType(Material.AIR);
        worker.addCarried(dropFor(minedType).name(), 1);
    }

    private Block findNearestOre(Location origin) {

        World world = origin.getWorld();

        if (world == null) {
            return null;
        }

        int radius = (int) searchRadius;
        Block nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        int baseX = origin.getBlockX();
        int baseY = origin.getBlockY();
        int baseZ = origin.getBlockZ();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {

                    Block block = world.getBlockAt(baseX + x, baseY + y, baseZ + z);

                    if (!ORES.contains(block.getType())) {
                        continue;
                    }

                    double distance = block.getLocation().distanceSquared(origin);

                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = block;
                    }
                }
            }
        }

        return nearest;
    }

    private boolean isNextToLava(Block block) {

        for (var face : new org.bukkit.block.BlockFace[] { org.bukkit.block.BlockFace.UP, org.bukkit.block.BlockFace.DOWN,
                org.bukkit.block.BlockFace.NORTH, org.bukkit.block.BlockFace.SOUTH, org.bukkit.block.BlockFace.EAST,
                org.bukkit.block.BlockFace.WEST }) {

            if (block.getRelative(face).getType() == Material.LAVA) {
                return true;
            }
        }

        return false;
    }

    private Material dropFor(Material ore) {

        String name = ore.name().toLowerCase(Locale.ROOT);

        if (name.contains("coal")) {
            return Material.COAL;
        }

        if (name.contains("copper")) {
            return Material.RAW_COPPER;
        }

        if (name.contains("iron")) {
            return Material.RAW_IRON;
        }

        if (name.contains("gold")) {
            return Material.RAW_GOLD;
        }

        if (name.contains("redstone")) {
            return Material.REDSTONE;
        }

        if (name.contains("lapis")) {
            return Material.LAPIS_LAZULI;
        }

        if (name.contains("diamond")) {
            return Material.DIAMOND;
        }

        if (name.contains("emerald")) {
            return Material.EMERALD;
        }

        return ore;
    }

}
