package com.sack.rpgroll.common.block;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Un bloque que rompe una máquina (la cantera de RPGRoll-Machines) en nombre de su dueño.
 * <p>
 * Es un {@link BlockBreakEvent} normal, así que las protecciones (GriefPrevention) y los
 * registros (CoreProtect) lo ven y lo cancelan o lo apuntan como si lo hubiera roto el
 * jugador. Lo que cambia es la herramienta: la de la máquina, no la que el dueño lleve en la
 * mano (puede estar en la otra punta del mundo con una espada).
 * <p>
 * Quien calcule drops a partir de la herramienta debe usar {@link #tool()} y
 * {@link #miningTier()}; y quien tenga un bloque que una máquina no deba romper (un lucky
 * block, un mueble) lo cancela: la máquina se lo salta.
 */
public class MachineBreakEvent extends BlockBreakEvent {

    private final ItemStack tool;
    private final int miningTier;

    /**
     * @param miningTier nivel de picado de la máquina (el de las menas de RPGRoll-Items), o -1
     *                   para que cuente el que dé la herramienta
     */
    public MachineBreakEvent(Block block, Player owner, ItemStack tool, int miningTier) {
        super(block, owner);
        this.tool = tool;
        this.miningTier = miningTier;
    }

    /** La herramienta virtual de la máquina (con sus encantamientos de fortuna o toque de seda). */
    public ItemStack tool() {
        return tool.clone();
    }

    public int miningTier() {
        return miningTier;
    }
}
