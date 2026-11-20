package com.sack.rpgroll.emotes.catalog;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Todos los emotes cargados, por id. */
public final class EmoteCatalog {

    static final List<String> RARITY_ORDER = List.of("common", "uncommon", "rare", "epic", "legendary");

    private static final Comparator<Emote> ORDER = Comparator
            .comparing(Emote::category)
            .thenComparingInt((Emote e) -> {
                int i = RARITY_ORDER.indexOf(e.rarity());
                return i < 0 ? RARITY_ORDER.size() : i;
            })
            // Por id y no por nombre: el nombre suele ser una clave de lang («lang:emotes.<id>»).
            .thenComparing(Emote::id);

    private final Map<String, Emote> byId = new LinkedHashMap<>();
    private final String defaultModel;

    public EmoteCatalog() {
        this("rpgroll_emotes");
    }

    public EmoteCatalog(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    /**
     * Lee los {@code *.yml} de las carpetas, recursivamente y en orden. Un archivo roto se avisa y se salta;
     * si dos emotes comparten id, gana el último.
     */
    public void load(List<File> folders, Consumer<String> warn) {
        byId.clear();
        for (File folder : folders) {
            if (folder == null || !folder.isDirectory()) {
                continue;
            }
            List<Path> files;
            try (Stream<Path> walk = Files.walk(folder.toPath())) {
                files = walk.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".yml"))
                        .sorted().toList();
            } catch (IOException e) {
                warn.accept("No se pudo leer la carpeta de emotes " + folder + ": " + e.getMessage());
                continue;
            }
            for (Path p : files) {
                loadFile(p, warn);
            }
        }
    }

    private void loadFile(Path p, Consumer<String> warn) {
        String name = p.getFileName().toString();
        String fileId = name.substring(0, name.length() - 4);
        try {
            YamlConfiguration yml = new YamlConfiguration();
            yml.loadFromString(Files.readString(p));
            Emote emote = EmoteParser.parse(fileId, yml, defaultModel, warn);
            if (byId.put(emote.id(), emote) != null) {
                warn.accept("Emote duplicado «" + emote.id() + "» (" + p + "): gana el último.");
            }
        } catch (Exception e) {
            warn.accept("Emote «" + fileId + "» ignorado (" + p + "): " + e.getMessage());
        }
    }

    /** Quita y avisa los emotes cuyo {@code (model, animation)} no existe. */
    public void checkAnimations(BiPredicate<String, String> exists, Consumer<String> warn) {
        byId.values().removeIf(e -> {
            if (exists.test(e.model(), e.animation())) {
                return false;
            }
            warn.accept("Emote «" + e.id() + "» quitado: no existe la animación «" + e.animation()
                    + "» en el modelo «" + e.model() + "».");
            return true;
        });
    }

    public Optional<Emote> get(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id.toLowerCase(Locale.ROOT)));
    }

    /** Todos, ordenados por categoría, rareza e id. */
    public List<Emote> all() {
        List<Emote> out = new ArrayList<>(byId.values());
        out.sort(ORDER);
        return out;
    }

    /** Las categorías de {@code emotes}, ordenadas y sin repetir. */
    public static List<String> categories(List<Emote> emotes) {
        return List.copyOf(new TreeSet<>(emotes.stream().map(Emote::category).toList()));
    }

    /**
     * Los que se pueden mostrar: todos menos los {@code hidden}, salvo un oculto bloqueado que {@code owns}
     * reconoce (desbloqueado o por permiso). Un oculto libre (los de magia) no sale nunca: lo lanza Magic.
     */
    public List<Emote> visibleTo(Predicate<Emote> owns) {
        return all().stream().filter(e -> !e.hidden() || (e.locked() && owns.test(e))).toList();
    }
}
