package com.sack.rpgroll.worldforge.profile;

import com.sack.rpgroll.worldforge.api.Ids;
import com.sack.rpgroll.worldforge.api.Problem;
import com.sack.rpgroll.worldforge.api.Result;
import com.sack.rpgroll.worldforge.storage.SafeFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Profiles on disk. {@code presets/} holds the ones worlds can use; {@code drafts/} holds
 * imports and work in progress, which never reach a world until published. Reads come from
 * memory (safe on any thread); writes touch the disk and must run off the tick thread.
 */
public final class ProfileService {

    /** A profile as loaded: maybe unusable, with the reasons. */
    public record Entry(Profile profile, String hash, boolean draft, List<Problem> problems, Path file) {
        public boolean usable() {
            return !draft && problems.stream().noneMatch(Problem::isError);
        }
    }

    public static final List<String> BUILT_IN = List.of("vanilla", "amplified", "flat", "empty", "void-platform",
            "resource-rich", "custom", "nether", "end", "montanas");

    private final Path presets;
    private final Path drafts;
    private final Path imports;
    private final Path exports;
    private final long maxImportBytes;
    private final Supplier<ProfileValidator> validator;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final List<Problem> loadProblems = new ArrayList<>();

    /**
     * @param validator a supplier because the live catalog needs the server registries,
     *                  which are ready only once the server has started
     */
    public ProfileService(Path dataFolder, long maxImportBytes, Supplier<ProfileValidator> validator) {
        this.presets = dataFolder.resolve("presets");
        this.drafts = dataFolder.resolve("drafts");
        this.imports = dataFolder.resolve("imports");
        this.exports = dataFolder.resolve("exports");
        this.maxImportBytes = maxImportBytes;
        this.validator = validator;
    }

    /** Copies missing built-in presets, then reads every file. A broken file is kept as it is and reported. */
    public synchronized void load(java.util.function.Function<String, String> builtInSource) throws IOException {
        Files.createDirectories(presets);
        Files.createDirectories(drafts);
        Files.createDirectories(imports);
        Files.createDirectories(exports);
        for (String id : BUILT_IN) {
            Path target = presets.resolve(id + ".yml");
            String text = builtInSource.apply(id);
            if (!Files.exists(target) && text != null) {
                SafeFiles.writeAtomic(target, text);
            }
        }
        entries.clear();
        loadProblems.clear();
        readFolder(presets, false);
        readFolder(drafts, true);
    }

