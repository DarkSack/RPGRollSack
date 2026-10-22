package com.sack.rpgroll.worldforge.diagnostics;

import com.sack.rpgroll.worldforge.api.Problem;
import com.sack.rpgroll.worldforge.generation.GenerationProvider;
import com.sack.rpgroll.worldforge.generation.Providers;
import com.sack.rpgroll.worldforge.dimension.Dimensions;
import com.sack.rpgroll.worldforge.platform.ServerPlatform;
import com.sack.rpgroll.worldforge.protection.Protections;
import com.sack.rpgroll.worldforge.regen.JobState;
import com.sack.rpgroll.worldforge.regen.Jobs;
import com.sack.rpgroll.worldforge.profile.ProfileService;
import com.sack.rpgroll.worldforge.storage.Database;
import com.sack.rpgroll.worldforge.storage.Records;
import com.sack.rpgroll.worldforge.worlds.WorldService;

import io.papermc.paper.datapack.Datapack;

import org.bukkit.Bukkit;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * The state of WorldForge and of the server as far as it matters here. Every line says
 * what was checked; a dimension or world counts as available only if the server has it
 * loaded, not because a file exists.
 */
public final class Diagnostics {

    private final String version;
    private final ServerPlatform platform;
    private final Path dataFolder;
    private final ProfileService profiles;
    private final Providers providers;
    private final Database db;
    private final String dbError;
    private final Records records;
    private final WorldService worlds;
    private final Supplier<List<String>> configProblems;
    private final Protections protections;
    private final Jobs jobs;
    private final Dimensions dimensions;

    public Diagnostics(String version, ServerPlatform platform, Path dataFolder, ProfileService profiles,
                       Providers providers, Database db, String dbError, Records records, WorldService worlds,
                       Supplier<List<String>> configProblems, Protections protections, Jobs jobs,
                       Dimensions dimensions) {
        this.version = version;
        this.platform = platform;
        this.dataFolder = dataFolder;
        this.profiles = profiles;
        this.providers = providers;
        this.db = db;
        this.dbError = dbError;
        this.records = records;
        this.worlds = worlds;
        this.configProblems = configProblems;
        this.protections = protections;
        this.jobs = jobs;
        this.dimensions = dimensions;
    }

