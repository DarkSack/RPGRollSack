package com.sack.rpgroll.common.structure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.data.BlockData;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;

/**
 * Lector de {@code .schem}. Único punto de la Lib que toca {@code com.sk89q.*}: solo se carga
 * tras comprobar {@link BlockSources#schemAvailable()}.
 * shortcut: los datos de bloque-entidad no se copian (tile siempre null), usar .nbt para eso.
 */
public final class SchemBlockSource implements BlockSource {

    private final int width;
    private final int height;
    private final int length;
    private final List<SourceBlock> blocks = new ArrayList<>();

    public SchemBlockSource(File file) throws IOException {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            throw new IOException("Formato de schematic no reconocido: " + file.getName());
        }
        Clipboard clipboard;
        try (FileInputStream in = new FileInputStream(file); ClipboardReader reader = format.getReader(in)) {
            clipboard = reader.read();
        }
        BlockVector3 dims = clipboard.getDimensions();
        width = dims.x();
        height = dims.y();
        length = dims.z();
        BlockVector3 min = clipboard.getMinimumPoint();
        for (BlockVector3 pos : clipboard.getRegion()) {
            BlockState state = clipboard.getBlock(pos);
            BlockData data = BukkitAdapter.adapt(state);
            if (data.getMaterial().isAir()) {
                continue;
            }
            blocks.add(new SourceBlock(pos.x() - min.x(), pos.y() - min.y(), pos.z() - min.z(), data, null));
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
