package com.sack.rpgroll.machines.area;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;

import java.util.Set;

/** Qué cultivos cosecha la Granja, cuándo están maduros y con qué se vuelven a plantar. */
public final class Crops {

    private static final Set<Material> AGEABLE = Set.of(Material.WHEAT, Material.CARROTS, Material.POTATOES,
            Material.BEETROOTS, Material.NETHER_WART, Material.COCOA, Material.SWEET_BERRY_BUSH);
    private static final BlockFace[] SIDES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private Crops() {
    }

    /** Lo que se gasta al replantar (null si no se replanta o no gasta nada). */
    public static Material seed(Material crop) {
        return switch (crop) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case NETHER_WART -> Material.NETHER_WART;
            case COCOA -> Material.COCOA_BEANS;
            default -> null;
        };
    }

    /** Si después de cosecharlo se vuelve a poner (la calabaza y la sandía no: el tallo sigue). */
    public static boolean replant(Material crop) {
        return AGEABLE.contains(crop);
    }

    /** Edad con la que se vuelve a poner: el arbusto de bayas se queda en 1, como al recogerlas a mano. */
    public static int replantAge(Material crop) {
        return crop == Material.SWEET_BERRY_BUSH ? 1 : 0;
    }

    /** ¿Está para cosechar? Calabazas y sandías solo si un tallo está pegado a ellas (no las decorativas). */
    public static boolean ripe(Block block) {
        Material type = block.getType();
        if (type == Material.PUMPKIN || type == Material.MELON) {
            Material stem = type == Material.PUMPKIN ? Material.ATTACHED_PUMPKIN_STEM : Material.ATTACHED_MELON_STEM;
            for (BlockFace face : SIDES) {
                if (block.getRelative(face).getType() == stem) {
                    return true;
                }
            }
            return false;
        }
        if (!AGEABLE.contains(type) || !(block.getBlockData() instanceof Ageable ageable)) {
            return false;
        }
        return type == Material.SWEET_BERRY_BUSH ? ageable.getAge() >= 2 : ageable.getAge() >= ageable.getMaximumAge();
    }

    /** Un cultivo que todavía crece (lo que el abono hace crecer). */
    public static boolean growing(Block block) {
        return AGEABLE.contains(block.getType()) && block.getBlockData() instanceof Ageable ageable
                && ageable.getAge() < ageable.getMaximumAge();
    }
}
