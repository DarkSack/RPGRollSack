package com.sack.rpgroll.worldforge.regen;

import com.sack.rpgroll.worldforge.api.Ids;
import com.sack.rpgroll.worldforge.api.Result;
import com.sack.rpgroll.worldforge.platform.ServerPlatform;
import com.sack.rpgroll.worldforge.protection.Protections;
import com.sack.rpgroll.worldforge.regen.Jobs.Job;
import com.sack.rpgroll.worldforge.regen.RegionFiles.ChunkPos;
import com.sack.rpgroll.worldforge.storage.Records;
import com.sack.rpgroll.worldforge.worlds.WorldService;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.logging.Logger;

/**
 * Regeneration and restore jobs: plan → confirm with a code → backup → edit → done.
 *
 * <p>Files are only edited when the server is not using them: a world WorldForge manages
 * is unloaded first (Paper); anything else (main world, its dimensions, datapack
 * dimensions, everything on Folia) waits for the next start, where {@link #startup()} runs
 * before the server opens the worlds. The backup is taken right before editing, so it
 * holds exactly what the edit replaces.
 */
public final class Regeneration {

    /** Folia has no save-all and the plan reads the region files on disk. */
    public static final String FOLIA_UNSAVED = "En Folia lo generado hace poco no está en disco hasta el autoguardado o el próximo reinicio.";

    public static final String REGEN = "regen";
    public static final String RESTORE = "restore";

    /** A square of chunks, or every generated chunk. Chunk coordinates. */
    public record Scope(String description, int minX, int minZ, int maxX, int maxZ, boolean everything) {
        public static Scope radius(int blockX, int blockZ, int radius) {
            int cx = blockX >> 4, cz = blockZ >> 4;
            return new Scope(radius + " chunks alrededor de " + blockX + ", " + blockZ, cx - radius, cz - radius, cx + radius, cz + radius, false);
        }

        public static Scope area(int x1, int z1, int x2, int z2) {
            return new Scope("área " + x1 + ", " + z1 + " → " + x2 + ", " + z2,
                    Math.min(x1, x2) >> 4, Math.min(z1, z2) >> 4, Math.max(x1, x2) >> 4, Math.max(z1, z2) >> 4, false);
        }

        public static Scope all() {
            return new Scope("todos los chunks generados", 0, 0, 0, 0, true);
        }

        boolean contains(ChunkPos c) {
            return everything || (c.x() >= minX && c.x() <= maxX && c.z() >= minZ && c.z() <= maxZ);
        }

        long size() {
            return everything ? 0 : (long) (maxX - minX + 1) * (maxZ - minZ + 1);
        }
    }

    public record Preview(Job job, int protectedChunks, long neverGenerated, long backupBytes, boolean restart) {
    }

    private record Target(Path dimension, ChunkPos spawn) {
    }

    private final ServerPlatform platform;
    private final Jobs jobs;
    private final Records records;
    private final Backups backups;
    private final Protections protections;
    private final WorldService worlds;
    private final Path container;
    private final Logger log;
    private final IntSupplier maxChunks;
    private final IntSupplier confirmMinutes;

    public Regeneration(ServerPlatform platform, Jobs jobs, Records records, Backups backups, Protections protections,
                        WorldService worlds, Path container, Logger log, IntSupplier maxChunks, IntSupplier confirmMinutes) {
        this.platform = platform;
        this.jobs = jobs;
        this.records = records;
        this.backups = backups;
        this.protections = protections;
        this.worlds = worlds;
        this.container = container.toAbsolutePath().normalize();
        this.log = log;
        this.maxChunks = maxChunks;
        this.confirmMinutes = confirmMinutes;
    }

    // ── plan ────────────────────────────────────────────

