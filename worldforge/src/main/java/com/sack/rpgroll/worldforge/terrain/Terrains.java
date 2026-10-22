package com.sack.rpgroll.worldforge.terrain;

import com.sack.rpgroll.worldforge.storage.SafeFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * The terrains in {@code terrains/*.yml}. The built-in ones are copied there when missing, so
 * they also serve as examples to copy; a file that does not parse is reported and left alone.
 */
public final class Terrains {

    public static final List<String> BUILT_IN = List.of("montanas", "continentes", "archipielago", "suave", "extremo");

    private final Path folder;
    private volatile Map<String, TerrainSpec> all = Map.of();

    public Terrains(Path folder) {
        this.folder = folder;
    }

    /** Returns what could not be read. */
    public synchronized List<String> load(Function<String, String> builtInSource) throws IOException {
        Files.createDirectories(folder);
        for (String id : BUILT_IN) {
            Path target = folder.resolve(id + ".yml");
            String text = builtInSource.apply(id);
            if (!Files.exists(target) && text != null) {
                SafeFiles.writeAtomic(target, text);
            }
        }
        List<String> problems = new ArrayList<>();
        Map<String, TerrainSpec> read = new TreeMap<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.yml")) {
            for (Path file : files) {
                String name = file.getFileName().toString();
                var r = TerrainSpec.parse(Files.readString(file, StandardCharsets.UTF_8));
                if (!r.isOk()) {
                    problems.add("terrains/" + name + ": " + r.errors().getFirst() + " (el archivo no se toca).");
                } else if (!name.equals(r.value().id() + ".yml")) {
                    problems.add("terrains/" + name + ": el archivo tiene que llamarse " + r.value().id() + ".yml.");
                } else {
                    read.put(r.value().id(), r.value());
                }
            }
        }
        all = Map.copyOf(read);
        return problems;
    }

    public Optional<TerrainSpec> get(String id) {
        return Optional.ofNullable(all.get(id));
    }

    public List<TerrainSpec> list() {
        return new TreeMap<>(all).values().stream().toList();
    }
}
