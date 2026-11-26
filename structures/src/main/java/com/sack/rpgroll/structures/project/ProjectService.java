package com.sack.rpgroll.structures.project;

import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.structures.catalog.MaterialSpec;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.deposit.MoneyService;
import com.sack.rpgroll.structures.deposit.MoneyService.ChargeResult;
import com.sack.rpgroll.structures.storage.Box;
import com.sack.rpgroll.structures.storage.BuildRecord;
import com.sack.rpgroll.structures.storage.MaterialRow;
import com.sack.rpgroll.structures.storage.ProjectRecord;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import com.sack.rpgroll.structures.workbench.WorkbenchState;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Obras activas en memoria, respaldadas por {@link ProjectRepository}. La memoria se toca en el hilo principal
 * ({@code main}); la base de datos corre en su hilo y su resultado vuelve por {@code main}.
 */
public final class ProjectService {

    private final ProjectRepository repo;
    private final Consumer<Runnable> main;
    private final Logger log;
    private final Function<String, List<MaterialSpec>> specsOf;
    /** null: sin cobro posible (una estructura con dinero no se crea). */
    private final MoneyService money;
    private final Map<UUID, Project> byId = new ConcurrentHashMap<>();
    private final Map<UUID, Project> byBench = new ConcurrentHashMap<>();
    /** Caja de una obra en un mundo. */
    private record Placed(String world, Box box) {
        boolean hits(Placed o) {
            return world.equals(o.world) && box.intersects(o.box);
        }
    }

    /** El dinero de la estructura no se cobró: no se creó nada. */
    public static final class ChargeException extends RuntimeException {
        private final ChargeResult result;

        public ChargeException(ChargeResult result) {
            super("Cobro no hecho: " + result);
            this.result = result;
        }

        public ChargeResult result() {
            return result;
        }
    }

    /** No se repara: {@link #key()} es la clave de idioma del motivo. */
    public static final class RepairException extends RuntimeException {
        public static final String CHANGED = "repair.changed";
        public static final String NOTHING = "repair.nothing";
        public static final String VANISHED = "repair.vanished";
        public static final String NOT_LOADED = "repair.not-loaded";
        /** Mejora de una construcción cuyo registro ya no está (o no es del dueño). */
        public static final String UPGRADE_GONE = "build-upgrade.gone";

        private final String key;

