package com.sack.rpgroll.worldforge.regen;

import com.sack.rpgroll.worldforge.storage.SafeFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeSet;

/**
 * Copies of world files taken right before WorldForge edits them, with a SHA-256 manifest.
 * Paths are relative to the server's world container, so a backup can only be written back
 * where it came from. A backup is never deleted by WorldForge.
 *
 * <pre>backups/&lt;bak-id&gt;/manifest.txt   one line per file: sha256 size path
 * backups/&lt;bak-id&gt;/files/&lt;path&gt;</pre>
 */
public final class Backups {

    public record Entry(String path, long size, String sha256) {
    }

    public record Manifest(String id, List<Entry> entries, String sha256) {
        public long bytes() {
            return entries.stream().mapToLong(Entry::size).sum();
        }
    }

    private final Path root;
    private final Path container;

    public Backups(Path root, Path container) {
        this.root = root.toAbsolutePath().normalize();
        this.container = container.toAbsolutePath().normalize();
    }

    /** Bytes of the files that exist, for the preview and the free-space check. */
    public long bytes(Collection<String> paths) throws IOException {
        long sum = 0;
        for (String p : paths) {
            Path f = inContainer(p);
            if (Files.isRegularFile(f)) {
                sum += Files.size(f);
            }
        }
        return sum;
    }

    public long freeSpace() throws IOException {
        Files.createDirectories(root);
        return Files.getFileStore(root).getUsableSpace();
    }

    /** Copies the files that exist, re-reads every copy and checks its hash. Missing files are skipped. */
    public Manifest create(String id, Collection<String> paths) throws IOException {
        Path dir = folder(id);
        if (Files.exists(dir)) {
            throw new IOException("ya existe el respaldo " + id);
        }
        List<Entry> entries = new ArrayList<>();
        for (String p : new TreeSet<>(paths)) {
            Path src = inContainer(p);
            if (!Files.isRegularFile(src)) {
                continue;
            }
            byte[] data = Files.readAllBytes(src);
            Path copy = inside(dir.resolve("files"), p);
            Files.createDirectories(copy.getParent());
            Files.write(copy, data);
            String sha = sha256(data);
            if (!sha.equals(sha256(Files.readAllBytes(copy)))) {
                throw new IOException("la copia de " + p + " no coincide al releerla");
            }
            entries.add(new Entry(p, data.length, sha));
        }
        StringBuilder text = new StringBuilder();
        entries.forEach(e -> text.append(e.sha256()).append(' ').append(e.size()).append(' ').append(e.path()).append('\n'));
        Files.createDirectories(dir);
        SafeFiles.writeAtomic(dir.resolve("manifest.txt"), text.toString());
        return new Manifest(id, List.copyOf(entries), sha256(text.toString().getBytes(StandardCharsets.UTF_8)));
    }

    /** Reads a manifest and checks every copy against it. */
    public Manifest verify(String id, String expectedManifestSha) throws IOException {
        Path dir = folder(id);
        byte[] raw = Files.readAllBytes(dir.resolve("manifest.txt"));
        String sha = sha256(raw);
        if (expectedManifestSha != null && !expectedManifestSha.equals(sha)) {
            throw new IOException("el manifiesto de " + id + " cambió desde que se hizo el respaldo");
        }
        List<Entry> entries = new ArrayList<>();
        for (String line : new String(raw, StandardCharsets.UTF_8).split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] f = line.split(" ", 3);
            if (f.length != 3 || !f[0].matches("[0-9a-f]{64}") || !f[1].matches("[0-9]{1,12}")) {
                throw new IOException("línea de manifiesto no válida en " + id + ": " + line);
            }
            Entry e = new Entry(f[2], Long.parseLong(f[1]), f[0]);
            inContainer(e.path());
            Path copy = inside(dir.resolve("files"), e.path());
            if (!Files.isRegularFile(copy) || !e.sha256().equals(sha256(Files.readAllBytes(copy)))) {
                throw new IOException("la copia de " + e.path() + " en " + id + " falta o está dañada");
            }
            entries.add(e);
        }
        return new Manifest(id, List.copyOf(entries), sha);
    }

    /** Writes every file of a verified backup back to its place: temporary copy, hash check, atomic move. */
    public void restore(Manifest m) throws IOException {
        Path files = folder(m.id()).resolve("files");
        for (Entry e : m.entries()) {
            Path target = inContainer(e.path());
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".wf-restore");
            Files.copy(inside(files, e.path()), tmp, StandardCopyOption.REPLACE_EXISTING);
            if (!e.sha256().equals(sha256(Files.readAllBytes(tmp)))) {
                Files.deleteIfExists(tmp);
                throw new IOException("la copia restaurada de " + e.path() + " no coincide");
            }
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public Path folder(String id) throws IOException {
        if (!id.matches("bak-[0-9a-f]{8}")) {
            throw new IOException("id de respaldo no válido: " + id);
        }
        return root.resolve(id);
    }

    private Path inContainer(String relative) throws IOException {
        return inside(container, relative);
    }

    private static Path inside(Path base, String relative) throws IOException {
        Path p = base.resolve(relative).normalize();
        if (relative.isBlank() || Path.of(relative).isAbsolute() || !p.startsWith(base) || p.equals(base)) {
            throw new IOException("ruta fuera de su carpeta: " + relative);
        }
        return p;
    }

    static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
