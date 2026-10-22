package com.sack.rpgroll.worldforge.worlds;

import com.sack.rpgroll.worldforge.api.Ids;
import com.sack.rpgroll.worldforge.api.Problem;
import com.sack.rpgroll.worldforge.api.Result;
import com.sack.rpgroll.worldforge.generation.GenerationProvider;
import com.sack.rpgroll.worldforge.generation.Providers;
import com.sack.rpgroll.worldforge.generation.WorldForgeGenerator;
import com.sack.rpgroll.worldforge.platform.ServerPlatform;
import com.sack.rpgroll.worldforge.profile.Profile;
import com.sack.rpgroll.worldforge.profile.ProfileService;
import com.sack.rpgroll.worldforge.storage.Records;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Worlds WorldForge creates and looks after. Creating, loading and unloading run on the
 * global scheduler; on Folia they are refused with a message, because Folia throws on all
 * three (FEASIBILITY P1, P10).
 */
public final class WorldService {

    public static final String READY = "READY";
    public static final String UNLOADED = "UNLOADED";
    public static final String MISSING = "MISSING";
    public static final String ERROR = "ERROR";

    /** Status of one world as the panel and {@code world list} show it. */
    public record View(String name, String key, String environment, boolean loaded, boolean managed,
                       String profileId, String status, String detail) {
    }

    private static final Set<String> VANILLA_KEYS = Set.of("minecraft:overworld", "minecraft:the_nether",
            "minecraft:the_end");

    private final ServerPlatform platform;
    private final ProfileService profiles;
    private final Providers providers;
    private final Records records;
    private final Logger log;
    /** Profile each loaded managed world was opened with (for what is not generation, like gravity). */
    private final Map<String, Profile> active = new java.util.concurrent.ConcurrentHashMap<>();

    public WorldService(ServerPlatform platform, ProfileService profiles, Providers providers, Records records, Logger log) {
        this.platform = platform;
        this.profiles = profiles;
        this.providers = providers;
        this.records = records;
        this.log = log;
    }

    /**
     * The profile a loaded world runs with: the one WorldForge opened it with, or, for a world set
     * in bukkit.yml, its generator's. Null for worlds WorldForge does not generate or manage.
     */
    public Profile profileOf(World w) {
        Profile p = active.get(w.getName());
        return p != null ? p : w.getGenerator() instanceof WorldForgeGenerator g ? g.profile() : null;
    }

    public static Problem foliaBlocked(String what) {
        return Problem.error("", "Folia no permite " + what + " con el servidor en marcha (lanza UnsupportedOperationException). "
                + "Usa una dimensión por datapack (se carga al reiniciar) o haz esto en Paper.");
    }

    /** Folder 26.1 uses for a world created by name: inside the main world (FEASIBILITY P2). */
    public static Path folderFor(String name) {
        Path overworld = Bukkit.getWorlds().getFirst().getWorldFolder().toPath();
        return overworld.getParent().getParent().resolve("minecraft").resolve(name);
    }

