package com.sack.rpgroll.structures.build;

import com.sack.rpgroll.common.structure.SourceBlock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;

/** Acceso al mundo del motor de construcción; separado para probar el motor sin servidor. Hilo principal. */
public interface BlockWriter {

    /** Lo que hay ahora en una posición. */
    record BlockInfo(Material type, boolean replaceable, BlockData data) {
    }

    boolean chunkLoaded(String world, int x, int z);

    BlockInfo read(String world, int x, int y, int z);

    void write(String world, int x, int y, int z, SourceBlock b);

    /** ¿Tiene ese bloque un estado propio (cofre con ítems, cartel con texto...) que {@link #write} perdería? */
    default boolean keepsState(String world, int x, int y, int z) {
        return false;
    }

    /** El de verdad, sobre Paper. Fino a propósito: no se puede probar sin servidor. */
    static BlockWriter bukkit() {
        return new BlockWriter() {
            @Override
            public boolean chunkLoaded(String world, int x, int z) {
                World w = Bukkit.getWorld(world);
                return w != null && w.isChunkLoaded(x >> 4, z >> 4);
            }

            @Override
            public BlockInfo read(String world, int x, int y, int z) {
                Block b = Bukkit.getWorld(world).getBlockAt(x, y, z);
                return new BlockInfo(b.getType(), b.isReplaceable(), b.getBlockData());
            }

            @Override
            public boolean keepsState(String world, int x, int y, int z) {
                return Bukkit.getWorld(world).getBlockAt(x, y, z).getState(false) instanceof TileState;
            }

            @Override
            public void write(String world, int x, int y, int z, SourceBlock s) {
                Block b = Bukkit.getWorld(world).getBlockAt(x, y, z);
                if (s.tile() == null) {
                    b.setBlockData(s.data(), false);
                    return;
                }
                // El tile guarda el BlockData SIN girar: la copia lleva el girado antes de escribirse (una sola escritura).
                BlockState copy = s.tile().copy(b.getLocation());
                copy.setBlockData(s.data());
                copy.update(true, false);
            }
        };
    }
}