    public CompletableFuture<Result<Preview>> plan(String world, Scope scope, String actor) {
        Optional<String> blocked = protections.blockedReason();
        if (blocked.isPresent()) {
            return done(Result.fail("protections", blocked.get()));
        }
        return platform.global(() -> {
            World w = Bukkit.getWorld(world);
            if (w == null) {
                return null;
            }
            if (!platform.folia()) {
                w.save(true); // flush: chunks generated a moment ago must be in the region files the plan reads
            }
            Location s = w.getSpawnLocation();
            return new Target(w.getWorldFolder().toPath().toAbsolutePath().normalize(), new ChunkPos(s.getBlockX() >> 4, s.getBlockZ() >> 4));
        }).thenCompose(t -> {
            if (t == null) {
                return done(Result.<Preview>fail("world", "«" + world + "» no está cargado; cárgalo para planificar (la aparición y la carpeta se leen del mundo)."));
            }
            if (!t.dimension().startsWith(container) || t.dimension().equals(container)) {
                return done(Result.<Preview>fail("world", "la carpeta de «" + world + "» está fuera de la del servidor; no se toca."));
            }
            String dir = relative(t.dimension());
            return jobs.locking(world).thenCompose(locks -> {
                if (!locks.isEmpty()) {
                    return done(Result.<Preview>fail("world", busy(locks.getFirst())));
                }
                return records.world(world).thenCompose(rec -> platform.async(() -> scan(world, scope, t, dir))
                        .thenCompose(scan -> {
                            if (!scan.isOk()) {
                                return done(Result.<Preview>fail(scan.problems()));
                            }
                            Scan sc = scan.value();
                            long now = System.currentTimeMillis();
                            Job job = new Job(Ids.newJobId(), REGEN, world, dir, JobState.PLANNED, Ids.newToken(), encode(sc.chunks()),
                                    sc.chunks().size(), 0, 0, null, scope.description(), actor, now, now);
                            Preview p = new Preview(job, sc.protectedChunks(), sc.neverGenerated(), sc.bytes(),
                                    platform.folia() || rec.isEmpty());
                            return jobs.insert(job)
                                    .thenCompose(v -> records.history(actor, "regen.plan", world, job.id() + " " + job.total() + " chunks"))
                                    .thenApply(v -> Result.ok(p));
                        }));
            });
        });
    }

    private record Scan(List<ChunkPos> chunks, int protectedChunks, long neverGenerated, long bytes) {
    }

    private Result<Scan> scan(String world, Scope scope, Target t, String dir) {
        try {
            List<ChunkPos> inScope = RegionFiles.generated(t.dimension()).stream().filter(scope::contains).toList();
            List<ChunkPos> todo = new ArrayList<>();
            int prot = 0;
            for (ChunkPos c : inScope) {
                if (protections.isProtected(world, c, t.spawn())) {
                    prot++;
                } else {
                    todo.add(c);
                }
            }
            long never = scope.everything() ? 0 : scope.size() - inScope.size();
            if (todo.isEmpty()) {
                return Result.fail("scope", "no hay chunks que regenerar: " + prot + " protegidos y " + never + " sin generar."
                        + (platform.folia() && never > 0 ? " " + FOLIA_UNSAVED : ""));
            }
            if (todo.size() > maxChunks.getAsInt()) {
                return Result.fail("scope", todo.size() + " chunks superan el máximo por trabajo (" + maxChunks.getAsInt()
                        + ", regeneration.max-chunks); divide la zona.");
            }
            todo.sort(Comparator.comparingInt(ChunkPos::x).thenComparingInt(ChunkPos::z));
            return Result.ok(new Scan(todo, prot, never, backups.bytes(files(dir, todo))));
        } catch (IOException e) {
            return Result.fail("", "no se pudieron leer las regiones de " + dir + ": " + e.getMessage());
        }
    }