    public CompletableFuture<List<String>> report() {
        List<String> out = new ArrayList<>();
        out.add("WorldForge " + version + " · Minecraft " + Bukkit.getMinecraftVersion() + " · " + Bukkit.getName()
                + " " + Bukkit.getVersion());
        out.add("Plataforma: " + platform.name() + (platform.folia()
                ? " — crear, cargar y descargar mundos en caliente están DESACTIVADOS (Folia lanza UnsupportedOperationException)."
                : " — todas las funciones de mundos disponibles."));

        // Off the tick thread: the command that asks runs on it.
        CompletableFuture<String> sqlite = db == null
                ? CompletableFuture.completedFuture("✘ SQLite: " + dbError)
                : platform.async(() -> {
                    try {
                        return "✔ SQLite: esquema " + db.schemaVersion() + " (" + dataFolder.resolve("worldforge.db").getFileName() + ")";
                    } catch (Exception e) {
                        return "✘ SQLite: " + e.getMessage();
                    }
                });
        configProblems.get().forEach(p -> out.add("✘ Configuración: " + p));
        protections.blockedReason().ifPresentOrElse(r -> out.add("✘ Regeneración BLOQUEADA: " + r),
                () -> out.add("Protecciones: " + protections.areas().size() + " zonas · " + protections.spawnRadius()
                        + " chunks alrededor de cada aparición"));

        List<ProfileService.Entry> all = profiles.all();
        long usable = all.stream().filter(ProfileService.Entry::usable).count();
        long drafts = all.stream().filter(ProfileService.Entry::draft).count();
        out.add("Presets: " + usable + " utilizables, " + drafts + " borradores, "
                + (all.size() - usable - drafts) + " con errores.");
        for (ProfileService.Entry e : all) {
            e.problems().stream().filter(Problem::isError).findFirst()
                    .ifPresent(p -> out.add("  ✘ " + (e.draft() ? "borrador " : "") + e.profile().id() + ": " + p));
        }
        profiles.loadProblems().forEach(p -> out.add("  ✘ " + p));

        for (GenerationProvider p : providers.all().values().stream()
                .sorted(Comparator.comparing(GenerationProvider::id)).toList()) {
            out.add("Proveedor " + p.id() + ": " + p.capabilities().stream().map(c -> c.label())
                    .collect(Collectors.joining(", ")));
        }

        long free = dataFolder.toFile().getUsableSpace();
        out.add("Espacio libre junto a los datos: " + (free / (1024 * 1024)) + " MB");

        CompletableFuture<List<String>> packs = platform.global(() -> Bukkit.getDatapackManager().getPacks().stream()
                .filter(Datapack::isEnabled)
                .map(d -> d.getName() + " (" + d.getSource() + ")").sorted().toList())
                .thenApply(l -> List.of("Datapacks activos: " + String.join(", ", l)));

        CompletableFuture<List<String>> worldLines = worlds == null
                ? CompletableFuture.completedFuture(List.of())
                : worlds.list().thenApply(views -> views.stream().map(v -> "Mundo " + v.name() + " [" + v.key() + "] "
                        + (v.loaded() ? "cargado" : "NO cargado") + " · " + v.status()
                        + (v.profileId() == null ? "" : " · preset " + v.profileId())).toList());

        CompletableFuture<List<String>> history = records == null
                ? CompletableFuture.completedFuture(List.of())
                : records.recentHistory(5).thenApply(h -> {
                    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    List<String> lines = new ArrayList<>();
                    if (!h.isEmpty()) {
                        lines.add("Últimas operaciones:");
                    }
                    h.forEach(e -> lines.add("  " + f.format(new Date(e.ts())) + " " + e.actor() + " " + e.action()
                            + (e.target() == null ? "" : " " + e.target())));
                    return lines;
                });

        CompletableFuture<List<String>> jobLines = jobs == null
                ? CompletableFuture.completedFuture(List.of())
                : jobs.inState(Arrays.stream(JobState.values()).filter(st -> !st.finished()).toArray(JobState[]::new))
                        .thenApply(l -> l.stream().map(j -> (j.state() == JobState.NEEDS_RECOVERY ? "✘ " : "")
                                + "Trabajo " + j.id() + " (" + j.type() + ", " + j.world() + "): " + j.state()
                                + (j.message() == null ? "" : " — " + j.message())).toList());

        CompletableFuture<List<String>> dimLines = dimensions == null
                ? CompletableFuture.completedFuture(List.of())
                : dimensions.list().thenApply(l -> l.stream().filter(d -> !d.state().equals(Dimensions.REMOVED))
                        .map(d -> (d.state().equals(Dimensions.ERROR) ? "✘ " : "") + "Dimensión " + d.id() + " (" + d.type() + ", "
                                + d.generator() + "): " + d.state() + (d.loaded() ? " · cargada como " + d.worldName() : ""))
                        .collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), x -> {
                            boolean data = dimensions.vanillaReadable();
                            x.add((data ? "" : "✘ ") + "Terrenos: " + dimensions.terrains().list().size() + " en terrains/ · mundo principal: "
                                    + dimensions.mainTerrain().orElse("vanilla") + " · datos de vanilla del servidor: "
                                    + (data ? "legibles" : "NO legibles (no se pueden escribir terrenos)"));
                            return x;
                        })));

        return sqlite.thenCompose(line -> {
            out.add(2, line);
            return jobLines;
        }).thenCompose(jl -> {
            out.addAll(jl);
            return dimLines;
        }).thenCompose(dl -> {
            out.addAll(dl);
            return packs;
        }).thenCombine(worldLines, (a, b) -> {
            List<String> l = new ArrayList<>(a);
            l.addAll(b);
            return l;
        }).thenCombine(history, (a, b) -> {
            out.addAll(a);
            out.addAll(b);
            return out;
        });
    }
}