    public CompletableFuture<Result<World>> create(String name, String presetId, String actor) {
        if (platform.folia()) {
            return CompletableFuture.completedFuture(Result.fail(List.of(foliaBlocked("crear mundos"))));
        }
        Optional<Problem> badName = Ids.checkWorldName("name", name);
        if (badName.isPresent()) {
            return CompletableFuture.completedFuture(Result.fail(List.of(badName.get())));
        }
        Optional<ProfileService.Entry> entry = profiles.preset(presetId);
        if (entry.isEmpty()) {
            return CompletableFuture.completedFuture(Result.fail("preset", "no hay un preset publicado «" + presetId + "»."));
        }
        if (!entry.get().usable()) {
            List<Problem> why = new ArrayList<>(entry.get().problems().stream().filter(Problem::isError).toList());
            why.addFirst(Problem.error("preset", "«" + presetId + "» tiene errores; valídalo y corrígelo antes."));
            return CompletableFuture.completedFuture(Result.fail(why));
        }
        Profile profile = entry.get().profile();
        List<Problem> now = profiles.errorsNow(profile);
        if (!now.isEmpty()) {
            List<Problem> why = new ArrayList<>(now);
            why.addFirst(Problem.error("preset", "«" + presetId + "» ya no es válido (sus plantillas cambiaron)."));
            return CompletableFuture.completedFuture(Result.fail(why));
        }
        GenerationProvider provider = providers.get(profile.provider()).orElseThrow();

        return records.world(name).thenCompose(existing -> {
            if (existing.isPresent()) {
                return CompletableFuture.completedFuture(Result.<World>fail("name", "ya hay un mundo administrado «" + name + "» (estado " + existing.get().status() + ")."));
            }
            return platform.global(() -> {
                if (Bukkit.getWorld(name) != null) {
                    return Result.<World>fail("name", "ya hay un mundo cargado «" + name + "».");
                }
                if (Files.exists(folderFor(name))) {
                    return Result.<World>fail("name", "ya existe la carpeta de un mundo «" + name + "» que WorldForge no administra; no se pisa.");
                }
                World w = provider.configure(new WorldCreator(name), profile).createWorld();
                if (w == null) {
                    return Result.<World>fail("", "el servidor no creó el mundo (mira el registro de la consola).");
                }
                active.put(name, profile);
                return Result.ok(w);
            }).thenCompose(r -> {
                if (!r.isOk()) {
                    return CompletableFuture.completedFuture(r);
                }
                World w = r.value();
                return buildPlatform(w, profile)
                        .thenCompose(v -> records.saveWorld(new Records.ManagedWorld(name, profile.id(),
                                entry.get().hash(), READY, System.currentTimeMillis(), actor)))
                        .thenCompose(v -> records.history(actor, "world.create", name,
                                "preset=" + profile.id() + " provider=" + profile.provider() + " seed=" + w.getSeed()))
                        .thenApply(v -> r);
            });
        });
    }

    /** Builds the profile's platform, one chunk at a time on the thread that owns it. WorldForge's generator
     *  already places it; this covers vanilla (no generator) and terrain that carved it. */
    CompletableFuture<Void> buildPlatform(World w, Profile p) {
        if (p.platform() == null) {
            return CompletableFuture.completedFuture(null);
        }
        Material block = Material.matchMaterial(p.platform().block());
        int cx = p.spawn() == null ? 0 : p.spawn().x();
        int cz = p.spawn() == null ? 0 : p.spawn().z();
        int r = p.platform().radius(), y = p.platform().y();
        List<CompletableFuture<Void>> chunks = new ArrayList<>();
        for (int chunkX = (cx - r) >> 4; chunkX <= (cx + r) >> 4; chunkX++) {
            for (int chunkZ = (cz - r) >> 4; chunkZ <= (cz + r) >> 4; chunkZ++) {
                int fx = chunkX, fz = chunkZ;
                chunks.add(platform.at(w, fx, fz, () -> {
                    for (int x = Math.max(cx - r, fx << 4); x <= Math.min(cx + r, (fx << 4) + 15); x++) {
                        for (int z = Math.max(cz - r, fz << 4); z <= Math.min(cz + r, (fz << 4) + 15); z++) {
                            w.getBlockAt(x, y, z).setType(block, false);
                        }
                    }
                    return null;
                }));
            }
        }
        return CompletableFuture.allOf(chunks.toArray(CompletableFuture[]::new));
    }

    /**
     * Takes over a world folder WorldForge did not create (another plugin's, or one copied in), in
     * 26.1's layout, and loads it with a preset. Chunks that exist do not change; new ones use the preset.
     */
    public CompletableFuture<Result<World>> importWorld(String name, String presetId, String actor) {
        if (platform.folia()) {
            return CompletableFuture.completedFuture(Result.fail(List.of(foliaBlocked("cargar mundos"))));
        }
        Optional<Problem> badName = Ids.checkWorldName("name", name);
        if (badName.isPresent()) {
            return CompletableFuture.completedFuture(Result.fail(List.of(badName.get())));
        }
        Optional<ProfileService.Entry> entry = profiles.preset(presetId);
        if (entry.isEmpty() || !entry.get().usable() || !profiles.errorsNow(entry.get().profile()).isEmpty()) {
            return CompletableFuture.completedFuture(Result.fail("preset", "«" + presetId + "» no es un preset publicado y válido."));
        }
        return records.world(name).thenCompose(existing -> {
            if (existing.isPresent()) {
                return CompletableFuture.completedFuture(Result.<World>fail("name", "WorldForge ya administra «" + name + "» (estado " + existing.get().status() + ")."));
            }
            return platform.global(() -> {
                if (Bukkit.getWorld(name) != null) {
                    return Result.<Void>fail("name", "«" + name + "» está cargado por otro plugin: descárgalo con él antes de importarlo.");
                }
                Path folder = folderFor(name);
                if (Files.isDirectory(folder.resolve("region"))) {
                    return Result.<Void>ok(null);
                }
                Path legacy = Bukkit.getWorldContainer().toPath().resolve(name);
                return Result.<Void>fail("name", Files.exists(legacy.resolve("level.dat"))
                        ? "«" + name + "» está en el formato de antes de 26.1 (" + legacy + "); WorldForge solo importa mundos de 26.1 (" + folder + ")."
                        : "no hay un mundo en " + folder + ".");
            }).thenCompose(found -> !found.isOk()
                    ? CompletableFuture.completedFuture(Result.<World>fail(found.problems()))
                    : records.saveWorld(new Records.ManagedWorld(name, presetId, entry.get().hash(), UNLOADED, System.currentTimeMillis(), actor))
                            .thenCompose(v -> records.history(actor, "world.import", name, "preset=" + presetId))
                            .thenCompose(v -> load(name, actor)));
        });
    }