    /** Plans putting back the files a finished or half-done job replaced. */
    public CompletableFuture<Result<Preview>> planRestore(String sourceId, String actor) {
        return jobs.get(sourceId).thenCompose(src -> {
            if (src.isEmpty()) {
                return done(Result.<Preview>fail("job", "no hay un trabajo «" + sourceId + "»."));
            }
            Job s = src.get();
            if ((s.state() != JobState.COMPLETED && s.state() != JobState.NEEDS_RECOVERY) || s.backupId() == null) {
                return done(Result.<Preview>fail("job", s.id() + " está en " + s.state()
                        + "; solo se restaura un trabajo terminado o a medias que tenga respaldo."));
            }
            return jobs.locking(s.world()).thenCompose(locks -> {
                Optional<Job> other = locks.stream().filter(j -> !j.id().equals(s.id())).findFirst();
                if (other.isPresent()) {
                    return done(Result.<Preview>fail("world", busy(other.get())));
                }
                return jobs.backup(s.backupId()).thenCompose(bak -> records.world(s.world()).thenCompose(rec -> platform.async(() -> {
                    if (bak.isEmpty()) {
                        return Result.<Backups.Manifest>fail("backup", "falta el registro del respaldo " + s.backupId() + ".");
                    }
                    try {
                        return Result.ok(backups.verify(bak.get().id(), bak.get().manifestSha256()));
                    } catch (IOException e) {
                        return Result.<Backups.Manifest>fail("backup", e.getMessage());
                    }
                }).thenCompose(m -> {
                    if (!m.isOk()) {
                        return done(Result.<Preview>fail(m.problems()));
                    }
                    long now = System.currentTimeMillis();
                    Job job = new Job(Ids.newJobId(), RESTORE, s.world(), s.dimensionDir(), JobState.PLANNED, Ids.newToken(),
                            "backup=" + s.backupId() + ";job=" + s.id(), m.value().entries().size(), 0, 0, null,
                            "devolver los archivos de " + s.id() + " al respaldo " + s.backupId(), actor, now, now);
                    Preview p = new Preview(job, 0, 0, m.value().bytes(), platform.folia() || rec.isEmpty());
                    return jobs.insert(job)
                            .thenCompose(v -> records.history(actor, "restore.plan", s.world(), job.id() + " ← " + s.backupId()))
                            .thenApply(v -> Result.ok(p));
                })));
            });
        });
    }

    // ── confirm and run ─────────────────────────────────

    /**
     * Confirms a plan with its code. Online jobs report their end through {@code notify};
     * the returned text says what happens next.
     */
    public CompletableFuture<Result<String>> confirm(String id, String token, String actor, Consumer<String> notify) {
        return jobs.get(id).thenCompose(found -> {
            if (found.isEmpty()) {
                return done(Result.<String>fail("job", "no hay un trabajo «" + id + "»."));
            }
            Job j = found.get();
            if (j.state() != JobState.PLANNED) {
                return done(Result.<String>fail("job", j.id() + " ya no espera confirmación (está en " + j.state() + ")."));
            }
            if (!j.token().equals(token)) {
                return done(Result.<String>fail("token", "el código no coincide; míralo en el plan (/wf regen info " + j.id() + ")."));
            }
            if (System.currentTimeMillis() > j.createdAt() + confirmMinutes.getAsInt() * 60_000L) {
                return jobs.move(j.id(), JobState.PLANNED, JobState.CANCELLED, "caducó sin confirmar")
                        .thenApply(v -> Result.<String>fail("job", "el plan caducó (" + confirmMinutes.getAsInt() + " min); planifica otra vez."));
            }
            Optional<String> blocked = protections.blockedReason();
            if (blocked.isPresent()) {
                return done(Result.<String>fail("protections", blocked.get()));
            }
            String source = sourceJob(j);
            return jobs.locking(j.world()).thenCompose(locks -> {
                Optional<Job> other = locks.stream().filter(x -> !x.id().equals(source)).findFirst();
                if (other.isPresent()) {
                    return done(Result.<String>fail("world", busy(other.get())));
                }
                return platform.async(() -> spaceProblem(j)).thenCompose(space -> {
                    if (space != null) {
                        return done(Result.<String>fail("disk", space));
                    }
                    return jobs.move(j.id(), JobState.PLANNED, JobState.CONFIRMED, "confirmado por " + actor).thenCompose(moved -> {
                        if (!moved) {
                            return done(Result.<String>fail("job", "otra persona lo confirmó o canceló a la vez."));
                        }
                        Job confirmed = withState(j, JobState.CONFIRMED);
                        return records.history(actor, j.type() + ".confirm", j.world(), j.id())
                                .thenCompose(v -> records.world(j.world()))
                                .thenCompose(rec -> {
                                    if (platform.folia() || rec.isEmpty()) {
                                        return jobs.move(j.id(), JobState.CONFIRMED, JobState.PENDING_RESTART,
                                                        "se aplica en el próximo arranque, antes de abrir los mundos")
                                                .thenApply(v -> Result.ok("Confirmado. Se aplicará en el PRÓXIMO ARRANQUE, antes de que el servidor"
                                                        + " abra los mundos (respaldo incluido). Reinicia cuando te venga bien."));
                                    }
                                    runOnline(confirmed, actor, notify);
                                    return done(Result.ok("Confirmado: se descarga el mundo, se respalda y se edita; te aviso al terminar."));
                                });
                    });
                });
            });
        });
    }

