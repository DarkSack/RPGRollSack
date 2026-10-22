package com.sack.rpgroll.worldforge.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.regex.Pattern;

/** File writes that never leave half a file, and paths that never leave their folder. */
public final class SafeFiles {

    /** A plain file name: no separators, no "..", no drive letters, no leading dot. */
    private static final Pattern FILE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.-]{0,63}");

    private SafeFiles() {
    }

    /** Writes to a temp file beside {@code target}, then moves it over in one step. */
    public static void writeAtomic(Path target, String content) throws IOException {
        writeAtomic(target, content.getBytes(StandardCharsets.UTF_8));
    }

    public static void writeAtomic(Path target, byte[] content) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(tmp, content,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE,
                StandardOpenOption.SYNC);
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * {@code root/name} if {@code name} is a plain file name, else empty. Used for every
     * file name that comes from a command, the GUI or an imported profile.
     */
    public static Optional<Path> child(Path root, String name) {
        if (name == null || !FILE_NAME.matcher(name).matches() || name.contains("..")) {
            return Optional.empty();
        }
        Path base = root.toAbsolutePath().normalize();
        Path p = base.resolve(name).normalize();
        return p.getParent() != null && p.getParent().equals(base) ? Optional.of(p) : Optional.empty();
    }

    /** True if {@code path} is {@code root} or inside it, after normalizing both. */
    public static boolean within(Path root, Path path) {
        return path.toAbsolutePath().normalize().startsWith(root.toAbsolutePath().normalize());
    }
}
