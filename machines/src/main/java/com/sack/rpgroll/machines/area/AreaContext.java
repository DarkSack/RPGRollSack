package com.sack.rpgroll.machines.area;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

/** Lo que la base le da a un tipo de máquina mientras trabaja. */
public interface AreaContext {

    /**
     * Rompe {@code target} en nombre de {@code owner} con {@code tool} (un {@code MachineBreakEvent}: las
     * protecciones y CoreProtect lo ven como suyo) y devuelve lo que soltó. Null si alguien lo canceló:
     * entonces el bloque sigue ahí.
     */
    List<ItemStack> breakAs(Player owner, Block target, ItemStack tool);

    Random random();
}