    private void runOnline(Job j, String actor, Consumer<String> notify) {
        platform.global(() -> Bukkit.getWorld(j.world()) != null).thenCompose(wasLoaded -> {
            CompletableFuture<Result<Void>> unload = wasLoaded ? worlds.unload(j.world(), actor) : done(Result.ok(null));
            return unload.thenCompose(u -> {
                if (!u.isOk()) {
                    String why = "no se pudo descargar el mundo, no se tocó nada: " + u.errors().getFirst().message();
                    return jobs.move(j.id(), JobState.CONFIRMED, JobState.FAILED, why).thenApply(v -> j.id() + ": " + why);
                }
                return platform.async(() -> execute(j)).thenCompose(text -> !wasLoaded
                        ? done(j.id() + ": " + text)
                        : worlds.load(j.world(), actor).thenApply(l -> j.id() + ": " + text
                                + (l.isOk() ? " El mundo vuelve a estar cargado." : " ✘ No se volvió a cargar: " + l.errors().getFirst().message())));
            });
        }).whenComplete((text, err) -> {
            String out = err == null ? text : j.id() + ": ✘ " + err;
            log.info(out);
            notify.accept(out);
        });
    }

    /**
     * Backup, then edit. Blocking; runs on an async thread (online) or in {@code onEnable}
     * before the worlds open (restart). Returns what happened, in words.
     */
    String execute(Job j) {
        JobState from = j.state();
        Optional<String> blocked = protections.blockedReason();
        if (blocked.isPresent()) {
            String why = "no se tocó nada: " + blocked.get();
            jobs.move(j.id(), from, from == JobState.PENDING_RESTART ? JobState.CANCELLED : JobState.FAILED, why).join();
            return why;
        }
        if (!jobs.move(j.id(), from, JobState.BACKING_UP, "respaldando").join()) {
            return "el trabajo cambió de estado mientras tanto; no se tocó nada.";
        }
        String bak = Ids.newBackupId();
        try {
            Backups.Manifest m = backups.create(bak, filesOf(j));
            m = backups.verify(bak, m.sha256());
            jobs.saveBackup(new Jobs.Backup(bak, j.id(), j.world(), relative(backups.folder(bak)), m.entries().size(), m.bytes(),
                    m.sha256(), true, System.currentTimeMillis())).join();
            jobs.move(j.id(), JobState.BACKING_UP, JobState.READY, "respaldo " + bak + " verificado: " + m.entries().size()
                    + " archivos, " + mb(m.bytes())).join();
        } catch (IOException | RuntimeException e) {
            String why = "el respaldo falló, no se tocó nada: " + e.getMessage();
            jobs.move(j.id(), JobState.BACKING_UP, JobState.FAILED, why).join();
            return why;
        }
        jobs.move(j.id(), JobState.READY, JobState.RUNNING, null).join();
        try {
            String result = j.type().equals(RESTORE) ? applyRestore(j) : applyRegen(j);
            jobs.move(j.id(), JobState.RUNNING, JobState.COMPLETED, result).join();
            records.history(j.createdBy(), j.type() + ".done", j.world(), j.id() + " " + result).join();
            return result;
        } catch (IOException | RuntimeException e) {
            String why = "se detuvo a medias (" + e.getMessage() + "). El respaldo " + bak + " está verificado: /wf regen restore "
                    + j.id() + " lo devuelve, o /wf regen accept " + j.id() + " acepta el estado actual.";
            jobs.move(j.id(), JobState.RUNNING, JobState.INTERRUPTED, null).join();
            jobs.move(j.id(), JobState.INTERRUPTED, JobState.NEEDS_RECOVERY, why).join();
            return "✘ " + why;
        }
    }

    private String applyRegen(Job j) throws IOException {
        Path dim = dimension(j);
        int done = 0, skipped = 0;
        for (ChunkPos c : decode(j.plan())) {
            // Protections added after the plan count too.
            if (protections.isProtected(j.world(), c, null)) {
                skipped++;
            } else {
                RegionFiles.drop(dim, c);
                done++;
            }
            if ((done + skipped) % 256 == 0) {
                jobs.progress(j.id(), done, skipped).join();
            }
        }
        jobs.progress(j.id(), done, skipped).join();
        return done + " chunks regenerados (se generan de nuevo al visitarlos)"
                + (skipped > 0 ? "; " + skipped + " omitidos por protecciones añadidas después del plan." : ".");
    }

