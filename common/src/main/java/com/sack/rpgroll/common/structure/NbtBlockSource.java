package com.sack.rpgroll.common.structure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.structure.Palette;
import org.bukkit.structure.Structure;

/** Lector de {@code .nbt} con el StructureManager de Paper (primera paleta). */
public final class NbtBlockSource implements BlockSource {

    private final int width;
    private final int height;
    private final int length;
    private final List<SourceBlock> blocks = new ArrayList<>();

    public NbtBlockSource(File file) throws IOException {
        Structure structure = Bukkit.getStructureManager().loadStructure(file);
        width = (int) structure.getSize().getX();
        height = (int) structure.getSize().getY();
        length = (int) structure.getSize().getZ();
        if (structure.getPaletteCount() == 0) {
            return;
        }
        Palette palette = structure.getPalettes().get(0);
        for (BlockState state : palette.getBlocks()) {
            BlockData data = state.getBlockData();
            if (data.getMaterial().isAir()) {
                continue;
            }
            blocks.add(new SourceBlock(state.getX(), state.getY(), state.getZ(), data,
                    state instanceof TileState ? state : null));
        }
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public List<SourceBlock> blocks() {
        return blocks;
    }
}