        public RepairException(String key) {
            super("Reparación rechazada: " + key);
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    /** La caja nueva choca con una obra activa o con otra que aún se está guardando. */
    public static final class OverlapException extends IllegalStateException {
        public OverlapException(String message) {
            super(message);
        }
    }

    /**
     * Mesas con un insert en curso y la caja que reservan: impide una segunda obra en la mesa, y otra que se cruce
     * en cualquier mesa, antes de que la primera llegue a la memoria.
     */
    private final Map<UUID, Placed> creating = new ConcurrentHashMap<>();
    /** Caja de cada obra activa en memoria, por id de obra (las terminadas las mira el check contra la base). */
    private final Map<UUID, Placed> boxes = new ConcurrentHashMap<>();
    /** Mesas cuya obra acaba de terminar; siguen en COMPLETED hasta que alguien abre el menú. */
    private final Set<UUID> completed = ConcurrentHashMap.newKeySet();

    public ProjectService(ProjectRepository repo, Consumer<Runnable> main, Logger log) {
        this(repo, main, log, id -> List.of());
    }

    /** {@code specsOf}: materiales de una estructura por id (vacío si no se conoce); sirve para sanar obras al cargar. */
    public ProjectService(ProjectRepository repo, Consumer<Runnable> main, Logger log,
                          Function<String, List<MaterialSpec>> specsOf) {
        this(repo, main, log, specsOf, null);
    }

    /** {@code money}: cobro del {@code money:} de la estructura al confirmar. */
    public ProjectService(ProjectRepository repo, Consumer<Runnable> main, Logger log,
                          Function<String, List<MaterialSpec>> specsOf, MoneyService money) {
        this.repo = repo;
        this.main = main;
        this.log = log;
        this.specsOf = specsOf;
        this.money = money;
    }

    /**
     * Lee las obras activas y sus materiales. Bloquea: se llama al arrancar. Una obra BUILDING sigue en BUILDING: el
     * motor la retoma desde su cursor guardado.
     */
    public void load() {
        Map<UUID, Box> saved = repo.activeBoxes().join();
        for (ProjectRecord r : repo.active().join()) {
            Project p = new Project(r, repo.materials(r.id()).join());
            // Una obra que quedó en WAITING_MATERIALS con todo depositado (cayó antes de guardar READY) se sana aquí.
            List<MaterialSpec> specs = specsOf.apply(r.structure());
            List<String> extra = DepositMath.missingRows(specs, p.materials());
            if (!extra.isEmpty()) {
                log.warning("La obra " + p.id() + " (" + r.structure() + ") no tiene " + extra
                        + ": no formaban parte de su precio y se dan por cumplidos.");
            }
            if (r.state() == ProjectState.WAITING_MATERIALS && !specs.isEmpty()
                    && DepositMath.complete(specs, p.materials())) {
                p.setState(ProjectState.READY);
                try {
                    repo.saveProgress(p.id(), ProjectState.READY, p.cursor(), p.placed(), p.skipped(), p.error()).join();
                } catch (RuntimeException e) {
                    log.log(Level.WARNING, "No se pudo guardar READY al sanar la obra " + p.id(), e);
                }
            }
            // ERROR con el cursor al final: no se pudo registrar (antes se rendía a los 3 fallos). El motor reintenta.
            if (r.state() == ProjectState.ERROR && r.total() > 0 && r.cursor() == r.total()) {
                p.setState(ProjectState.BUILDING);
                p.setError(null);
                try {
                    repo.saveProgress(p.id(), ProjectState.BUILDING, p.cursor(), p.placed(), p.skipped(), null).join();
                } catch (RuntimeException e) {
                    log.log(Level.WARNING, "No se pudo guardar BUILDING al reintentar la obra " + p.id(), e);
                }
            }
            Box box = saved.get(r.id());
            if (box != null) {
                boxes.put(r.id(), new Placed(r.world(), box));
            }
            add(p);
        }
    }

    private void add(Project p) {
        byId.put(p.id(), p);
        byBench.put(p.bench(), p);
    }

    public boolean hasActive(UUID bench) {
        return byBench.containsKey(bench);
    }

    public Optional<Project> forBench(UUID bench) {
        return Optional.ofNullable(byBench.get(bench));
    }

    public Optional<Project> byId(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** Todas las obras activas en memoria (cualquier estado salvo los finales). */
    public List<Project> all() {
        return new ArrayList<>(byId.values());
    }

    public List<Project> ofPlayer(UUID owner) {
        return new ArrayList<>(byId.values().stream().filter(p -> p.owner().equals(owner)).toList());
    }

    /** Sin jugador que pague: solo estructuras sin dinero (si no, {@link ChargeException} UNAVAILABLE). */
    public CompletableFuture<Project> create(UUID bench, StructureDefinition def, UUID owner, Location origin,
                                             Rotation r) {
        return create(bench, def, owner, null, origin, r);
    }

    /**
     * Hilo principal. Crea una obra de {@code payer} en WAITING_MATERIALS. {@code origin} es la esquina mínima de la
     * caja ya girada. El dinero de la estructura se cobra entero aquí, ANTES del alta (el MONEY_CHARGE va en su
     * misma transacción); si el alta falla, se devuelve.
     *
     * @throws IllegalStateException si la mesa ya tiene una obra activa (o una en creación)
     * @throws OverlapException       si la caja se cruza con una obra activa o en creación (en otra mesa también)
     * @throws ChargeException        si el dinero no se pudo cobrar (no se crea nada)
     */
    public CompletableFuture<Project> create(UUID bench, StructureDefinition def, Player payer, Location origin,
                                             Rotation r) {
        return create(bench, def, payer.getUniqueId(), payer, origin, r);
    }

    private CompletableFuture<Project> create(UUID bench, StructureDefinition def, UUID owner, Player payer,
                                              Location origin, Rotation r) {
        Map<String, Integer> required = new LinkedHashMap<>();
        for (MaterialSpec spec : def.materials()) {
            required.merge(spec.key(), spec.amount(), Integer::sum);
        }
        long now = System.currentTimeMillis();
        return insertNew(new ProjectRecord(UUID.randomUUID(), bench, def.id(), owner, ProjectState.WAITING_MATERIALS,
                origin.getWorld().getName(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), r, 0, 0, 0,
                def.volume(), null, now, now, def.sourceHash(), MoneyService.cents(def.money())), def, required, payer);
    }

    /** {@link #createRepair(UUID, StructureDefinition, BuildRecord, Map, int, Player, long)} sin dinero. */
    public CompletableFuture<Project> createRepair(UUID bench, StructureDefinition def, BuildRecord build,
                                                   Map<String, Integer> price, int blocks) {
        return createRepair(bench, def, build, price, blocks, null, 0);
    }

    /**
     * Hilo principal. Obra REPAIR de {@code build} (de su dueño, con su origen y su giro) en WAITING_MATERIALS, con
     * {@code price} como materiales y {@code cents} como dinero (cobrado a {@code payer} como en {@link #create}). El
     * plan es el de la estructura entera: el motor salta lo que ya está y lo que puso otro, y no escribe más que los
     * {@code blocks} pagados (lo que se rompa después de crearla no vuelve gratis). El choque es el de
     * {@link #create}: la caja de esa construcción no entra (las terminadas solo cuentan en la vista previa); la de
     * cualquier obra activa o en creación sí, también la de otra reparación de la misma construcción.
     *
     * @throws RepairException {@code repair.changed} si la estructura ya no es la de la construcción
     *                         ({@link RepairMath#matches}); y las mismas que {@link #create}
     */
    public CompletableFuture<Project> createRepair(UUID bench, StructureDefinition def, BuildRecord build,
                                                   Map<String, Integer> price, int blocks, Player payer,
                                                   long cents) {
        if (blocks <= 0) {
            throw new IllegalArgumentException("Una reparación paga al menos un bloque: " + blocks);
        }
        if (!def.id().equals(build.structure())) {
            throw new IllegalArgumentException("La construcción " + build.id() + " no es de " + def.id());
        }
        if (!RepairMath.matches(def, build)) {
            throw new RepairException(RepairException.CHANGED);
        }
        long now = System.currentTimeMillis();
        return insertNew(new ProjectRecord(UUID.randomUUID(), bench, def.id(), build.owner(),
                ProjectState.WAITING_MATERIALS, build.world(), build.x(), build.y(), build.z(), build.rotation(), 0, 0,
                0, def.volume(), null, now, now, def.sourceHash(), cents, ProjectKind.REPAIR, build.id(), blocks), def,
                new LinkedHashMap<>(price), payer);
    }

    /**
     * {@link #createUpgrade(UUID, StructureDefinition, BuildRecord, StructureDefinition, Player)} sin quien pague
     * (solo sin dinero).
     */
    public CompletableFuture<Project> createUpgrade(UUID bench, StructureDefinition target, BuildRecord from,
                                                    StructureDefinition original) {
        return createUpgrade(bench, target, from, original, null);
    }

    /**
     * Cuánto se mueve (x, z) el origen de una mejora respecto al de la construcción para que la esquina local (0,0)
     * de las dos estructuras caiga en el mismo bloque del mundo: cada una se gira con sus propias medidas, así que con
     * giro y otro tamaño sus esquinas mínimas no coinciden.
     */
    public static int[] upgradeShift(Rotation r, int oldWidth, int oldLength, int newWidth, int newLength) {
        int[] a = r.apply(0, 0, oldWidth, oldLength), b = r.apply(0, 0, newWidth, newLength);
        return new int[] {a[0] - b[0], a[1] - b[1]};
    }

    /**
     * Hilo principal. Obra UPGRADE de {@code from} (de su dueño, con su giro, y el origen movido según
     * {@link #upgradeShift} con las medidas de {@code original}, la estructura de {@code from}) hacia {@code target}, en
     * WAITING_MATERIALS: el precio es el entero de {@code target} (materiales y dinero, cobrado a {@code payer} como
     * en {@link #create}). El motor solo pisa los bloques que siguen siendo de la construcción original. Choca con
     * las obras activas o en creación (aquí) y con las construcciones terminadas salvo {@code from} (en la misma
     * transacción del alta: el futuro falla con {@link OverlapException}; si {@code from} ya no está registrada, con
     * {@link RepairException} {@link RepairException#UPGRADE_GONE}).
     *
     * @throws IllegalArgumentException si {@code target} no declara {@code upgrade_from} igual a la estructura de
     *                                  {@code from}; y las mismas que {@link #create}
     */
    public CompletableFuture<Project> createUpgrade(UUID bench, StructureDefinition target, BuildRecord from,
                                                    StructureDefinition original, Player payer) {
        if (!from.structure().equals(target.upgradeFrom())) {
            throw new IllegalArgumentException(target.id() + " no mejora " + from.structure());
        }
        Map<String, Integer> required = new LinkedHashMap<>();
        for (MaterialSpec spec : target.materials()) {
            required.merge(spec.key(), spec.amount(), Integer::sum);
        }
        int[] shift = upgradeShift(from.rotation(), original.blocks().width(), original.blocks().length(),
                target.blocks().width(), target.blocks().length());
        long now = System.currentTimeMillis();
        return insertNew(new ProjectRecord(UUID.randomUUID(), bench, target.id(), from.owner(),
                ProjectState.WAITING_MATERIALS, from.world(), from.x() + shift[0], from.y(), from.z() + shift[1],
                from.rotation(), 0, 0, 0,
                target.volume(), null, now, now, target.sourceHash(), MoneyService.cents(target.money()),
                ProjectKind.UPGRADE, from.id(), 0), target, required, payer);
    }

    /**
     * Hilo principal. Reparar {@code build} según lo que le falta ({@code r}, de {@link RepairMath#of}): sin nada que
     * reparar o con la estructura cambiada, {@link RepairException}; si falta todo, la construcción desapareció: se
     * borra su registro con {@code remove} ({@code ModuleService.remove}, que llama a {@code onRemoved}) y
     * {@link RepairException} {@code repair.vanished}. Si no, {@link #createRepair} con el precio y el dinero
     * proporcionales ({@link RepairMath#price}, {@link RepairMath#money}).
     */
    public CompletableFuture<Project> repair(UUID bench, StructureDefinition def, BuildRecord build,
                                             RepairMath.Result r, Player payer,
                                             Function<UUID, CompletableFuture<Boolean>> remove) {
        if (!RepairMath.matches(def, build)) {
            throw new RepairException(RepairException.CHANGED);
        }
        if (r.missing() <= 0) {
            throw new RepairException(RepairException.NOTHING);
        }
        if (r.missing() >= r.total()) {
            remove.apply(build.id()).whenComplete((ok, e) -> {
                if (e != null) {
                    log.log(Level.WARNING, "No se pudo borrar la construcción desaparecida " + build.id(), e);
                } else {
                    log.info("La construcción " + build.id() + " (" + build.structure() + ") ya no está en el mundo;"
                            + " se borra su registro.");
                }
            });
            throw new RepairException(RepairException.VANISHED);
        }
        return createRepair(bench, def, build, RepairMath.price(def.materials(), r.missing(), r.total()), r.missing(),
                payer, RepairMath.money(MoneyService.cents(def.money()), r.missing(), r.total()));
    }

    /**
     * Alta de una obra nueva de cualquier tipo: mesa libre, caja sin choque, dinero ({@code rec.moneyCents()}) cobrado
     * a {@code payer} ANTES del alta (el MONEY_CHARGE va en su misma transacción) y la base de datos; si el alta
     * falla, se devuelve.
     */
    private CompletableFuture<Project> insertNew(ProjectRecord rec, StructureDefinition def,
                                                 Map<String, Integer> required, Player payer) {
        UUID bench = rec.bench();
        if (hasActive(bench) || creating.containsKey(bench)) {
            throw new IllegalStateException("La mesa " + bench + " ya tiene una obra activa");
        }
        int sx = rec.rotation().rotatedWidth(def.blocks().width(), def.blocks().length());
        int sz = rec.rotation().rotatedLength(def.blocks().width(), def.blocks().length());
        Placed mine = new Placed(rec.world(), new Box(rec.x(), rec.y(), rec.z(), rec.x() + sx - 1,
                rec.y() + def.blocks().height() - 1, rec.z() + sz - 1));
        // Hilo principal: comprobar y reservar no se pisan con otra confirmación.
        if (boxes.values().stream().anyMatch(mine::hits) || creating.values().stream().anyMatch(mine::hits)) {
            throw new OverlapException("La obra de la mesa " + bench + " se cruza con otra obra");
        }
        long cents = rec.moneyCents();
        if (cents > Integer.MAX_VALUE) {
            // El libro guarda la cantidad en un INTEGER (32 bits en MySQL).
            log.warning("La estructura " + def.id() + " pide más dinero del que se puede apuntar: " + def.money());
            throw new ChargeException(ChargeResult.ERROR);
        }
        if (cents > 0) {
            ChargeResult charged = money == null || payer == null ? ChargeResult.UNAVAILABLE
                    : money.charge(payer, cents);
            if (charged != ChargeResult.OK) {
                throw new ChargeException(charged);
            }
        }
        CompletableFuture<Project> result = new CompletableFuture<>();
        // Lo último: de aquí en adelante todo camino (también un fallo síncrono) libera la mesa y la caja.
        creating.put(bench, mine);
        CompletableFuture<Void> insert;
        try {
            insert = repo.insert(rec, required, sx, def.blocks().height(), sz);
        } catch (RuntimeException e) {
            insert = CompletableFuture.failedFuture(e);
        }
        insert.whenComplete((v, err) -> main.accept(() -> {
            if (err == null) {
                created(bench, mine, rec, required, result);
            } else if (cents == 0) {
                creating.remove(bench);
                logFailedInsert(rec, err);
                result.completeExceptionally(err);
            } else {
                settleFailedCharge(bench, mine, rec, required, cents, err, result);
            }
        }));
        return result;
    }

    /** Un choque con otra construcción o una construcción que ya no está (mejora): motivo normal, no un fallo. */
    private void logFailedInsert(ProjectRecord rec, Throwable err) {
        Throwable root = err instanceof java.util.concurrent.CompletionException && err.getCause() != null
                ? err.getCause() : err;
        if (root instanceof OverlapException || root instanceof RepairException) {
            log.info("La obra " + rec.id() + " no se creó: " + root.getMessage());
        } else {
            log.log(Level.SEVERE, "No se pudo guardar la obra " + rec.id(), err);
        }
    }

    /** Hilo principal: la obra ya está en la base; pasa a memoria y libera la reserva. */
    private void created(UUID bench, Placed mine, ProjectRecord rec, Map<String, Integer> required,
                         CompletableFuture<Project> result) {
        creating.remove(bench);
        Map<String, MaterialRow> rows = new LinkedHashMap<>();
        required.forEach((k, n) -> rows.put(k, new MaterialRow(n, 0, 0)));
        Project p = new Project(rec, rows);
        boxes.put(p.id(), mine);
        add(p);
        result.complete(p);
    }

    /**
     * Hilo principal. Un alta con cobro falló: se mira en el libro si su MONEY_CHARGE llegó (commit hecho pero el
     * driver lanzó). Si está, la obra existe y se recupera sin devolver; si no, se devuelve el cobro. Si ni eso se
     * puede saber, no se devuelve (podría pagar dos veces) y queda un SEVERE para un admin. La mesa sigue reservada
     * mientras se mira.
     */
    private void settleFailedCharge(UUID bench, Placed mine, ProjectRecord rec, Map<String, Integer> required,
                                    long cents, Throwable err, CompletableFuture<Project> result) {
        UUID charge = ProjectRepository.chargeId(rec.id());
        CompletableFuture<Boolean> check;
        try {
            check = repo.hasLedger(charge);
        } catch (RuntimeException e) {
            check = CompletableFuture.failedFuture(e);
        }
        check.whenComplete((found, checkErr) -> main.accept(() -> {
            if (checkErr == null && Boolean.TRUE.equals(found)) {
                log.log(Level.SEVERE, "El alta de la obra " + rec.id() + " dio error pero llegó a la base con su"
                        + " cobro; se recupera sin devolver el dinero", err);
                created(bench, mine, rec, required, result);
                return;
            }
            creating.remove(bench);
            logFailedInsert(rec, err);
            if (checkErr == null) {
                money.refund(rec.owner(), cents, charge.toString());
            } else {
                log.log(Level.SEVERE, "COBRO SIN VERIFICAR " + rec.owner() + " " + MoneyService.format(cents)
                        + " obra " + rec.id() + " (cobro " + charge + "): no se pudo mirar el libro; si la obra no"
                        + " está en projects, devolver el dinero a mano", checkErr);
            }
            result.completeExceptionally(err);
        }));
    }

    /** Saca la obra de la memoria (terminada o cancelada). Si terminó, la mesa queda en COMPLETED hasta abrir el menú. */
    public void finish(Project p) {
        byId.remove(p.id());
        byBench.remove(p.bench(), p);
        boxes.remove(p.id());
        if (p.state() == ProjectState.COMPLETED) {
            completed.add(p.bench());
        }
    }

    /** El jugador abrió el menú de la mesa: deja de mostrarse COMPLETED. */
    public void acknowledge(UUID bench) {
        completed.remove(bench);
    }

    public WorkbenchState benchState(UUID bench) {
        Project p = byBench.get(bench);
        if (p == null) {
            return completed.contains(bench) ? WorkbenchState.COMPLETED : WorkbenchState.IDLE;
        }
        return switch (p.state()) {
            case WAITING_MATERIALS -> WorkbenchState.WAITING_MATERIALS;
            case READY -> WorkbenchState.READY;
            case BUILDING -> WorkbenchState.BUILDING;
            case PAUSED -> WorkbenchState.PAUSED;
            case ERROR -> WorkbenchState.ERROR;
            case COMPLETED -> WorkbenchState.COMPLETED;
            case CANCELLED -> WorkbenchState.IDLE;
        };
    }
}