    private String applyRestore(Job j) throws IOException {
        String bakId = field(j.plan(), "backup");
        Jobs.Backup bak = jobs.backup(bakId).join().orElseThrow(() -> new IOException("falta el registro del respaldo " + bakId));
        Backups.Manifest m = backups.verify(bakId, bak.manifestSha256());
        backups.restore(m);
        jobs.progress(j.id(), m.entries().size(), 0).join();
        String source = sourceJob(j);
        jobs.get(source).join().filter(s -> s.state() == JobState.NEEDS_RECOVERY)
                .ifPresent(s -> jobs.move(s.id(), JobState.NEEDS_RECOVERY, JobState.RESTORED, "restaurado por " + j.id()).join());
        return m.entries().size() + " archivos devueltos al respaldo " + bakId + " (" + mb(m.bytes()) + ").";
    }

    /**
     * Runs in {@code onEnable}, before the server opens the worlds: settles jobs a shutdown
     * cut short (never resuming them blindly) and runs the ones waiting for this start.
     */
    public List<String> startup() {
        List<String> out = new ArrayList<>();
        for (Job j : jobs.inState(JobState.CONFIRMED, JobState.BACKING_UP, JobState.READY, JobState.RUNNING).join()) {
            switch (j.state()) {
                case CONFIRMED, BACKING_UP -> jobs.move(j.id(), j.state(), JobState.FAILED,
                        "el servidor se apagó antes de editar; no se tocó nada.").join();
                case READY -> jobs.move(j.id(), JobState.READY, JobState.CANCELLED,
                        "el servidor se apagó antes de editar; no se tocó nada (respaldo " + j.backupId() + ").").join();
                default -> {
                    jobs.move(j.id(), JobState.RUNNING, JobState.INTERRUPTED, null).join();
                    jobs.move(j.id(), JobState.INTERRUPTED, JobState.NEEDS_RECOVERY, "el servidor se apagó mientras editaba ("
                            + j.done() + "/" + j.total() + "). Respaldo " + j.backupId() + ": /wf regen restore " + j.id()
                            + " o /wf regen accept " + j.id() + ". El mundo queda bloqueado para otras operaciones.").join();
                }
            }
            out.add("⚠ " + j.id() + " estaba en " + j.state() + " al apagarse: " + jobs.get(j.id()).join().map(Job::message).orElse(""));
        }
        for (Job j : jobs.inState(JobState.PENDING_RESTART).join()) {
            out.add(j.id() + " (" + j.type() + ", " + j.world() + "): " + execute(j));
        }
        return out;
    }

    // ── cancel, accept, read ────────────────────────────

    public CompletableFuture<Result<String>> cancel(String id, String actor) {
        return jobs.get(id).thenCompose(found -> {
            if (found.isEmpty()) {
                return done(Result.<String>fail("job", "no hay un trabajo «" + id + "»."));
            }
            Job j = found.get();
            if (j.state() != JobState.PLANNED && j.state() != JobState.PENDING_RESTART) {
                return done(Result.<String>fail("job", j.id() + " está en " + j.state() + "; solo se cancela un plan o un trabajo que espera al reinicio"
                        + (j.state() == JobState.NEEDS_RECOVERY ? " (para este usa restore o accept)." : ".")));
            }
            return jobs.move(j.id(), j.state(), JobState.CANCELLED, "cancelado por " + actor).thenCompose(moved -> moved
                    ? records.history(actor, j.type() + ".cancel", j.world(), j.id()).thenApply(v -> Result.ok(j.id() + " cancelado; no se tocó nada."))
                    : done(Result.<String>fail("job", "cambió de estado a la vez; míralo con /wf regen info " + j.id() + ".")));
        });
    }

