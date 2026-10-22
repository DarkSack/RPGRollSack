package com.sack.rpgroll.worldforge.template;

import com.sack.rpgroll.worldforge.api.Ids;
import com.sack.rpgroll.worldforge.api.Problem;
import com.sack.rpgroll.worldforge.api.Result;
import com.sack.rpgroll.worldforge.profile.ProfileValidator;
import com.sack.rpgroll.worldforge.storage.SafeFiles;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BlockVector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Saved templates: {@code structures/<name>.nbt} in the plugin folder, in Minecraft's own
 * structure format (what a structure block writes), so a file made elsewhere can be dropped in.
 * A template is never overwritten: a published preset that names one keeps generating the same.
 */
public final class Templates {

    public static final long MAX_BYTES = 2L << 20;

    /** One file of the folder; {@code problem} is null when it can be used. */
    public record Info(String name, int x, int y, int z, String sha256, String problem) {
    }

    private final Path folder;

    public Templates(Path folder) {
        this.folder = folder;
    }

    public Path folder() {
        return folder;
    }

    /** Reads and checks one template. Safe off the main thread: it reads the file and the registries only. */
    public Result<Structure> load(String name) {
        Optional<Problem> bad = Ids.checkId("structure", name);
        if (bad.isPresent()) {
            return Result.fail(List.of(bad.get()));
        }
        Path f = folder.resolve(name + ".nbt");
        try {
            if (!Files.isRegularFile(f)) {
                return Result.fail("structure", "no existe la plantilla «" + name + "» (structures/" + name + ".nbt).");
            }
            long bytes = Files.size(f);
            if (bytes > MAX_BYTES) {
                return Result.fail("structure", "«" + name + "» ocupa " + bytes / 1024 + " KB; como mucho " + MAX_BYTES / 1024 + " KB.");
            }
            Structure s = Bukkit.getStructureManager().loadStructure(f.toFile());
            BlockVector v = s.getSize();
            if (v.getBlockX() < 1 || v.getBlockY() < 1 || v.getBlockZ() < 1 || s.getPaletteCount() < 1) {
                return Result.fail("structure", "«" + name + "» está vacía.");
            }
            return Result.ok(s);
        } catch (IOException | RuntimeException e) {
            return Result.fail("structure", "«" + name + "» no se puede leer: " + e.getMessage());
        }
    }

    public Result<int[]> size(String name) {
        return load(name).map(s -> new int[] {s.getSize().getBlockX(), s.getSize().getBlockY(), s.getSize().getBlockZ()});
    }

    /** Names of the {@code .nbt} files in the folder, sorted, without reading them. */
    public List<String> names() {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(folder)) {
            return s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".nbt"))
                    .map(n -> n.substring(0, n.length() - 4)).sorted().toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Every {@code .nbt} in the folder, read and checked. */
    public List<Info> list() {
        List<Info> out = new ArrayList<>();
        for (String name : names()) {
            Result<Info> r = info(name);
            out.add(r.isOk() ? r.value() : new Info(name, 0, 0, 0, "", r.errors().getFirst().message()));
        }
        return out;
    }

    public Result<Info> info(String name) {
        return size(name).map(s -> new Info(name, s[0], s[1], s[2], sha(folder.resolve(name + ".nbt")), null));
    }

    /**
     * Copies a box of {@code world} (corners included) to a new template, without entities.
     * Call it where those blocks may be read: the main thread, or the region's on Folia.
     */
    public Result<Info> save(String name, World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        Optional<Problem> bad = Ids.checkId("nombre", name);
        if (bad.isPresent()) {
            return Result.fail(List.of(bad.get()));
        }
        Path f = folder.resolve(name + ".nbt");
        if (Files.exists(f)) {
            return Result.fail("nombre", "ya existe la plantilla «" + name + "». No se sobrescriben (un preset puede usarla): guarda con otro nombre.");
        }
        int minX = Math.min(x1, x2), minY = Math.min(y1, y2), minZ = Math.min(z1, z2);
        int sx = Math.abs(x2 - x1) + 1, sy = Math.abs(y2 - y1) + 1, sz = Math.abs(z2 - z1) + 1;
        if (sx > ProfileValidator.MAX_TEMPLATE_WIDTH || sz > ProfileValidator.MAX_TEMPLATE_WIDTH || sy > ProfileValidator.MAX_TEMPLATE_HEIGHT) {
            return Result.fail("zona", "mide " + sx + "×" + sy + "×" + sz + "; como mucho " + ProfileValidator.MAX_TEMPLATE_WIDTH
                    + " de ancho y de fondo y " + ProfileValidator.MAX_TEMPLATE_HEIGHT + " de alto.");
        }
        if (minY < world.getMinHeight() || minY + sy > world.getMaxHeight()) {
            return Result.fail("zona", "se sale del mundo (y de " + world.getMinHeight() + " a " + (world.getMaxHeight() - 1) + ").");
        }
        for (int cx = minX >> 4; cx <= (minX + sx - 1) >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= (minZ + sz - 1) >> 4; cz++) {
                if (!world.isChunkLoaded(cx, cz)) {
                    return Result.fail("zona", "el chunk " + cx + ", " + cz + " no está cargado: acércate o usa /forceload.");
                }
            }
        }
        StructureManager sm = Bukkit.getStructureManager();
        Structure s = sm.createStructure();
        s.fill(new Location(world, minX, minY, minZ), new BlockVector(sx, sy, sz), false);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            sm.saveStructure(bytes, s);
            SafeFiles.writeAtomic(f, bytes.toByteArray());
        } catch (IOException e) {
            return Result.fail("", "no se pudo escribir structures/" + name + ".nbt: " + e.getMessage());
        }
        return info(name);
    }

    private static String sha(Path f) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(f)), 0, 8);
        } catch (IOException | NoSuchAlgorithmException e) {
            return "?";
        }
    }
}