    /** Loads a managed world that is not loaded, with the generator of its current profile. */
    public CompletableFuture<Result<World>> load(String name, String actor) {
        if (platform.folia()) {
            return CompletableFuture.completedFuture(Result.fail(List.of(foliaBlocked("cargar mundos"))));
        }
        return records.world(name).thenCompose(rec -> {
            if (rec.isEmpty()) {
                return CompletableFuture.completedFuture(Result.<World>fail("name", "«" + name + "» no es un mundo administrado por WorldForge."));
            }
            Optional<ProfileService.Entry> entry = profiles.preset(rec.get().profileId());
            if (entry.isEmpty() || !entry.get().usable() || !profiles.errorsNow(entry.get().profile()).isEmpty()) {
                return CompletableFuture.completedFuture(Result.<World>fail("preset", "el preset «" + rec.get().profileId()
                        + "» de este mundo no existe o tiene errores; no se carga con otro generador."));
            }
            Profile profile = entry.get().profile();
            List<Problem> warnings = new ArrayList<>();
            if (!entry.get().hash().equals(rec.get().profileHash())) {
                warnings.add(Problem.warning("preset", "el preset cambió desde que se creó el mundo: los chunks NUEVOS usarán la versión actual; los existentes no cambian."));
            }
            return platform.global(() -> {
                if (Bukkit.getWorld(name) != null) {
                    return Result.<World>fail("name", "ya está cargado.");
                }
                if (!Files.isDirectory(folderFor(name))) {
                    return Result.<World>fail("name", "no está la carpeta del mundo (" + folderFor(name) + ").");
                }
                World w = providers.get(profile.provider()).orElseThrow()
                        .configure(new WorldCreator(name), profile.withSeed(null)).createWorld();
                if (w == null) {
                    return Result.<World>fail("", "el servidor no cargó el mundo.");
                }
                active.put(name, profile);
                return Result.ok(w, warnings);
            }).thenCompose(r -> r.isOk()
                    ? records.setWorldStatus(name, READY).thenCompose(v -> records.history(actor, "world.load", name, null)).thenApply(v -> r)
                    : CompletableFuture.completedFuture(r));
        });
    }

    /** Unloads (and saves) a managed world. Players in it are sent to the main world's spawn first. */
    public CompletableFuture<Result<Void>> unload(String name, String actor) {
        if (platform.folia()) {
            return CompletableFuture.completedFuture(Result.fail(List.of(foliaBlocked("descargar mundos"))));
        }
        return records.world(name).thenCompose(rec -> {
            if (rec.isEmpty()) {
                return CompletableFuture.completedFuture(Result.<Void>fail("name", "solo se descargan mundos administrados por WorldForge."));
            }
            return platform.global(() -> {
                World w = Bukkit.getWorld(name);
                if (w == null) {
                    return null;
                }
                World fallback = Bukkit.getWorlds().getFirst();
                List<CompletableFuture<Boolean>> moves = new ArrayList<>();
                for (Player p : w.getPlayers()) {
                    p.sendMessage("Este mundo se va a descargar; te llevamos al punto de aparición.");
                    moves.add(p.teleportAsync(fallback.getSpawnLocation()));
                }
                return CompletableFuture.allOf(moves.toArray(CompletableFuture[]::new));
            }).thenCompose(moved -> moved == null
                    ? CompletableFuture.completedFuture(Result.<Void>fail("name", "no está cargado."))
                    // Teleports finish on later ticks: unload only once every player has left.
                    : moved.orTimeout(30, TimeUnit.SECONDS).thenCompose(v -> waitEmptyThenUnload(name)))
                    .thenCompose(r -> r.isOk()
                            ? records.setWorldStatus(name, UNLOADED).thenCompose(v -> records.history(actor, "world.unload", name, null)).thenApply(v -> r)
                            : CompletableFuture.completedFuture(r));
        });
    }

