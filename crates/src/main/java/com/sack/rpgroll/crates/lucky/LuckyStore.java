package com.sack.rpgroll.crates.lucky;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

/**
 * Dónde hay lucky blocks colocados, guardado en el propio chunk (PDC): solo
 * cuenta como lucky block el que se colocó con su ítem. Un bloque musical
 * con instrumento de esqueleto que no esté aquí es un bloque musical normal
 * (no da premio al romperlo), aunque alguien lo haya conseguido con una
 * calavera encima.
 */
public class LuckyStore {

    private final NamespacedKey key;

    public LuckyStore(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "lucky-blocks");
    }

    /** Posición dentro del chunk en un int: y (con margen para las negativas), x y z de 4 bits. */
    static int pack(int x, int y, int z) {
        return ((y + 2048) << 8) | ((x & 15) << 4) | (z & 15);
    }

    public boolean contains(Block block) {
        int[] positions = read(block);
        int packed = pack(block.getX(), block.getY(), block.getZ());
        for (int position : positions) {
            if (position == packed) {
                return true;
            }
        }
        return false;
    }

    public void add(Block block) {
        if (contains(block)) {
            return;
        }
        int[] positions = read(block);
        int[] next = Arrays.copyOf(positions, positions.length + 1);
        next[positions.length] = pack(block.getX(), block.getY(), block.getZ());
        write(block, next);
    }

    public void remove(Block block) {
        int packed = pack(block.getX(), block.getY(), block.getZ());
        int[] next = Arrays.stream(read(block)).filter(position -> position != packed).toArray();
        write(block, next);
    }

    private int[] read(Block block) {
        int[] positions = block.getChunk().getPersistentDataContainer().get(key, PersistentDataType.INTEGER_ARRAY);
        return positions == null ? new int[0] : positions;
    }

    private void write(Block block, int[] positions) {
        PersistentDataContainer container = block.getChunk().getPersistentDataContainer();
        if (positions.length == 0) {
            container.remove(key);
        } else {
            container.set(key, PersistentDataType.INTEGER_ARRAY, positions);
        }
    }

}
