package com.sack.rpgroll.common.structure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;

/** Punto de entrada para leer estructuras ({@code .nbt} siempre, {@code .schem} con WorldEdit/FAWE). */
public final class BlockSources {

    private BlockSources() {
    }

    public static BlockSource read(File file) throws IOException {
        return read(file, Bukkit.getPluginManager()::isPluginEnabled);
    }

    /** {@code pluginEnabled} se inyecta para poder probar sin servidor. */
    public static BlockSource read(File file, Predicate<String> pluginEnabled) throws IOException {
        String name = file.getName();
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".nbt")) {
            return new NbtBlockSource(file);
        }
        if (lower.endsWith(".schem") || lower.endsWith(".schematic")) {
            if (!schemAvailable(pluginEnabled)) {
                throw new IOException("WorldEdit o FastAsyncWorldEdit no está activo");
            }
            return new SchemBlockSource(file);
        }
        throw new IOException("Formato no soportado: " + name);
    }

    public static boolean schemAvailable() {
        return schemAvailable(Bukkit.getPluginManager()::isPluginEnabled);
    }

    public static boolean schemAvailable(Predicate<String> pluginEnabled) {
        return pluginEnabled.test("WorldEdit") || pluginEnabled.test("FastAsyncWorldEdit");
    }

    /** Posiciones y {@link BlockData} girados; el resultado cabe en {@code 0..w'-1, 0..l'-1}. */
    public static List<SourceBlock> rotate(BlockSource src, Rotation r) {
        List<SourceBlock> out = new ArrayList<>(src.blocks().size());
        for (SourceBlock b : src.blocks()) {
            int[] p = r.apply(b.x(), b.z(), src.width(), src.length());
            BlockData data = b.data();
            if (r != Rotation.NONE) {
                data = data.clone();
                data.rotate(r.toBukkit());
            }
            out.add(new SourceBlock(p[0], b.y(), p[1], data, b.tile()));
        }
        return out;
    }
}
