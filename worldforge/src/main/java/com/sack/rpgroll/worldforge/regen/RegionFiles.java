package com.sack.rpgroll.worldforge.regen;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Anvil region files ({@code r.<rx>.<rz>.mca}, 32×32 chunks): a 4 KiB table of chunk
 * locations followed by a 4 KiB table of timestamps. A chunk whose location entry is zero
 * does not exist, and the server generates it again the next time it loads it
 * (FEASIBILITY P8, P9). Only call {@link #drop} while the server is not using the file:
 * world unloaded, or before the worlds open in {@code onLoad}.
 */
public final class RegionFiles {

    /** Folders of a dimension that hold one region file per 32×32 chunks. */
    public static final List<String> KINDS = List.of("region", "entities", "poi");

    public record ChunkPos(int x, int z) {
        public int regionX() {
            return x >> 5;
        }

        public int regionZ() {
            return z >> 5;
        }
    }

    private RegionFiles() {
    }

    static int index(int cx, int cz) {
        return (cx & 31) + (cz & 31) * 32;
    }

    public static Path file(Path dimension, String kind, ChunkPos c) {
        return dimension.resolve(kind).resolve("r." + c.regionX() + "." + c.regionZ() + ".mca");
    }

    /** The chunks that exist in one region file; empty if the file is missing or too short. */
    public static List<ChunkPos> generated(Path regionFile, int rx, int rz) throws IOException {
        List<ChunkPos> out = new ArrayList<>();
        if (!Files.isRegularFile(regionFile) || Files.size(regionFile) < 4096) {
            return out;
        }
        ByteBuffer header = ByteBuffer.allocate(4096);
        try (FileChannel ch = FileChannel.open(regionFile, StandardOpenOption.READ)) {
            while (header.hasRemaining() && ch.read(header) >= 0) {
                // read the whole table
            }
        }
        for (int i = 0; i < 1024; i++) {
            if (header.getInt(i * 4) != 0) {
                out.add(new ChunkPos((rx << 5) + (i & 31), (rz << 5) + (i >> 5)));
            }
        }
        return out;
    }

    /** Every chunk that exists in a dimension, from its {@code region} folder. */
    public static List<ChunkPos> generated(Path dimension) throws IOException {
        List<ChunkPos> out = new ArrayList<>();
        Path folder = dimension.resolve("region");
        if (!Files.isDirectory(folder)) {
            return out;
        }
        try (var files = Files.list(folder)) {
            for (Path f : files.toList()) {
                String[] p = f.getFileName().toString().split("\\.");
                if (p.length == 4 && p[0].equals("r") && p[3].equals("mca")) {
                    try {
                        out.addAll(generated(f, Integer.parseInt(p[1]), Integer.parseInt(p[2])));
                    } catch (NumberFormatException ignored) {
                        // not a region file name
                    }
                }
            }
        }
        return out;
    }

    public static boolean exists(Path dimension, ChunkPos c) throws IOException {
        Path f = file(dimension, "region", c);
        return generated(f, c.regionX(), c.regionZ()).contains(c);
    }

    /**
     * Forgets one chunk in every kind of region file of the dimension: zeroes its location
     * and timestamp entries and deletes its external {@code c.<x>.<z>.mcc}, if any.
     *
     * @return whether the chunk had terrain data
     */
    public static boolean drop(Path dimension, ChunkPos c) throws IOException {
        boolean had = false;
        for (String kind : KINDS) {
            Path f = file(dimension, kind, c);
            if (Files.isRegularFile(f) && Files.size(f) >= 8192) {
                boolean data = zero(f, index(c.x(), c.z()));
                had |= kind.equals("region") && data;
            }
            Files.deleteIfExists(f.resolveSibling("c." + c.x() + "." + c.z() + ".mcc"));
        }
        return had;
    }

    private static boolean zero(Path f, int i) throws IOException {
        try (FileChannel ch = FileChannel.open(f, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            ByteBuffer before = ByteBuffer.allocate(4);
            ch.read(before, i * 4L);
            ch.write(ByteBuffer.allocate(4), i * 4L);
            ch.write(ByteBuffer.allocate(4), 4096 + i * 4L);
            ch.force(true);
            return before.getInt(0) != 0;
        }
    }
}