    private CompletableFuture<Result<Void>> waitEmptyThenUnload(String name) {
        return platform.global(() -> {
            World w = Bukkit.getWorld(name);
            if (w == null) {
                return Result.<Void>ok(null);
            }
            if (!w.getPlayers().isEmpty()) {
                return Result.<Void>fail("", "quedan jugadores en el mundo; vuelve a intentarlo en unos segundos.");
            }
            if (!Bukkit.unloadWorld(w, true)) {
                return Result.<Void>fail("", "el servidor no lo descargó (¿lo usa otro plugin?).");
            }
            active.remove(name);
            return Result.<Void>ok(null);
        });
    }

    /**
     * Points a managed world at another preset. Chunks that already exist do not change;
     * new ones use the new preset once the world is reloaded (done here on Paper). A world
     * WorldForge does not manage (the main worlds) needs {@code bukkit.yml} and a restart.
     */
    public CompletableFuture<Result<String>> applyPreset(String name, String presetId, String actor) {
        Optional<ProfileService.Entry> entry = profiles.preset(presetId);
        if (entry.isEmpty() || !entry.get().usable() || !profiles.errorsNow(entry.get().profile()).isEmpty()) {
            return CompletableFuture.completedFuture(Result.fail("preset", "«" + presetId + "» no es un preset publicado y válido."));
        }
        return records.world(name).thenCompose(rec -> {
            if (rec.isEmpty()) {
                return CompletableFuture.completedFuture(Result.<String>fail("world", "«" + name + "» no lo administra WorldForge. "
                        + "Para un mundo del servidor pon en bukkit.yml → worlds: " + name + ": generator: RPGRoll-WorldForge:" + presetId
                        + " y reinicia; solo afecta a los chunks nuevos."));
            }
            Records.ManagedWorld m = rec.get();
            return records.saveWorld(new Records.ManagedWorld(name, presetId, entry.get().hash(), m.status(), m.createdAt(), m.createdBy()))
                    .thenCompose(v -> records.history(actor, "generation.apply", name, "preset " + m.profileId() + " → " + presetId))
                    .thenCompose(v -> platform.global(() -> Bukkit.getWorld(name) != null))
                    .thenCompose(loaded -> {
                        if (!loaded || platform.folia()) {
                            return CompletableFuture.completedFuture(Result.ok("Guardado. Los chunks nuevos usarán «" + presetId + "» la próxima vez que se cargue el mundo."));
                        }
                        return unload(name, actor).thenCompose(u -> u.isOk()
                                ? load(name, actor).thenApply(l -> l.isOk()
                                        ? Result.ok("Mundo recargado: los chunks nuevos ya usan «" + presetId + "»; los existentes no cambian.", l.problems())
                                        : Result.<String>fail(l.problems()))
                                : CompletableFuture.completedFuture(Result.<String>fail(u.problems())));
                    });
        });
    }