    private void readFolder(Path folder, boolean draft) throws IOException {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.yml")) {
            for (Path file : files) {
                String fileId = file.getFileName().toString().replaceFirst("\\.yml$", "");
                Result<Profile> parsed = ProfileCodec.read(Files.readString(file, StandardCharsets.UTF_8));
                if (!parsed.isOk()) {
                    loadProblems.add(Problem.error(folder.getFileName() + "/" + file.getFileName(),
                            "no se pudo leer: " + parsed.errors().getFirst() + " (el archivo no se toca)."));
                    continue;
                }
                Profile p = parsed.value();
                List<Problem> problems = new ArrayList<>(parsed.problems());
                if (!fileId.equals(p.id())) {
                    problems.add(Problem.error("id", "el archivo se llama «" + fileId + "» y el perfil dice «" + p.id() + "»."));
                }
                problems.addAll(validator.get().validate(p));
                String key = (draft ? "draft:" : "") + p.id();
                if (entries.containsKey(key)) {
                    loadProblems.add(Problem.error(file.getFileName().toString(), "id repetido «" + p.id() + "»."));
                    continue;
                }
                entries.put(key, new Entry(p, ProfileCodec.hash(p), draft, problems, file));
            }
        }
    }

    /** Re-checks every profile against the validator (the server registries may have changed). */
    public synchronized void revalidate() {
        entries.replaceAll((k, e) -> {
            List<Problem> problems = new ArrayList<>(ProfileCodec.read(ProfileCodec.write(e.profile())).problems());
            problems.addAll(validator.get().validate(e.profile()));
            return new Entry(e.profile(), e.hash(), e.draft(), problems, e.file());
        });
    }

    public Optional<Entry> preset(String id) {
        return Optional.ofNullable(entries.get(id));
    }

    public Optional<Entry> draft(String id) {
        return Optional.ofNullable(entries.get("draft:" + id));
    }

    /** Presets first, then drafts; by id. */
    public List<Entry> all() {
        return entries.values().stream()
                .sorted(Comparator.comparing(Entry::draft).thenComparing(e -> e.profile().id()))
                .toList();
    }

    public List<Problem> loadProblems() {
        return List.copyOf(loadProblems);
    }

    /**
     * Saves a draft. {@code expectedHash} is the version the editor started from: if someone
     * else saved in between, nothing is written (null = new draft).
     */
    public synchronized Result<Entry> saveDraft(Profile p, String expectedHash) {
        Optional<Problem> bad = Ids.checkId("id", p.id());
        if (bad.isPresent()) {
            return Result.fail(List.of(bad.get()));
        }
        Entry current = entries.get("draft:" + p.id());
        String currentHash = current == null ? null : current.hash();
        if (!java.util.Objects.equals(currentHash, expectedHash)) {
            return Result.fail("", "otro administrador guardó «" + p.id() + "» mientras lo editabas. Vuelve a abrirlo.");
        }
        return write(drafts, p, true);
    }

    /** Validates a draft and moves it to presets. Refuses to overwrite a preset that changed meanwhile. */
    public synchronized Result<Entry> publish(String id, String expectedPresetHash) {
        Entry d = entries.get("draft:" + id);
        if (d == null) {
            return Result.fail("id", "no hay un borrador «" + id + "».");
        }
        List<Problem> problems = validator.get().validate(d.profile());
        if (problems.stream().anyMatch(Problem::isError)) {
            return Result.fail(problems);
        }
        Entry current = entries.get(id);
        if (current != null && !current.hash().equals(expectedPresetHash)) {
            return Result.fail("", "el preset «" + id + "» cambió; revisa la comparación antes de publicar.");
        }
        Result<Entry> written = write(presets, d.profile(), false);
        if (written.isOk()) {
            try {
                Files.deleteIfExists(d.file());
            } catch (IOException e) {
                return Result.ok(written.value(), List.of(Problem.warning("", "publicado, pero el borrador no se pudo borrar: " + e.getMessage())));
            }
            entries.remove("draft:" + id);
        }
        return written;
    }

    /** New draft copied from an existing preset or draft. */
    public synchronized Result<Entry> duplicate(String sourceId, String newId, String newName) {
        Entry src = entries.containsKey(sourceId) ? entries.get(sourceId) : entries.get("draft:" + sourceId);
        if (src == null) {
            return Result.fail("source", "no existe «" + sourceId + "».");
        }
        if (entries.containsKey(newId) || entries.containsKey("draft:" + newId)) {
            return Result.fail("id", "ya existe «" + newId + "».");
        }
        return saveDraft(src.profile().withId(newId, newName), null);
    }

    /** Deterministic export to {@code exports/<id>.yml} plus a manifest with its SHA-256. */
    public Result<Path> export(String id) {
        Entry e = entries.get(id);
        if (e == null) {
            return Result.fail("id", "no hay un preset «" + id + "».");
        }
        String text = ProfileCodec.write(e.profile());
        Path file = exports.resolve(id + ".yml");
        try {
            SafeFiles.writeAtomic(file, text);
            SafeFiles.writeAtomic(exports.resolve(id + ".manifest"), "file: " + id + ".yml\nsha256: "
                    + sha256(text.getBytes(StandardCharsets.UTF_8)) + "\nschema: " + e.profile().schema()
                    + "\nminecraft: " + e.profile().minecraft() + "\nprovider: " + e.profile().provider() + "\n");
            return Result.ok(file);
        } catch (IOException ex) {
            return Result.fail("", "no se pudo escribir la exportación: " + ex.getMessage());
        }
    }

    /**
     * Reads {@code imports/<fileName>} and stores it as a draft. Only plain file names inside
     * the imports folder are accepted; size is capped; the profile is validated first.
     */
    public synchronized Result<Entry> importFile(String fileName) {
        Optional<Path> file = SafeFiles.child(imports, fileName);
        if (file.isEmpty()) {
            return Result.fail("file", "«" + fileName + "» no es un nombre de archivo válido (solo el nombre, dentro de la carpeta imports).");
        }
        try {
            if (!Files.isRegularFile(file.get())) {
                return Result.fail("file", "no existe imports/" + fileName + ".");
            }
            long size = Files.size(file.get());
            if (size > maxImportBytes) {
                return Result.fail("file", "pesa " + size + " bytes; el límite es " + maxImportBytes + ".");
            }
            Result<Profile> parsed = ProfileCodec.read(Files.readString(file.get(), StandardCharsets.UTF_8));
            if (!parsed.isOk()) {
                return Result.fail(parsed.problems());
            }
            Profile p = parsed.value();
            Optional<Problem> bad = Ids.checkId("id", p.id());
            if (bad.isPresent()) {
                return Result.fail(List.of(bad.get()));
            }
            Result<Entry> saved = write(drafts, p, true);
            if (!saved.isOk()) {
                return saved;
            }
            // Errors of a draft do not stop the import (that is what drafts are for); they
            // travel as warnings and stay on the entry until fixed. Publishing re-checks.
            List<Problem> warnings = new ArrayList<>(parsed.problems());
            saved.value().problems().stream()
                    .map(x -> x.isError() ? Problem.warning(x.field(), "(impide publicar) " + x.message()) : x)
                    .forEach(warnings::add);
            return Result.ok(saved.value(), warnings);
        } catch (IOException e) {
            return Result.fail("file", "no se pudo leer: " + e.getMessage());
        }
    }

    /**
     * Errors of {@code p} right now. Its templates live in their own files, which can change or
     * disappear after the preset was loaded; callers that are about to generate check again.
     */
    public List<Problem> errorsNow(Profile p) {
        return p.templates().isEmpty() ? List.of() : validator.get().validate(p).stream().filter(Problem::isError).toList();
    }

    private Result<Entry> write(Path folder, Profile p, boolean draft) {
        Path file = folder.resolve(p.id() + ".yml");
        try {
            SafeFiles.writeAtomic(file, ProfileCodec.write(p));
        } catch (IOException e) {
            return Result.fail("", "no se pudo guardar " + file.getFileName() + ": " + e.getMessage());
        }
        List<Problem> problems = validator.get().validate(p);
        Entry e = new Entry(p, ProfileCodec.hash(p), draft, problems, file);
        entries.put((draft ? "draft:" : "") + p.id(), e);
        return Result.ok(e);
    }

    static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
