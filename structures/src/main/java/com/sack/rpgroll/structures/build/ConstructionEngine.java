package com.sack.rpgroll.structures.build;

import com.sack.rpgroll.common.structure.BlockSource;
import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.catalog.RotatedCache;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.catalog.StructurePerms;
import com.sack.rpgroll.structures.config.PerformanceSettings;
import com.sack.rpgroll.structures.project.DepositMath;
import com.sack.rpgroll.structures.project.Project;
import com.sack.rpgroll.structures.project.ProjectKind;
import com.sack.rpgroll.structures.project.ProjectService;
import com.sack.rpgroll.structures.project.ProjectState;
import com.sack.rpgroll.structures.protect.AreaGuard;
import com.sack.rpgroll.structures.storage.Box;
import com.sack.rpgroll.structures.storage.BuildRecord;
import com.sack.rpgroll.structures.storage.MaterialRow;
import com.sack.rpgroll.structures.storage.Movement;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import com.sack.rpgroll.structures.workbench.WorkbenchService;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coloca los bloques de las obras BUILDING bajo el presupuesto global por tick. Todo en el hilo principal salvo
 * el cálculo del plan (girar y ordenar, en {@code planner}), que se instala aquí al estar listo.
 *
 * <p>Materiales: cada tick que una obra avanza se apunta el CONSUME de la diferencia entre
 * {@link DepositMath#consumedAt} del cursor nuevo y lo ya cobrado (memoria al momento, para que una cancelación
 * calcule bien lo no gastado; la base de datos va en orden detrás). Todos los CONSUME del tick van en un solo lote.
 * Lo ya cobrado sale de la base de datos al crear el plan de la obra, así que al retomar desde un cursor guardado más
 * viejo no se cobra dos veces. El cursor se guarda como mucho una vez por segundo por obra, y siempre al empezar,
 * pausar, terminar, fallar y en {@link #flushAll}.
 */
public final class ConstructionEngine {

    public enum StartResult {
        STARTED, NOT_READY, LIMIT_SERVER, LIMIT_WORLD, LIMIT_PLAYER, NO_PERMISSION, PROTECTED,
        /** El módulo de la estructura no deja construir ahí; su clave en {@link #deniedKey()}. */
        MODULE_DENIED
    }

    /** {@code precheck} del módulo de la estructura: clave de idioma si no se puede construir en esa caja. */
    @FunctionalInterface
    public interface Precheck {
        Optional<String> check(StructureDefinition def, UUID owner, String world, Box box);
    }

    static final long SAVE_EVERY_MS = 1000;
    /**
     * Espera entre intentos de registrar una obra terminada, según los fallos seguidos (el último se repite). Nunca
     * pasa a ERROR por esto: la obra sigue BUILDING, con su caja y su mesa ocupadas, hasta que la base de datos
     * responda.
     */
    static final long[] COMPLETE_BACKOFF_MS = { 1_000, 5_000, 30_000, 60_000 };
    static final String BUILD_PERMISSION = "rpgrollstructures.build";
    /** Motivos de pausa: clave de idioma en {@link Project#error()} mientras la obra está PAUSED. */
    public static final String PAUSED_PROTECTED = "project.paused.protected";
    public static final String PAUSED_BENCH = "project.paused.bench";
    public static final String PAUSED_PLAYER = "project.paused.player";

    /** Plan calculado una vez por obra y lo que ya se cobró de cada material. */
    private static final class Run {
        final StructureDefinition def;
        final PlacementPlan plan;
        final Map<String, Integer> charged = new HashMap<>();
        int countdown;
        long lastSave;
        /** Registro de obra terminada en vuelo, intentos fallidos y cuándo fue el último. */
        boolean completing;
        int completeFailures;
        long lastComplete;
        /** Última comprobación de la protección de la zona. */
        long lastGuard;
        /** Mejora: bloques de la construcción original por posición ({@link #key}); vacío en las demás. */
        final Map<Long, BlockData> own;

        Run(StructureDefinition def, PlacementPlan plan, Map<Long, BlockData> own, long now) {
            this.def = def;
            this.plan = plan;
            this.own = own;
            this.lastSave = now - SAVE_EVERY_MS;
            this.lastComplete = now;
            this.lastGuard = now;
            this.countdown = Math.max(1, def.ticksPerCycle());
        }
    }

    /** Plan que se calcula fuera del hilo principal; uno a la vez. */
    private record Planning(UUID project, StructureDefinition def, CompletableFuture<Planned> plan) {
    }

    /** El plan y, en una mejora, los bloques de la construcción original. */
    private record Planned(PlacementPlan plan, Map<Long, BlockData> own) {
    }

    /** Un CONSUME del lote del tick, con lo necesario para deshacerlo en memoria si no entra. */
    private record Charge(Project project, Run run, Movement move) {
    }

    private final ProjectService projects;
    private final ProjectRepository repo;
    private final Function<String, Optional<StructureDefinition>> defs;
    private final Supplier<PerformanceSettings> perf;
    private final BlockWriter writer;
    private final Consumer<Runnable> main;
    private final BiConsumer<Project, BuildRecord> completed;
    private final Consumer<Project> paused;
    private final LongSupplier clock;
    private final Executor planner;
    private final Predicate<UUID> benchLoaded;
    private final AreaGuard guard;
    private final Precheck precheck;
    private final Logger log;
    /** Clave del último {@link StartResult#MODULE_DENIED} de {@link #start}. */
    private String deniedKey;
    private final Map<UUID, Run> runs = new HashMap<>();
    /** Obras cuya estructura no está en el catálogo: se avisa una vez y esperan. */
    private final Set<UUID> missing = new HashSet<>();
    /**
     * Obras arrancadas o reanudadas por alguien con bypass: no se les vuelve a mirar la protección. Solo en memoria:
     * tras un reinicio se comprueban otra vez y, si la zona está protegida, se pausan hasta que alguien con bypass
     * las reanude.
     */
    private final Set<UUID> bypassed = new HashSet<>();
    private final List<Charge> charges = new ArrayList<>();
    private Planning planning;
    private BuildBudget budget;
    private int budgetSize = -1;

    /**
     * @param main      vuelve al hilo principal desde el de la base de datos
     * @param completed   obra terminada y registrada, con la construcción tal como quedó en la base de datos:
     *                    mensaje al dueño, partículas, sonido y {@code onBuilt} del módulo. En una REPAIR la
     *                    construcción es null: no hay otra nueva y su módulo ya está; en una UPGRADE es la nueva,
     *                    que ya reemplazó a la vieja ({@link Project#buildId()})
     * @param paused      obra pausada por el motor (motivo en {@link Project#error()}): aviso al dueño si está
     * @param planner     dónde se calcula el plan (girar y ordenar): fuera del hilo principal
     * @param benchLoaded ¿está cargado el chunk de la mesa de la obra? false también si la mesa es huérfana
     * @param guard       protección de la zona entera, por el dueño de la obra: al arrancar, al instalar el plan, al
     *                    reanudar y cada {@code protection-recheck-seconds}
     * @param precheck    el del módulo de la estructura, al arrancar (tras la protección)
     */
    public ConstructionEngine(ProjectService projects, ProjectRepository repo,
                              Function<String, Optional<StructureDefinition>> defs, Supplier<PerformanceSettings> perf,
                              BlockWriter writer, Consumer<Runnable> main, BiConsumer<Project, BuildRecord> completed,
                              Consumer<Project> paused, LongSupplier clock, Executor planner,
                              Predicate<UUID> benchLoaded, AreaGuard guard, Precheck precheck, Logger log) {
        this.projects = projects;
        this.repo = repo;
        this.defs = defs;
        this.perf = perf;
        this.writer = writer;
        this.main = main;
        this.completed = completed;
        this.paused = paused;
        this.clock = clock;
        this.planner = planner;
        this.benchLoaded = benchLoaded;
        this.guard = guard;
        this.precheck = precheck;
        this.log = log;
    }

    /**
     * READY → BUILDING si hay permiso, la zona no está protegida para el dueño de la obra (salvo bypass de quien
     * arranca) y hay sitio en los límites; las obras en pausa siguen ocupando su sitio. El plan se calcula luego, en
     * {@link #tick} (uno nuevo por tick como mucho).
     */
    public StartResult start(Player p, Project project) {
        if (project.state() != ProjectState.READY || project.busy()) {
            return StartResult.NOT_READY;
        }
        Optional<StructureDefinition> def = defs.apply(project.structure());
        if (def.isEmpty()) {
            return StartResult.NOT_READY;
        }
        if (!p.hasPermission(BUILD_PERMISSION) || !StructurePerms.allowed(p, def.get().permission())) {
            return StartResult.NO_PERMISSION;
        }
        boolean bypass = p.hasPermission(WorkbenchService.PERM_BYPASS);
        if (!bypass && denied(project, def.get())) {
            return StartResult.PROTECTED;
        }
        // Una reparación va sobre una construcción con su módulo ya en marcha (p. ej. el terreno de la parcela).
        Optional<String> why = project.kind() == ProjectKind.REPAIR ? Optional.empty()
                : precheck.check(def.get(), project.owner(), project.world(), box(project, def.get().blocks()));
        if (why.isPresent()) {
            deniedKey = why.get();
            return StartResult.MODULE_DENIED;
        }
        PerformanceSettings s = perf.get();
        int server = 0, world = 0, player = 0;
        for (Project o : projects.all()) {
            if (o.state() == ProjectState.BUILDING || o.state() == ProjectState.PAUSED) {
                server++;
                world += o.world().equals(project.world()) ? 1 : 0;
                player += o.owner().equals(project.owner()) ? 1 : 0;
            }
        }
        if (server >= s.maxProjectsServer()) {
            return StartResult.LIMIT_SERVER;
        }
        if (world >= s.maxProjectsWorld()) {
            return StartResult.LIMIT_WORLD;
        }
        if (player >= s.maxProjectsPlayer()) {
            return StartResult.LIMIT_PLAYER;
        }
        markBypass(project, bypass);
        project.setState(ProjectState.BUILDING);
        save(project);
        return StartResult.STARTED;
    }

    /** Clave de idioma del último {@link StartResult#MODULE_DENIED} (hilo principal: léase justo tras start). */
    public String deniedKey() {
        return deniedKey;
    }

    /** Pausa a mano (dueño, dueño de la mesa o bypass). */
    public void pause(Project project) {
        pause(project, PAUSED_PLAYER);
    }

    /** BUILDING → PAUSED con el motivo ({@link #PAUSED_BENCH}...); una obra ocupada o en otro estado no cambia. */
    public void pause(Project project, String reason) {
        if (project.state() == ProjectState.BUILDING && !project.busy()) {
            project.setState(ProjectState.PAUSED);
            project.setError(reason);
            save(project);
        }
    }

    /** {@link #resume(Project, boolean)} sin bypass. */
    public StartResult resume(Project project) {
        return resume(project, false);
    }

    /**
     * PAUSED → BUILDING si la zona no está protegida para el dueño de la obra (salvo {@code bypass} de quien
     * reanuda); borra el motivo de la pausa. Si está protegida sigue PAUSED con {@link #PAUSED_PROTECTED}.
     */
    public StartResult resume(Project project, boolean bypass) {
        if (project.state() != ProjectState.PAUSED || project.busy()) {
            return StartResult.NOT_READY;
        }
        Run run = runs.get(project.id());
        Optional<StructureDefinition> def = run != null ? Optional.of(run.def) : defs.apply(project.structure());
        if (def.isEmpty()) {
            return StartResult.NOT_READY;
        }
        if (!bypass && denied(project, def.get())) {
            project.setError(PAUSED_PROTECTED);
            save(project);
            return StartResult.PROTECTED;
        }
        markBypass(project, bypass);
        if (run != null) {
            run.lastGuard = clock.getAsLong();
        }
        project.setState(ProjectState.BUILDING);
        project.setError(null);
        save(project);
        return StartResult.STARTED;
    }

    public void tick() {
        PerformanceSettings s = perf.get();
        if (budget == null || budgetSize != s.blocksPerTick()) {
            budget = new BuildBudget(s.blocksPerTick());
            budgetSize = s.blocksPerTick();
        }
        List<Project> all = projects.all();
        all.sort(Comparator.comparing(Project::id)); // orden estable para el reparto por turnos
        Set<UUID> alive = new HashSet<>();
        all.forEach(p -> alive.add(p.id()));
        runs.keySet().retainAll(alive); // canceladas o terminadas fuera del motor
        missing.retainAll(alive);
        bypassed.retainAll(alive);
        if (planning != null && projects.byId(planning.project())
                .filter(p -> p.state() == ProjectState.BUILDING).isEmpty()) {
            planning = null; // cancelada, terminada o en pausa mientras se calculaba: deja sitio a otra
        }

        List<UUID> ready = new ArrayList<>();
        Map<UUID, Integer> wanted = new HashMap<>();
        Map<UUID, Project> byId = new HashMap<>();
        boolean planned = false; // un Run nuevo por tick: al arrancar no se instalan todos a la vez
        long now = clock.getAsLong();
        for (Project p : all) {
            if (p.state() != ProjectState.BUILDING || p.busy()) {
                continue;
            }
            if (!benchLoaded.test(p.bench())) {
                continue; // espera a la mesa como al chunk: sin cambiar de estado ni quitar presupuesto
            }
            Run run = runs.get(p.id());
            boolean installed = run == null;
            if (installed) {
                run = plan(p, !planned);
                if (run == null) {
                    continue;
                }
                planned = true;
            }
            int left = run.plan.size() - p.cursor();
            if (left <= 0) {
                // Terminada sin registrar (caída o fallo de la base de datos): reintento con espera creciente.
                if (!run.completing && now - run.lastComplete >= completeDelay(run.completeFailures)) {
                    complete(p, run);
                }
                continue;
            }
            // Al instalar (arranque del servidor, primera vez) y cada protection-recheck-seconds.
            if (installed || now - run.lastGuard >= s.protectionRecheckSeconds() * 1000L) {
                run.lastGuard = now;
                if (guarded(p, run.def)) {
                    continue;
                }
            }
            if (!run.def.instant()) {
                if (--run.countdown > 0) {
                    continue;
                }
                run.countdown = Math.max(1, run.def.ticksPerCycle());
            }
            SourceBlock next = run.plan.at(p.cursor());
            if (!writer.chunkLoaded(p.world(), p.x() + next.x(), p.z() + next.z())) {
                continue; // espera al chunk sin quitar presupuesto a las demás
            }
            int want = run.def.instant() ? Integer.MAX_VALUE : run.def.blocksPerCycle();
            ready.add(p.id());
            wanted.put(p.id(), Math.min(want, left));
            byId.put(p.id(), p);
        }
        budget.share(ready, wanted).forEach((id, n) -> {
            Project p = byId.get(id);
            Run run = runs.get(id);
            // El estado se vuelve a mirar: una cancelación o pausa en este mismo tick no escribe ni cobra.
            if (n > 0 && run != null && p.state() == ProjectState.BUILDING && !p.busy()) {
                place(p, run, n);
            }
        });
        flushCharges();
    }

    /** Hilo principal, al apagar: guarda el cursor de cada obra en curso y espera (5 s) a la base de datos. */
    public void flushAll() {
        flushCharges();
        List<CompletableFuture<Boolean>> saves = new ArrayList<>();
        for (Project p : projects.all()) {
            if (runs.containsKey(p.id())
                    && (p.state() == ProjectState.BUILDING || p.state() == ProjectState.PAUSED)) {
                saves.add(save(p));
            }
        }
        try {
            // La base de datos va en orden: tras estos guardados ya están también los CONSUME anteriores.
            CompletableFuture.allOf(saves.toArray(CompletableFuture[]::new)).get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warning("No se terminó de guardar el progreso de las obras en 5 s; se retoman desde el último cursor.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.log(Level.SEVERE, "Fallo al guardar el progreso de las obras al apagar", e);
        }
    }

    /**
     * Plan de la obra. Si no hay ninguno en cálculo, empieza el suyo en {@code planner}; si el suyo ya está y
     * {@code install}, lo instala (hilo principal) y lo devuelve. null mientras tanto, si la estructura no está o si la
     * obra pasó a ERROR.
     */
    private Run plan(Project p, boolean install) {
        if (planning == null) {
            startPlan(p);
        }
        if (!install || planning == null || !planning.project().equals(p.id()) || !planning.plan().isDone()) {
            return null;
        }
        Planning done = planning;
        planning = null;
        Planned planned;
        try {
            planned = done.plan().join();
        } catch (CompletionException | CancellationException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fail(p, "plan: " + cause.getMessage(), cause);
            return null;
        }
        PlacementPlan plan = planned.plan();
        if (plan.size() != p.total()) {
            // El cursor es un índice del plan: con otro número de bloques ya no señala lo mismo.
            fail(p, "la estructura cambió (" + p.total() + " → " + plan.size() + " bloques)", null);
            return null;
        }
        Run run = new Run(done.def(), plan, planned.own(), clock.getAsLong());
        p.materials().forEach((key, row) -> run.charged.put(key, row.consumed()));
        runs.put(p.id(), run);
        return run;
    }

    /** ¿Niega la protección la zona entera al dueño de la obra? */
    private boolean denied(Project p, StructureDefinition def) {
        return guard.check(p.owner(), p.world(), box(p, def.blocks())).isPresent();
    }

    /** Sin bypass y con la zona protegida: PAUSED con {@link #PAUSED_PROTECTED}, guardada y aviso al dueño. */
    private boolean guarded(Project p, StructureDefinition def) {
        if (bypassed.contains(p.id()) || !denied(p, def)) {
            return false;
        }
        log.info("La obra " + p.id() + " (" + p.structure() + ") se pausa: la zona está protegida para su dueño.");
        p.setState(ProjectState.PAUSED);
        p.setError(PAUSED_PROTECTED);
        save(p);
        try {
            paused.accept(p);
        } catch (RuntimeException e) {
            log.log(Level.WARNING, "Aviso de obra pausada " + p.id() + " con error", e);
        }
        return true;
    }

    private void markBypass(Project p, boolean bypass) {
        if (bypass) {
            bypassed.add(p.id());
        } else {
            bypassed.remove(p.id());
        }
    }

    private void startPlan(Project p) {
        Optional<StructureDefinition> def = defs.apply(p.structure());
        if (def.isEmpty()) {
            if (missing.add(p.id())) {
                log.warning("La obra " + p.id() + " es de la estructura " + p.structure()
                        + ", que no está en el catálogo; espera hasta que vuelva.");
            }
            return;
        }
        StructureDefinition d = def.get();
        // Mismo volumen y otros bloques: el cursor ya no señala lo mismo. Obras viejas (sin hash): sin comprobar. Con
        // el cursor al final ya está todo puesto: solo falta registrarla, el esquemático da igual.
        if (p.cursor() < p.total() && p.sourceHash() != null && d.sourceHash() != null
                && !p.sourceHash().equals(d.sourceHash())) {
            fail(p, "el esquemático de la estructura cambió desde que se creó la obra", null);
            return;
        }
        // Mejora: la estructura original, en el mismo origen y giro, dice qué bloques se pueden pisar.
        StructureDefinition old = p.kind() == ProjectKind.UPGRADE && d.upgradeFrom() != null
                ? defs.apply(d.upgradeFrom()).orElse(null) : null;
        if (p.kind() == ProjectKind.UPGRADE && old == null) {
            log.warning("La mejora " + p.id() + " no encuentra la estructura original " + d.upgradeFrom()
                    + " en el catálogo: solo escribe en huecos, sin pisar nada.");
        }
        CompletableFuture<Planned> f;
        try {
            // Lógica pura sobre SourceBlock: girar (clona cada BlockData) y ordenar no tocan el mundo.
            f = CompletableFuture.supplyAsync(() -> new Planned(new PlacementPlan(BuildOrders.order(
                    RotatedCache.get(d, p.rotation()), d.order(), d.customOrder())),
                    old == null ? Map.of() : own(RotatedCache.get(old, p.rotation()), ProjectService.upgradeShift(
                            p.rotation(), old.blocks().width(), old.blocks().length(), d.blocks().width(),
                            d.blocks().length()))), planner);
        } catch (RuntimeException e) {
            f = CompletableFuture.failedFuture(e);
        }
        planning = new Planning(p.id(), d, f);
    }

    /** Bloques de la original, relativos al origen de la mejora (el suyo menos {@code shift}). */
    private static Map<Long, BlockData> own(List<SourceBlock> blocks, int[] shift) {
        Map<Long, BlockData> out = new HashMap<>();
        blocks.forEach(b -> out.put(key(b.x() - shift[0], b.y(), b.z() - shift[1]), b.data()));
        return out;
    }

    private static long key(SourceBlock b) {
        return key(b.x(), b.y(), b.z());
    }

    /** Posición relativa al origen (puede ser negativa); 21 bits por eje, de sobra para cualquier caja. */
    private static long key(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }

    /** Espera antes del siguiente intento de registrar la obra, tras {@code failures} fallos seguidos. */
    static long completeDelay(int failures) {
        return failures <= 0 ? 0 : COMPLETE_BACKOFF_MS[Math.min(failures, COMPLETE_BACKOFF_MS.length) - 1];
    }

    private void place(Project p, Run run, int n) {
        int cursor = p.cursor(), placed = p.placed(), skipped = p.skipped();
        int end = Math.min(run.plan.size(), cursor + n);
        Set<Material> replace = run.def.placement() == null ? Set.of() : run.def.placement().replace();
        // Reparación: placed cuenta solo lo escrito, y no se escribe más que lo pagado (sobrevive al reinicio).
        boolean repair = p.kind() == ProjectKind.REPAIR;
        try {
            while (cursor < end) {
                SourceBlock b = run.plan.at(cursor);
                int x = p.x() + b.x(), y = p.y() + b.y(), z = p.z() + b.z();
                if (!writer.chunkLoaded(p.world(), x, z)) {
                    break; // espera a que el chunk cargue; el cursor se queda aquí
                }
                BlockWriter.BlockInfo now = writer.read(p.world(), x, y, z);
                if (b.data().equals(now.data())) {
                    placed += repair ? 0 : 1; // ya está (p. ej. al retomar tras una caída)
                } else if (repair && placed >= p.repairBlocks()) {
                    skipped++; // hueco que no se pagó: nunca gratis
                } else if (isAir(now.type()) || now.replaceable() || replace.contains(now.type())
                        || (now.data() != null && now.data().equals(run.own.get(key(b)))
                        && !writer.keepsState(p.world(), x, y, z))) {
                    // Lo último: en una mejora, un bloque que sigue siendo el de la construcción original, salvo
                    // que guarde algo (cofre con ítems, cartel): se perdería al escribir encima.
                    writer.write(p.world(), x, y, z, b);
                    placed++;
                } else {
                    skipped++; // bloque de otro: nunca se pisa
                }
                cursor++;
            }
        } catch (RuntimeException e) {
            p.setProgress(cursor, placed, skipped);
            consume(p, run);
            fail(p, "bloque " + cursor + ": " + e, e);
            return;
        }
        if (cursor == p.cursor()) {
            return;
        }
        p.setProgress(cursor, placed, skipped);
        consume(p, run);
        if (cursor >= run.plan.size()) {
            complete(p, run);
        } else if (clock.getAsLong() - run.lastSave >= SAVE_EVERY_MS) {
            save(p);
        }
    }

    /**
     * Añade al lote del tick lo que toca cobrar hasta el cursor actual. Id derivado de obra, material y cursor:
     * repetirlo no cobra.
     */
    private void consume(Project p, Run run) {
        DepositMath.consumedAt(p.materials(), p.cursor(), p.total()).forEach((key, target) -> {
            int delta = target - run.charged.getOrDefault(key, 0);
            if (delta <= 0) {
                return;
            }
            run.charged.merge(key, delta, Integer::sum);
            addConsumed(p, key, delta);
            UUID id = UUID.nameUUIDFromBytes((p.id() + ":" + key + ":" + p.cursor()).getBytes(StandardCharsets.UTF_8));
            charges.add(new Charge(p, run, new Movement(id, p.id(), p.owner(), Movement.Kind.CONSUME, key, delta)));
        });
    }

    /** Envía el lote de CONSUME en una transacción; lo que no entre se descuenta en memoria y se recobra luego. */
    private void flushCharges() {
        if (charges.isEmpty()) {
            return;
        }
        List<Charge> batch = List.copyOf(charges);
        charges.clear();
        CompletableFuture<List<Boolean>> f;
        try {
            f = repo.applyMovements(batch.stream().map(Charge::move).toList());
        } catch (RuntimeException e) {
            f = CompletableFuture.failedFuture(e);
        }
        f.whenComplete((oks, e) -> {
            List<Charge> undo = new ArrayList<>();
            for (int i = 0; i < batch.size(); i++) {
                if (e != null || oks == null || i >= oks.size() || !oks.get(i)) {
                    undo.add(batch.get(i));
                }
            }
            if (undo.isEmpty()) {
                return;
            }
            log.log(e != null ? Level.SEVERE : Level.WARNING, undo.size() + " CONSUME de " + batch.size()
                    + " no se apuntaron; se descuentan y se vuelven a cobrar con el siguiente avance", e);
            main.accept(() -> undo.forEach(c -> {
                c.run().charged.merge(c.move().materialKey(), -c.move().amount(), Integer::sum);
                addConsumed(c.project(), c.move().materialKey(), -c.move().amount());
            }));
        });
    }

    private static void addConsumed(Project p, String key, int delta) {
        Map<String, MaterialRow> rows = new LinkedHashMap<>(p.materials());
        MaterialRow r = rows.get(key);
        rows.put(key, new MaterialRow(r.required(), r.deposited(), r.consumed() + delta));
        p.setMaterials(rows);
    }

    /** Caja girada de la obra desde su esquina mínima. */
    private static Box box(Project p, BlockSource src) {
        int w = p.rotation().rotatedWidth(src.width(), src.length());
        int l = p.rotation().rotatedLength(src.width(), src.length());
        return new Box(p.x(), p.y(), p.z(), p.x() + w - 1, p.y() + src.height() - 1, p.z() + l - 1);
    }

    /**
     * Cobra lo que falte, guarda el cursor final y registra la obra. Solo si la base de datos lo confirma pasa a
     * COMPLETED y sale de la memoria; si no, sigue BUILDING (su caja y su mesa siguen ocupadas) y {@link #tick} lo
     * reintenta con espera creciente ({@link #COMPLETE_BACKOFF_MS}).
     */
    private void complete(Project p, Run run) {
        if (p.kind() == ProjectKind.REPAIR && p.buildId() == null) {
            fail(p, "reparación sin construcción (build_id)", null); // no hay nada que actualizar: no se reintenta
            return;
        }
        run.completing = true;
        run.lastComplete = clock.getAsLong();
        consume(p, run);
        flushCharges(); // el último CONSUME va antes del registro
        save(p);
        long at = System.currentTimeMillis();
        // Una reparación no crea construcción: la que ya había toma la fecha nueva y su módulo sigue igual.
        BuildRecord rec = p.kind() == ProjectKind.REPAIR ? null : new BuildRecord(UUID.randomUUID(), p.id(),
                p.structure(), p.owner(), p.world(), p.x(), p.y(), p.z(), p.rotation(), box(p, run.def.blocks()), at,
                p.sourceHash());
        CompletableFuture<Boolean> f;
        try {
            f = rec == null ? repo.completeRepair(p.id(), p.buildId(), at)
                    : p.kind() == ProjectKind.UPGRADE && p.buildId() != null
                    ? repo.completeUpgrade(p.id(), p.buildId(), rec) : repo.complete(p.id(), rec);
        } catch (RuntimeException e) {
            f = CompletableFuture.failedFuture(e);
        }
        f.whenComplete((ok, e) -> main.accept(() -> {
            run.completing = false;
            if (e == null && Boolean.TRUE.equals(ok)) {
                p.setState(ProjectState.COMPLETED);
                runs.remove(p.id());
                projects.finish(p);
                try {
                    completed.accept(p, rec);
                } catch (RuntimeException ex) {
                    log.log(Level.WARNING, "Aviso de obra terminada " + p.id() + " con error", ex);
                }
                return;
            }
            if (projects.byId(p.id()).isEmpty() || p.state() != ProjectState.BUILDING) {
                return; // cancelada mientras tanto
            }
            run.completeFailures++;
            log.log(Level.WARNING, "No se pudo registrar la obra terminada " + p.id() + " (intento "
                    + run.completeFailures + "); se reintenta en " + completeDelay(run.completeFailures) / 1000
                    + " s", e);
        }));
    }

    private void fail(Project p, String reason, Throwable e) {
        String msg = reason.length() > 120 ? reason.substring(0, 120) : reason;
        log.log(Level.SEVERE, "La obra " + p.id() + " (" + p.structure() + ") pasa a ERROR: " + msg, e);
        p.setState(ProjectState.ERROR);
        p.setError(msg);
        runs.remove(p.id());
        save(p);
    }

    private CompletableFuture<Boolean> save(Project p) {
        Run run = runs.get(p.id());
        if (run != null) {
            run.lastSave = clock.getAsLong();
        }
        CompletableFuture<Boolean> f = repo.saveProgress(p.id(), p.state(), p.cursor(), p.placed(), p.skipped(),
                p.error());
        f.whenComplete((ok, e) -> {
            if (e != null) {
                log.log(Level.SEVERE, "No se pudo guardar el progreso de la obra " + p.id(), e);
            } else if (!ok) {
                log.warning("El progreso de " + p.id() + " no se guardó (obra inexistente o ya final).");
            }
        });
        return f;
    }

    /** {@code Material.isAir()} pide el registro del servidor; así sirve también en las pruebas. */
    private static boolean isAir(Material m) {
        return m == Material.AIR || m == Material.CAVE_AIR || m == Material.VOID_AIR;
    }
}