    /** What the server says about one world, plus WorldForge's record of it. */
    public CompletableFuture<Result<List<String>>> inspect(String name) {
        return records.world(name).thenCompose(rec -> platform.global(() -> {
            World w = Bukkit.getWorld(name);
            if (w == null && rec.isEmpty()) {
                return Result.<List<String>>fail("world", "no hay un mundo «" + name + "» (ni cargado ni administrado).");
            }
            List<String> lines = new ArrayList<>();
            rec.ifPresent(m -> lines.add("Administrado: preset " + m.profileId() + " (versión " + m.profileHash() + "), estado "
                    + m.status() + ", creado por " + m.createdBy()));
            if (rec.isPresent()) {
                profiles.preset(rec.get().profileId()).ifPresent(e -> {
                    if (!e.hash().equals(rec.get().profileHash())) {
                        lines.add("⚠ el preset cambió desde entonces (ahora " + e.hash() + "): afecta solo a chunks nuevos.");
                    }
                });
            }
            if (w == null) {
                lines.add("No está cargado.");
                return Result.ok(lines);
            }
            lines.add("Clave " + w.getKey() + " · entorno " + w.getEnvironment() + " · semilla " + w.getSeed());
            lines.add("Altura " + w.getMinHeight() + " a " + (w.getMaxHeight() - 1) + " · carpeta " + w.getWorldFolder());
            lines.add("Generador: " + (w.getGenerator() == null ? "Minecraft" : generatorName(w.getGenerator()))
                    + " · estructuras " + (w.canGenerateStructures()
                            && (w.getGenerator() == null || w.getGenerator().shouldGenerateStructures()) ? "sí" : "no"));
            lines.add("Aparición " + w.getSpawnLocation().getBlockX() + ", " + w.getSpawnLocation().getBlockY() + ", "
                    + w.getSpawnLocation().getBlockZ() + " · jugadores " + w.getPlayers().size()
                    + " · chunks cargados " + w.getLoadedChunks().length);
            if (w.getGenerator() instanceof WorldForgeGenerator g && g.templateCounts() != null) {
                g.templateCounts().forEach((id, c) -> lines.add("Plantilla " + id + ": " + c[0] + " colocadas, " + c[1]
                        + " sin sitio (desde que se cargó el mundo)"));
                g.recentTemplates().forEach(r -> lines.add("  última: " + r));
            }
            return Result.ok(lines);
        }));
    }

    /** WorldForge's by its simple name; another plugin's with its package, which says whose it is. */
    private static String generatorName(org.bukkit.generator.ChunkGenerator g) {
        if (g instanceof WorldForgeGenerator w) {
            return "WorldForgeGenerator · modo " + w.mode() + (w.terrain() == null ? "" : " · terreno " + w.terrain().id());
        }
        return g.getClass().getName();
    }

    /** Every loaded world plus managed ones that are not loaded. */
    public CompletableFuture<List<View>> list() {
        return records.worlds().thenCompose(managed -> platform.global(() -> {
            Map<String, Records.ManagedWorld> byName = new java.util.HashMap<>();
            managed.forEach(m -> byName.put(m.name(), m));
            List<View> out = new ArrayList<>();
            for (World w : Bukkit.getWorlds()) {
                Records.ManagedWorld m = byName.remove(w.getName());
                out.add(new View(w.getName(), w.getKey().toString(), w.getEnvironment().name(), true, m != null,
                        m == null ? null : m.profileId(), m != null ? m.status()
                                : VANILLA_KEYS.contains(w.getKey().toString()) ? "SERVIDOR"
                                : w.getKey().getNamespace().equals("worldforge") ? "DIMENSIÓN (datapack de WorldForge)" : "EXTERNO",
                        "chunks cargados: " + w.getLoadedChunks().length));
            }
            for (Records.ManagedWorld m : byName.values()) {
                out.add(new View(m.name(), "minecraft:" + m.name(), "?", false, true, m.profileId(), m.status(), null));
            }
            return out;
        }));
    }

    /**
     * At startup: Paper does not reopen worlds made with WorldCreator (FEASIBILITY P14), so
     * every managed world that was READY is loaded again with its preset. A missing preset or
     * folder marks the world instead of loading it with another generator.
     */
    public CompletableFuture<Void> restoreAtStartup() {
        return records.worlds().thenCompose(all -> {
            List<CompletableFuture<?>> steps = new ArrayList<>();
            for (Records.ManagedWorld m : all) {
                if (!READY.equals(m.status())) {
                    continue;
                }
                if (platform.folia()) {
                    log.warning("✘ " + m.name() + ": mundo administrado creado en Paper; Folia no carga mundos en caliente, queda sin cargar.");
                    continue;
                }
                steps.add(load(m.name(), "WorldForge").thenAccept(r -> {
                    if (r.isOk()) {
                        log.info("✔ Mundo administrado cargado: " + m.name() + " (preset " + m.profileId() + ")");
                        r.problems().forEach(p -> log.warning("  " + m.name() + ": " + p));
                    } else {
                        log.warning("✘ No se cargó " + m.name() + ": " + r.errors());
                        String status = r.errors().stream().anyMatch(p -> p.message().contains("carpeta")) ? MISSING : ERROR;
                        records.setWorldStatus(m.name(), status);
                    }
                }));
            }
            return CompletableFuture.allOf(steps.toArray(CompletableFuture[]::new));
        });
    }
}