    /** The admin keeps the world as it is after an interrupted job; the backup stays. */
    public CompletableFuture<Result<String>> accept(String id, String actor) {
        return jobs.get(id).thenCompose(found -> {
            if (found.isEmpty() || found.get().state() != JobState.NEEDS_RECOVERY) {
                return done(Result.<String>fail("job", "solo se acepta un trabajo en NEEDS_RECOVERY."));
            }
            Job j = found.get();
            return jobs.move(j.id(), JobState.NEEDS_RECOVERY, JobState.CANCELLED, "estado aceptado por " + actor
                            + "; respaldo " + j.backupId() + " conservado")
                    .thenCompose(v -> records.history(actor, "regen.accept", j.world(), j.id()))
                    .thenApply(v -> Result.ok(j.id() + ": se acepta el estado actual. El respaldo " + j.backupId() + " se conserva."));
        });
    }

    public Jobs jobs() {
        return jobs;
    }

    // ── helpers ─────────────────────────────────────────

    private String spaceProblem(Job j) {
        try {
            long need = backups.bytes(filesOf(j));
            long free = backups.freeSpace();
            long want = need + need / 10 + 64L * 1024 * 1024;
            return free < want ? "hacen falta " + mb(want) + " libres para el respaldo y hay " + mb(free) + "." : null;
        } catch (IOException | RuntimeException e) {
            return "no se pudo calcular el espacio: " + e.getMessage();
        }
    }

    private Collection<String> filesOf(Job j) throws IOException {
        if (j.type().equals(RESTORE)) {
            String bakId = field(j.plan(), "backup");
            Jobs.Backup bak = jobs.backup(bakId).join().orElseThrow(() -> new IOException("falta el registro del respaldo " + bakId));
            return backups.verify(bakId, bak.manifestSha256()).entries().stream().map(Backups.Entry::path).toList();
        }
        dimension(j);
        return files(j.dimensionDir(), decode(j.plan()));
    }

    /** Every file that holds the chunks: region, entities and poi, plus oversized-chunk files. */
    static Collection<String> files(String dir, Collection<ChunkPos> chunks) {
        TreeSet<String> out = new TreeSet<>();
        for (ChunkPos c : chunks) {
            for (String kind : RegionFiles.KINDS) {
                out.add(dir + "/" + kind + "/r." + c.regionX() + "." + c.regionZ() + ".mca");
                out.add(dir + "/" + kind + "/c." + c.x() + "." + c.z() + ".mcc");
            }
        }
        return out;
    }

    private Path dimension(Job j) throws IOException {
        Path p = container.resolve(j.dimensionDir()).normalize();
        if (!p.startsWith(container) || p.equals(container)) {
            throw new IOException("carpeta fuera del servidor: " + j.dimensionDir());
        }
        if (!Files.isDirectory(p.resolve("region"))) {
            throw new IOException("no existe " + j.dimensionDir() + "/region");
        }
        return p;
    }

    private String relative(Path p) {
        return container.relativize(p.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    static String encode(List<ChunkPos> chunks) {
        StringBuilder b = new StringBuilder();
        for (ChunkPos c : chunks) {
            if (!b.isEmpty()) {
                b.append(';');
            }
            b.append(c.x()).append(',').append(c.z());
        }
        return b.toString();
    }

    static List<ChunkPos> decode(String plan) {
        List<ChunkPos> out = new ArrayList<>();
        for (String part : plan.split(";")) {
            if (!part.isBlank()) {
                String[] xz = part.split(",");
                out.add(new ChunkPos(Integer.parseInt(xz[0].trim()), Integer.parseInt(xz[1].trim())));
            }
        }
        return out;
    }

    private static String sourceJob(Job j) {
        return j.type().equals(RESTORE) ? field(j.plan(), "job") : j.id();
    }

    private static String field(String plan, String key) {
        for (String part : plan.split(";")) {
            if (part.startsWith(key + "=")) {
                return part.substring(key.length() + 1);
            }
        }
        return "";
    }

    private static String busy(Job j) {
        return "el mundo tiene un trabajo en marcha o pendiente: " + j.id() + " (" + j.state() + "). Termínalo, cancélalo o recupéralo antes.";
    }

    public static String mb(long bytes) {
        return String.format(java.util.Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static Job withState(Job j, JobState s) {
        return new Job(j.id(), j.type(), j.world(), j.dimensionDir(), s, j.token(), j.plan(), j.total(), j.done(), j.failed(),
                j.backupId(), j.message(), j.createdBy(), j.createdAt(), j.updatedAt());
    }

    private static <T> CompletableFuture<T> done(T value) {
        return CompletableFuture.completedFuture(value);
    }
}
