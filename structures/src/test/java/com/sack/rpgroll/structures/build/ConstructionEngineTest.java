package com.sack.rpgroll.structures.build;

import com.sack.rpgroll.common.structure.BlockSource;
import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.catalog.BuildOrder;
import com.sack.rpgroll.structures.catalog.MaterialSpec;
import com.sack.rpgroll.structures.catalog.PlacementRules;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.config.PerformanceSettings;
import com.sack.rpgroll.structures.project.Project;
import com.sack.rpgroll.structures.project.ProjectService;
import com.sack.rpgroll.structures.project.RepairMath;
import com.sack.rpgroll.structures.project.ProjectState;
import com.sack.rpgroll.structures.protect.AreaGuard;
import com.sack.rpgroll.structures.storage.Box;
import com.sack.rpgroll.structures.storage.Database;
import com.sack.rpgroll.structures.storage.Dialect;
import com.sack.rpgroll.structures.storage.MaterialRow;
import com.sack.rpgroll.structures.storage.Movement;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConstructionEngineTest {

    static final String KEY = "minecraft:stone";

    @TempDir Path dir;
    Database db;
    ProjectRepository repo;
    ProjectService service;
    FakeWriter writer;
    final long[] now = {0};
    final List<Project> completed = new CopyOnWriteArrayList<>();
    /** Lo que recibe el aviso de obra terminada junto a la obra: la construcción ya registrada. */
    final List<com.sack.rpgroll.structures.storage.BuildRecord> builtRecords = new CopyOnWriteArrayList<>();
    ConstructionEngine.Precheck precheck = (d, owner, w, box) -> Optional.empty();
    final Map<String, StructureDefinition> defs = new HashMap<>();
    PerformanceSettings perf = perf(256, 10, 5, 2);
    AreaGuard guard = AreaGuard.allowAll();
    /** Mesas cargadas: todas salvo las de {@code benchesAway}. */
    final Set<UUID> benchesAway = new HashSet<>();
    final List<Project> pausedNotices = new CopyOnWriteArrayList<>();
    final List<java.util.logging.LogRecord> logs = new CopyOnWriteArrayList<>();
    final Logger logger = Logger.getAnonymousLogger();
    final BlockData stone = data(Material.STONE);
    /** Dónde se calcula el plan; directo salvo en la prueba del cálculo fuera del hilo principal. */
    java.util.concurrent.Executor planner = Runnable::run;

    /** Mundo en memoria: aire salvo lo que se pone; chunks cargados salvo los de {@code unloaded}. */
    static final class FakeWriter implements BlockWriter {
        final Map<String, BlockInfo> blocks = new HashMap<>();
        final Set<String> unloaded = new HashSet<>();
        /** Posiciones con un bloque con estado propio (cofre, cartel...). */
        final Set<String> states = new HashSet<>();
        int writes;
        RuntimeException fail;

        static String pos(int x, int y, int z) {
            return x + "," + y + "," + z;
        }

        @Override public boolean chunkLoaded(String world, int x, int z) {
            return !unloaded.contains(world + ":" + (x >> 4) + "," + (z >> 4));
        }

        @Override public BlockInfo read(String world, int x, int y, int z) {
            return blocks.getOrDefault(world + ":" + pos(x, y, z), new BlockInfo(Material.AIR, false, null));
        }

        @Override public boolean keepsState(String world, int x, int y, int z) {
            return states.contains(world + ":" + pos(x, y, z));
        }

        @Override public void write(String world, int x, int y, int z, SourceBlock b) {
            if (fail != null) {
                throw fail;
            }
            writes++;
            blocks.put(world + ":" + pos(x, y, z), new BlockInfo(b.data().getMaterial(), false, b.data()));
        }
    }

    static BlockData data(Material m) {
        BlockData d = mock(BlockData.class);
        when(d.getMaterial()).thenReturn(m);
        when(d.clone()).thenReturn(d);
        return d;
    }

    static PerformanceSettings perf(int perTick, int server, int world, int player) {
        return new PerformanceSettings(perTick, server, world, player, 4096, 64, 300, 100, 60);
    }

    @BeforeEach void setUp() throws Exception {
        db = Database.open(Dialect.SQLITE, "jdbc:sqlite:" + dir.resolve("data.db"), "", "",
                Logger.getAnonymousLogger());
        repo = new ProjectRepository(db, Dialect.SQLITE);
        service = new ProjectService(repo, Runnable::run, Logger.getAnonymousLogger());
        writer = new FakeWriter();
        logger.addHandler(new java.util.logging.Handler() {
            @Override public void publish(java.util.logging.LogRecord r) { logs.add(r); }
            @Override public void flush() { }
            @Override public void close() { }
        });
    }

    @AfterEach void tearDown() {
        db.close();
    }

    ConstructionEngine engine(ProjectService s) {
        return engine(s, repo);
    }

    ConstructionEngine engine(ProjectService s, ProjectRepository r) {
        return new ConstructionEngine(s, r, id -> Optional.ofNullable(defs.get(id)), () -> perf, writer,
                Runnable::run, (p, b) -> {
                    completed.add(p);
                    builtRecords.add(b);
                }, pausedNotices::add, () -> now[0], planner,
                id -> !benchesAway.contains(id), guard, precheck, logger);
    }

    /** Fila de {@code width} x {@code length} bloques de piedra, una capa. */
    StructureDefinition def(String id, int width, int length, boolean instant, int perCycle, int ticks) {
        List<SourceBlock> blocks = IntStream.range(0, width * length)
                .mapToObj(i -> new SourceBlock(i % width, 0, i / width, stone, null)).toList();
        BlockSource src = new BlockSource() {
            public int width() { return width; }
            public int height() { return 1; }
            public int length() { return length; }
            public List<SourceBlock> blocks() { return blocks; }
        };
        StructureDefinition d = new StructureDefinition(id, Map.of(), "casas", null, null, null, src,
                List.of(new MaterialSpec(List.of(KEY), width * length, false)), 0, instant, BuildOrder.BOTTOM_UP,
                List.of(), perCycle, ticks, new PlacementRules(false, false, List.of(), List.of(), Set.of()),
                "rpgrollstructures.build." + id);
        defs.put(id, d);
        return d;
    }

    int nextX = 100;

    /** Obra READY con todo depositado. */
    Project ready(StructureDefinition d, UUID owner, String worldName, Rotation r) {
        World w = mock(World.class);
        when(w.getName()).thenReturn(worldName);
        // Cada obra en su sitio: ProjectService rechaza cajas que se cruzan.
        Project p = service.create(UUID.randomUUID(), d, owner, new Location(w, nextX, 64, 200), r).join();
        nextX += 1000;
        int amount = d.materials().getFirst().amount();
        assertTrue(repo.applyMovement(new Movement(UUID.randomUUID(), p.id(), owner, Movement.Kind.DEPOSIT, KEY,
                amount)).join());
        p.setMaterials(Map.of(KEY, new MaterialRow(amount, amount, 0)));
        p.setState(ProjectState.READY);
        assertTrue(repo.saveProgress(p.id(), ProjectState.READY, 0, 0, 0, null).join());
        return p;
    }

    Project ready(StructureDefinition d) {
        return ready(d, UUID.randomUUID(), "world", Rotation.NONE);
    }

    static Player player(UUID id) {
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(id);
        when(p.hasPermission(anyString())).thenReturn(true);
        return p;
    }

    void settle() {
        db.submit(c -> null).join();
    }

    int dbConsumed(Project p) {
        settle();
        return repo.materials(p.id()).join().get(KEY).consumed();
    }

    void ticks(ConstructionEngine e, int n) {
        for (int i = 0; i < n; i++) {
            e.tick();
        }
    }

    @Test void instantBuildsWithinBudget() {
        Project p = ready(def("casa", 300, 1, true, 0, 0));
        ConstructionEngine e = engine(service);
        assertEquals(ConstructionEngine.StartResult.STARTED, e.start(player(p.owner()), p));
        e.tick();
        assertEquals(256, p.placed());
        assertEquals(256, p.cursor());
        assertEquals(256, dbConsumed(p));
        e.tick();
        settle();
        assertEquals(ProjectState.COMPLETED, p.state());
        assertEquals(300, writer.writes);
        assertEquals(300, dbConsumed(p));
        assertEquals(List.of(p), completed);
        assertTrue(service.byId(p.id()).isEmpty());
        assertTrue(repo.active().join().isEmpty());
        assertEquals(List.of(new Box(100, 64, 200, 399, 64, 200)), repo.boxesIn("world").join());
    }

    @Test void completedGetsTheRegisteredBuild() {
        Project p = ready(def("casa", 3, 1, true, 0, 0));
        ConstructionEngine e = engine(service);
        e.start(player(p.owner()), p);
        e.tick();
        settle();
        assertEquals(List.of(p), completed);
        assertEquals(repo.buildsOf(p.owner()).join(), builtRecords, "la misma fila que quedó en la base de datos");
    }

    @Test void modulePrecheckDeniesStartWithItsKey() {
        Project p = ready(def("casa", 3, 1, true, 0, 0));
        List<Box> asked = new java.util.ArrayList<>();
        precheck = (d, owner, w, box) -> {
            asked.add(box);
            return owner.equals(p.owner()) && w.equals("world") ? Optional.of("module.parcel.no-blocks")
                    : Optional.empty();
        };
        ConstructionEngine e = engine(service);
        assertEquals(ConstructionEngine.StartResult.MODULE_DENIED, e.start(player(p.owner()), p));
        assertEquals("module.parcel.no-blocks", e.deniedKey());
        assertEquals(List.of(new Box(100, 64, 200, 102, 64, 200)), asked);
        assertEquals(ProjectState.READY, p.state());
    }

    @Test void progressiveRespectsCycle() {
        Project p = ready(def("casa", 100, 1, false, 16, 20));
        ConstructionEngine e = engine(service);
        e.start(player(p.owner()), p);
        ticks(e, 19);
        assertEquals(0, p.placed());
        e.tick();
        assertEquals(16, p.placed());
        ticks(e, 20);
        assertEquals(32, p.placed());
        assertEquals(32, dbConsumed(p));
    }

    @Test void occupiedBlockIsSkippedNotReplaced() {
        Project p = ready(def("casa", 10, 1, true, 0, 0));
        BlockData foreign = data(Material.COBBLESTONE);
        BlockWriter.BlockInfo theirs = new BlockWriter.BlockInfo(Material.COBBLESTONE, false, foreign);
        writer.blocks.put("world:" + FakeWriter.pos(105, 64, 200), theirs);
        // Hierba alta (reemplazable) sí se pisa.
        writer.blocks.put("world:" + FakeWriter.pos(106, 64, 200),
                new BlockWriter.BlockInfo(Material.SHORT_GRASS, true, data(Material.SHORT_GRASS)));
        ConstructionEngine e = engine(service);
        e.start(player(p.owner()), p);
        e.tick();
        settle();
        assertEquals(ProjectState.COMPLETED, p.state());
        assertSame(theirs, writer.blocks.get("world:" + FakeWriter.pos(105, 64, 200)));
        assertEquals(1, p.skipped());
        assertEquals(9, p.placed());
        assertEquals(9, writer.writes);
    }

    @Test void replaceListAllowsOverwriting() {
        StructureDefinition base = def("casa", 3, 1, true, 0, 0);
        StructureDefinition d = new StructureDefinition(base.id(), base.names(), base.category(), base.tier(),
                base.icon(), base.schematic(), base.blocks(), base.materials(), 0, true, BuildOrder.BOTTOM_UP,
                List.of(), 0, 0, new PlacementRules(false, false, List.of(), List.of(), Set.of(Material.DIRT)),
                base.permission());
        defs.put(d.id(), d);
        Project p = ready(d);
        writer.blocks.put("world:" + FakeWriter.pos(101, 64, 200),
                new BlockWriter.BlockInfo(Material.DIRT, false, data(Material.DIRT)));
        ConstructionEngine e = engine(service);
        e.start(player(p.owner()), p);
        e.tick();
        assertEquals(3, writer.writes);
        assertEquals(0, p.skipped());
    }

    @Test void unloadedChunkWaitsWithoutAdvancing() {
        Project p = ready(def("casa", 40, 1, true, 0, 0));
        // x 100..139: chunks 6 (96..111), 7 (112..127) y 8 (128..143); el 7 está descargado.
        writer.unloaded.add("world:7,12");
        ConstructionEngine e = engine(service);
        e.start(player(p.owner()), p);
        e.tick();
        assertEquals(12, p.cursor());
        e.tick();
        assertEquals(12, p.cursor());
        assertEquals(ProjectState.BUILDING, p.state());
        assertEquals(12, dbConsumed(p));
        writer.unloaded.clear();
        e.tick();
        settle();
        assertEquals(ProjectState.COMPLETED, p.state());
    }

    @Test void resumeFromSavedCursorDoesNotRecharge() {
        perf = perf(50, 10, 5, 2);
        Project p = ready(def("casa", 200, 1, true, 0, 0));
        ConstructionEngine e = engine(service);
        e.start(player(p.owner()), p);
        e.tick();            // 50, se guarda (primer guardado)
        now[0] += 1000;
        e.tick();            // 100, se guarda
        e.tick();            // 150, no toca guardar: se cae aquí sin flushAll
        settle();
        assertEquals(150, repo.materials(p.id()).join().get(KEY).consumed());
        assertEquals(100, repo.active().join().getFirst().cursor());

        // Arranque nuevo: misma obra leída de la base de datos, otro motor.
        ProjectService fresh = new ProjectService(repo, Runnable::run, Logger.getAnonymousLogger());
        fresh.load();
        Project again = fresh.byId(p.id()).orElseThrow();
        assertEquals(ProjectState.BUILDING, again.state());
        assertEquals(100, again.cursor());
        int writesBefore = writer.writes;
        ConstructionEngine e2 = engine(fresh);
        ticks(e2, 5);
        settle();
        assertEquals(ProjectState.COMPLETED, again.state());
        assertEquals(200, again.placed());
        assertEquals(200, dbConsumed(again));
        assertEquals(50, writer.writes - writesBefore, "los bloques 100..149 ya estaban: no se reescriben");
        int ledger = db.submit(c -> {
            try (var ps = c.prepareStatement("SELECT SUM(amount) FROM ledger WHERE kind = 'CONSUME'")) {
                var rs = ps.executeQuery();
                rs.next();
                return rs.getInt(1);
            }
        }).join();
        assertEquals(200, ledger);
    }

    @Test void flushAllSavesCursor() {
        perf = perf(50, 10, 5, 2);
        Project p = ready(def("casa", 200, 1, true, 0, 0));
        ConstructionEngine e = engine(service);
        e.start(player(p.owner()), p);
        e.tick();
        e.tick();
        e.flushAll();
        assertEquals(100, repo.active().join().getFirst().cursor());
        assertEquals(100, repo.materials(p.id()).join().get(KEY).consumed());
    }

    @Test void limitsArePerServerWorldAndPlayer() {
        perf = perf(256, 3, 2, 1);
        StructureDefinition d = def("casa", 5, 1, false, 1, 20);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        ConstructionEngine e = engine(service);
        Project a1 = ready(d, a, "w1", Rotation.NONE);
        Project a2 = ready(d, a, "w2", Rotation.NONE);
        Project b1 = ready(d, b, "w1", Rotation.NONE);
        Project c1 = ready(d, c, "w1", Rotation.NONE);
        Project c2 = ready(d, c, "w3", Rotation.NONE);
        Project d1 = ready(d, UUID.randomUUID(), "w4", Rotation.NONE);
        assertEquals(ConstructionEngine.StartResult.STARTED, e.start(player(a), a1));
        assertEquals(ConstructionEngine.StartResult.LIMIT_PLAYER, e.start(player(a), a2));
        assertEquals(ConstructionEngine.StartResult.STARTED, e.start(player(b), b1));
        assertEquals(ConstructionEngine.StartResult.LIMIT_WORLD, e.start(player(c), c1));
        assertEquals(ConstructionEngine.StartResult.STARTED, e.start(player(c), c2));
        assertEquals(ConstructionEngine.StartResult.LIMIT_SERVER, e.start(player(d1.owner()), d1));
        // Una obra en pausa sigue ocupando su sitio.
        e.pause(c2);
        assertEquals(ConstructionEngine.StartResult.LIMIT_SERVER, e.start(player(d1.owner()), d1));
        assertEquals(ProjectState.READY, d1.state());
    }

    @Test void startNeedsReadyIdleAndPermission() {
        StructureDefinition d = def("casa", 5, 1, true, 0, 0);
        Project p = ready(d);
        ConstructionEngine e = engine(service);
        Player noBuild = player(p.owner());
        when(noBuild.hasPermission("rpgrollstructures.build")).thenReturn(false);
        assertEquals(ConstructionEngine.StartResult.NO_PERMISSION, e.start(noBuild, p));
        Player denied = player(p.owner());
        when(denied.isPermissionSet("rpgrollstructures.build.casa")).thenReturn(true);
        when(denied.hasPermission("rpgrollstructures.build.casa")).thenReturn(false);
        assertEquals(ConstructionEngine.StartResult.NO_PERMISSION, e.start(denied, p));
        p.setBusy(true);
        assertEquals(ConstructionEngine.StartResult.NOT_READY, e.start(player(p.owner()), p));
        p.setBusy(false);
        p.setState(ProjectState.WAITING_MATERIALS);
        assertEquals(ConstructionEngine.StartResult.NOT_READY, e.start(player(p.owner()), p));
        assertEquals(ProjectState.WAITING_MATERIALS, p.state());
    }

    @Test void startInProtectedAreaReturnsProtected() {
        Project p = ready(def("casa", 3, 2, true, 0, 0), UUID.randomUUID(), "world", Rotation.CW_90);
        List<Object[]> calls = new java.util.ArrayList<>();
        guard = (owner, world, box) -> {
            calls.add(new Object[] { owner, world, box });
            return Optional.of(new AreaGuard.Denial(com.sack.rpgroll.structures.integration.Hook.GRIEFPREVENTION,
                    "protect.griefprevention"));
        };
        ConstructionEngine e = engine(service);
        Player starter = player(UUID.randomUUID()); // el dueño de la mesa también arranca
        when(starter.hasPermission("rpgrollstructures.bypass")).thenReturn(false);
        assertEquals(ConstructionEngine.StartResult.PROTECTED, e.start(starter, p));
        assertEquals(ProjectState.READY, p.state());
        assertEquals(1, calls.size());
        assertEquals(p.owner(), calls.getFirst()[0], "por el dueño de la obra, no por quien arranca");
        assertEquals("world", calls.getFirst()[1]);
        // 3 x 2 girada 90°: 2 en x y 3 en z desde la esquina mínima.
        assertEquals(new Box(p.x(), 64, 200, p.x() + 1, 64, 202), calls.getFirst()[2]);

        Player bypass = player(UUID.randomUUID()); // con todos los permisos, también bypass
        assertEquals(ConstructionEngine.StartResult.STARTED, e.start(bypass, p));
        assertEquals(1, calls.size(), "bypass se salta la comprobación");
    }

    @Test void sameBlockDataIsNotRewritten() {
        Project p = ready(def("casa", 5, 1, true, 0, 0));
        writer.blocks.put("world:" + FakeWriter.pos(102, 64, 200),
                new BlockWriter.BlockInfo(Material.STONE, false, stone));
        ConstructionEngine e = engine(service);
        e.start(player(p.owner()), p);
        e.tick();
        settle();
        assertEquals(ProjectState.COMPLETED, p.state());
        assertEquals(4, writer.writes);
        assertEquals(5, p.placed());
        assertEquals(0, p.skipped());
    }

    @Test void leavingBuildingStopsWritingAndConsuming() {
        Project p = ready(def("casa", 100, 1, false, 10, 1));
        ConstructionEngine e = engine(service);
        e.start(player(p.owner()), p);
        e.tick();
        assertEquals(10, p.cursor());
        p.setBusy(true); // cancelación en vuelo
        e.tick();
        assertEquals(10, p.cursor());
        p.setBusy(false);
        e.pause(p);
        assertEquals(ProjectState.PAUSED, p.state());
        e.tick();
        assertEquals(10, p.cursor());
        assertEquals(10, writer.writes);
        assertEquals(10, dbConsumed(p));
        assertEquals(ProjectState.PAUSED, repo.active().join().getFirst().state());
        e.resume(p);
        e.tick();
        assertEquals(20, p.cursor());
        p.setState(ProjectState.CANCELLED);
        e.tick();
        assertEquals(20, writer.writes);
    }

    @Test void writeErrorMarksError() {
        Project p = ready(def("casa", 5, 1, true, 0, 0));
        writer.fail = new IllegalStateException("bloque raro");
        ConstructionEngine e = engine(service);
        e.start(player(p.owner()), p);
        e.tick();
        settle();
        assertEquals(ProjectState.ERROR, p.state());
        assertNotNull(p.error());
        assertEquals(ProjectState.ERROR, repo.active().join().getFirst().state());
        e.tick();
        assertEquals(0, writer.writes);
    }

    @Test void rotatedProjectUsesRotatedBox() {
        Project p = ready(def("casa", 3, 2, true, 0, 0), UUID.randomUUID(), "world", Rotation.CW_90);
        ConstructionEngine e = engine(service);
        e.start(player(p.owner()), p);
        e.tick();
        settle();
        assertEquals(ProjectState.COMPLETED, p.state());
        // 3 de ancho y 2 de largo, girada 90°: 2 en x y 3 en z desde la esquina mínima.
        assertEquals(List.of(new Box(100, 64, 200, 101, 64, 202)), repo.boxesIn("world").join());
        assertNotNull(writer.blocks.get("world:" + FakeWriter.pos(101, 64, 202)));
    }

    @Test void failedCompleteKeepsProjectAndRetries() {
        Project p = ready(def("casa", 5, 1, true, 0, 0));
        ProjectRepository r = spy(repo);
        doReturn(java.util.concurrent.CompletableFuture.failedFuture(new RuntimeException("bd")))
                .doReturn(java.util.concurrent.CompletableFuture.completedFuture(false))
                .doCallRealMethod().when(r).complete(any(), any());
        ConstructionEngine e = engine(service, r);
        e.start(player(p.owner()), p);
        e.tick();
        settle();
        assertEquals(ProjectState.BUILDING, p.state());
        assertTrue(service.byId(p.id()).isPresent());
        assertTrue(completed.isEmpty());
        assertTrue(service.hasActive(p.bench()));
        e.tick(); // menos de un segundo: no se reintenta
        settle();
        verify(r, times(1)).complete(any(), any());
        now[0] += 1000;
        e.tick();
        settle();
        assertEquals(ProjectState.BUILDING, p.state());
        now[0] += 5000; // tras el segundo fallo se espera 5 s
        e.tick();
        settle();
        verify(r, times(3)).complete(any(), any());
        assertEquals(ProjectState.COMPLETED, p.state());
        assertEquals(List.of(p), completed);
        assertTrue(service.byId(p.id()).isEmpty());
        assertEquals(List.of(new Box(100, 64, 200, 104, 64, 200)), repo.boxesIn("world").join());
    }

    @Test void failedCompletesBackOffAndStayBuilding() {
        Project p = ready(def("casa", 5, 1, true, 0, 0));
        ProjectRepository r = spy(repo);
        doReturn(java.util.concurrent.CompletableFuture.completedFuture(false)).when(r).complete(any(), any());
        ConstructionEngine e = engine(service, r);
        e.start(player(p.owner()), p);
        e.tick(); // primer intento en t = 0
        settle();
        int calls = 1;
        for (long wait : new long[] { 1000, 5000, 30000, 60000, 60000 }) {
            now[0] += wait - 1;
            e.tick();
            settle();
            verify(r, times(calls)).complete(any(), any());
            now[0] += 1;
            e.tick();
            settle();
            verify(r, times(++calls)).complete(any(), any());
        }
        assertEquals(ProjectState.BUILDING, p.state(), "nunca pasa a ERROR por no poder registrar");
        assertNull(p.error());
        assertTrue(service.byId(p.id()).isPresent());
        assertEquals(ProjectState.BUILDING, repo.active().join().getFirst().state());
    }

    @Test void completeChargesRemainingBeforeFinishing() {
        Project p = ready(def("casa", 5, 1, true, 0, 0));
        // Cayó con el cursor al final guardado pero sin el último CONSUME.
        assertTrue(repo.saveProgress(p.id(), ProjectState.BUILDING, 5, 5, 0, null).join());
        ProjectService fresh = new ProjectService(repo, Runnable::run, Logger.getAnonymousLogger());
        fresh.load();
        Project again = fresh.byId(p.id()).orElseThrow();
        ConstructionEngine e = engine(fresh);
        e.tick();
        settle();
        assertEquals(ProjectState.COMPLETED, again.state());
        assertEquals(5, dbConsumed(again));
        assertEquals(0, writer.writes);
    }

    @Test void consumesAreBatchedPerTick() {
        StructureDefinition d = def("casa", 100, 1, false, 10, 1);
        Project a = ready(d);
        Project b = ready(d);
        ProjectRepository r = spy(repo);
        ConstructionEngine e = engine(service, r);
        e.start(player(a.owner()), a);
        e.start(player(b.owner()), b);
        e.tick();
        e.tick(); // un plan nuevo por tick: ya están las dos
        settle();
        int ca = a.cursor(), cb = b.cursor();
        clearInvocations(r);
        e.tick();
        settle();
        assertEquals(ca + 10, a.cursor());
        assertEquals(cb + 10, b.cursor());
        verify(r, times(1)).applyMovements(anyList());
        verify(r, never()).applyMovement(any());
        assertEquals(a.cursor(), dbConsumed(a));
        assertEquals(b.cursor(), dbConsumed(b));
    }

    @Test void rejectedConsumeIsRevertedInMemory() {
        Project p = ready(def("casa", 100, 1, false, 10, 1));
        ProjectRepository r = spy(repo);
        doReturn(java.util.concurrent.CompletableFuture.completedFuture(List.of(false)))
                .doCallRealMethod().when(r).applyMovements(anyList());
        ConstructionEngine e = engine(service, r);
        e.start(player(p.owner()), p);
        e.tick();
        settle();
        assertEquals(10, p.cursor());
        assertEquals(0, p.materials().get(KEY).consumed());
        e.tick(); // el siguiente avance cobra también lo rechazado
        settle();
        assertEquals(20, dbConsumed(p));
        assertEquals(20, p.materials().get(KEY).consumed());
    }

    @Test void plansAreBuiltOnePerTick() {
        StructureDefinition d = def("casa", 1000, 1, true, 0, 0);
        List<Project> ps = List.of(ready(d), ready(d), ready(d));
        ConstructionEngine e = engine(service);
        ps.forEach(p -> assertEquals(ConstructionEngine.StartResult.STARTED, e.start(player(p.owner()), p)));
        assertTrue(ps.stream().allMatch(p -> p.state() == ProjectState.BUILDING));
        for (int t = 1; t <= 3; t++) {
            e.tick();
            assertEquals(t, ps.stream().filter(p -> p.cursor() > 0).count(), "tick " + t);
        }
    }

    @Test void waitingProjectDoesNotTakeBudget() {
        perf = perf(100, 10, 5, 2);
        Project waiting = ready(def("casa", 10, 1, true, 0, 0), UUID.randomUUID(), "wa", Rotation.NONE);
        Project busy = ready(def("grande", 1000, 1, true, 0, 0), UUID.randomUUID(), "world", Rotation.NONE);
        writer.unloaded.add("wa:6,12");
        ConstructionEngine e = engine(service);
        e.start(player(waiting.owner()), waiting);
        e.start(player(busy.owner()), busy);
        e.tick();
        int before = busy.cursor();
        e.tick();
        assertEquals(0, waiting.cursor());
        assertEquals(before + 100, busy.cursor());
    }

    @Test void planIsBuiltOffTheMainThread() {
        java.util.List<Runnable> queued = new java.util.ArrayList<>();
        planner = queued::add;
        StructureDefinition d = def("casa", 10, 1, true, 0, 0);
        Project a = ready(d);
        Project b = ready(d);
        ConstructionEngine e = engine(service);
        e.start(player(a.owner()), a);
        e.start(player(b.owner()), b);
        e.tick();
        assertEquals(1, queued.size(), "el plan va al otro hilo");
        assertEquals(0, a.cursor() + b.cursor());
        e.tick();
        assertEquals(1, queued.size(), "un plan en cálculo a la vez");
        queued.removeFirst().run();
        e.tick();
        assertEquals(1, java.util.stream.Stream.of(a, b).filter(x -> x.cursor() > 0).count(),
                "se instala en el hilo principal, uno por tick");
        e.tick();
        assertEquals(1, queued.size(), "y arranca el siguiente");
        queued.removeFirst().run();
        e.tick();
        settle();
        assertEquals(ProjectState.COMPLETED, a.state());
        assertEquals(ProjectState.COMPLETED, b.state());
    }

    @Test void plannedProjectCancelledMeanwhileFreesThePlanner() {
        java.util.List<Runnable> queued = new java.util.ArrayList<>();
        planner = queued::add;
        StructureDefinition d = def("casa", 10, 1, true, 0, 0);
        Project a = ready(d);
        ConstructionEngine e = engine(service);
        e.start(player(a.owner()), a);
        e.tick();
        a.setState(ProjectState.CANCELLED);
        service.finish(a);
        Project b = ready(d);
        e.start(player(b.owner()), b);
        e.tick();
        assertEquals(2, queued.size(), "el plan de la cancelada no bloquea a la siguiente");
    }

    StructureDefinition hashed(StructureDefinition d, String hash) {
        StructureDefinition h = new StructureDefinition(d.id(), d.names(), d.category(), d.tier(), d.icon(),
                d.schematic(), d.blocks(), d.materials(), d.money(), d.instant(), d.order(), d.customOrder(),
                d.blocksPerCycle(), d.ticksPerCycle(), d.placement(), d.permission(), hash);
        defs.put(h.id(), h);
        return h;
    }

    @Test void changedSchematicWithSameVolumeMarksError() {
        StructureDefinition d = hashed(def("casa", 5, 1, true, 0, 0), "aaa");
        Project p = ready(d);
        assertEquals("aaa", p.sourceHash());
        hashed(d, "bbb"); // otro esquemático con el mismo número de bloques
        ConstructionEngine e = engine(service);
        e.start(player(p.owner()), p);
        e.tick();
        settle();
        assertEquals(ProjectState.ERROR, p.state());
        assertTrue(p.error().contains("esquemático"), p.error());
        assertEquals(0, writer.writes);
    }

    @Test void projectWithoutHashIsNotChecked() {
        Project p = ready(def("casa", 5, 1, true, 0, 0)); // obra vieja: sin hash
        assertNull(p.sourceHash());
        hashed(defs.get("casa"), "bbb");
        ConstructionEngine e = engine(service);
        e.start(player(p.owner()), p);
        e.tick();
        settle();
        assertEquals(ProjectState.COMPLETED, p.state());
    }

    @Test void finishedProjectIsNotHashChecked() {
        StructureDefinition d = hashed(def("casa", 5, 1, true, 0, 0), "aaa");
        Project p = ready(d);
        // Cayó con todo puesto y sin registrar; luego el esquemático cambió: solo le falta el registro.
        assertTrue(repo.saveProgress(p.id(), ProjectState.BUILDING, 5, 5, 0, null).join());
        hashed(d, "bbb");
        ProjectService fresh = new ProjectService(repo, Runnable::run, Logger.getAnonymousLogger());
        fresh.load();
        Project again = fresh.byId(p.id()).orElseThrow();
        engine(fresh).tick();
        settle();
        assertEquals(ProjectState.COMPLETED, again.state());
        assertEquals(0, writer.writes);
    }

    /** Guarda que niega mientras {@code deny[0]}; cuenta las llamadas en {@code calls[0]}. */
    AreaGuard switchable(boolean[] deny, int[] calls) {
        return (owner, world, box) -> {
            calls[0]++;
            return deny[0] ? Optional.of(new AreaGuard.Denial(
                    com.sack.rpgroll.structures.integration.Hook.WORLDGUARD, "protect.worldguard")) : Optional.empty();
        };
    }

    static Player noBypass(UUID id) {
        Player p = player(id);
        when(p.hasPermission("rpgrollstructures.bypass")).thenReturn(false);
        return p;
    }

    @Test void benchNotLoadedWaitsWithoutStateChange() {
        perf = perf(100, 10, 5, 2);
        Project away = ready(def("casa", 10, 1, true, 0, 0));
        Project other = ready(def("grande", 1000, 1, true, 0, 0));
        benchesAway.add(away.bench());
        ConstructionEngine e = engine(service);
        e.start(player(away.owner()), away);
        e.start(player(other.owner()), other);
        ticks(e, 2);
        int before = other.cursor();
        e.tick();
        assertEquals(before + 100, other.cursor(), "la otra obra se lleva todo el presupuesto");
        assertEquals(0, away.cursor());
        assertEquals(ProjectState.BUILDING, away.state());
        assertNull(away.error());
        assertEquals(0, dbConsumed(away));
        assertEquals(ProjectState.BUILDING, repo.active().join().stream()
                .filter(r -> r.id().equals(away.id())).findFirst().orElseThrow().state());
        benchesAway.clear();
        ticks(e, 2);
        settle();
        assertEquals(ProjectState.COMPLETED, away.state());
    }

    @Test void protectionDeniedOnInstallPausesWithReason() {
        boolean[] deny = {false};
        int[] calls = {0};
        guard = switchable(deny, calls);
        Project p = ready(def("casa", 10, 1, true, 0, 0));
        ConstructionEngine e = engine(service);
        assertEquals(ConstructionEngine.StartResult.STARTED, e.start(noBypass(p.owner()), p));
        deny[0] = true; // p. ej. un claim nuevo mientras el servidor estaba apagado
        e.tick();
        assertEquals(ProjectState.PAUSED, p.state());
        assertEquals("project.paused.protected", p.error());
        assertEquals(0, p.cursor());
        assertEquals(0, writer.writes);
        assertEquals(List.of(p), pausedNotices, "aviso al dueño");
        settle();
        var row = repo.active().join().getFirst();
        assertEquals(ProjectState.PAUSED, row.state());
        assertEquals("project.paused.protected", row.error());
        ticks(e, 3);
        assertEquals(ProjectState.PAUSED, p.state(), "no se reanuda sola");
        assertEquals(0, writer.writes);
    }

    @Test void protectionRecheckDuringBuildPauses() {
        boolean[] deny = {false};
        int[] calls = {0};
        guard = switchable(deny, calls);
        Project p = ready(def("casa", 100, 1, false, 1, 1));
        ConstructionEngine e = engine(service);
        e.start(noBypass(p.owner()), p);
        int atStart = calls[0];
        e.tick(); // se instala y se comprueba
        assertEquals(atStart + 1, calls[0]);
        assertEquals(1, p.cursor());
        deny[0] = true;
        now[0] += 59_000;
        ticks(e, 5);
        assertEquals(atStart + 1, calls[0], "no en cada tick");
        assertEquals(ProjectState.BUILDING, p.state());
        assertEquals(6, p.cursor());
        now[0] += 1_000; // 60 s desde la última comprobación
        e.tick();
        assertEquals(atStart + 2, calls[0]);
        assertEquals(ProjectState.PAUSED, p.state());
        assertEquals("project.paused.protected", p.error());
        assertEquals(6, p.cursor());
        settle();
        assertEquals("project.paused.protected", repo.active().join().getFirst().error());
    }

    @Test void protectionIsNotRecheckedForABypassStart() {
        boolean[] deny = {true};
        int[] calls = {0};
        guard = switchable(deny, calls);
        Project p = ready(def("casa", 100, 1, false, 1, 1));
        ConstructionEngine e = engine(service);
        assertEquals(ConstructionEngine.StartResult.STARTED, e.start(player(p.owner()), p)); // con bypass
        e.tick();
        now[0] += 60_000;
        e.tick();
        assertEquals(ProjectState.BUILDING, p.state());
        assertEquals(0, calls[0]);
    }

    @Test void resumeRechecksProtection() {
        boolean[] deny = {false};
        int[] calls = {0};
        guard = switchable(deny, calls);
        Project p = ready(def("casa", 100, 1, false, 1, 1));
        ConstructionEngine e = engine(service);
        e.start(noBypass(p.owner()), p);
        e.tick();
        assertEquals(ConstructionEngine.StartResult.NOT_READY, e.resume(p), "no está en pausa");
        e.pause(p);
        assertEquals(ProjectState.PAUSED, p.state());
        assertEquals("project.paused.player", p.error());
        deny[0] = true;
        assertEquals(ConstructionEngine.StartResult.PROTECTED, e.resume(p));
        assertEquals(ProjectState.PAUSED, p.state());
        assertEquals("project.paused.protected", p.error());
        deny[0] = false;
        assertEquals(ConstructionEngine.StartResult.STARTED, e.resume(p));
        assertEquals(ProjectState.BUILDING, p.state());
        assertNull(p.error(), "el motivo se borra al reanudar");
        settle();
        assertNull(repo.active().join().getFirst().error());
        assertEquals(ProjectState.BUILDING, repo.active().join().getFirst().state());
        e.pause(p);
        deny[0] = true;
        assertEquals(ConstructionEngine.StartResult.STARTED, e.resume(p, true), "bypass se salta la comprobación");
    }

    @Test void everyErrorTransitionIsLogged() {
        Project hash = ready(hashed(def("a", 5, 1, true, 0, 0), "x"));
        hashed(defs.get("a"), "y");
        Project size = ready(def("b", 5, 1, true, 0, 0));
        def("b", 6, 1, true, 0, 0);
        Project write = ready(def("c", 5, 1, true, 0, 0));
        writer.fail = new IllegalStateException("bloque raro");
        ConstructionEngine e = engine(service);
        for (Project p : List.of(hash, size, write)) {
            e.start(player(p.owner()), p);
        }
        ticks(e, 6);
        writer.fail = null;
        planner = r -> { throw new java.util.concurrent.RejectedExecutionException("sin hilos"); };
        Project plan = ready(def("d", 5, 1, true, 0, 0));
        ConstructionEngine e2 = engine(service);
        e2.start(player(plan.owner()), plan);
        e2.tick();
        settle();
        for (Project p : List.of(hash, size, write, plan)) {
            assertEquals(ProjectState.ERROR, p.state(), p.structure());
            assertNotNull(p.error());
            assertTrue(logs.stream().anyMatch(r -> r.getLevel().intValue() >= java.util.logging.Level.WARNING.intValue()
                    && r.getMessage().contains(p.id().toString()) && r.getMessage().contains(p.structure())
                    && r.getMessage().contains("ERROR")), "falta el aviso de " + p.structure());
        }
    }

    /** Construcción terminada de {@code d} en (100,64,200), con sus bloques en el mundo; terminada en el instante 1. */
    com.sack.rpgroll.structures.storage.BuildRecord built(StructureDefinition d) {
        World w = mock(World.class);
        when(w.getName()).thenReturn("world");
        Project p = service.create(UUID.randomUUID(), d, UUID.randomUUID(), new Location(w, 100, 64, 200),
                Rotation.NONE).join();
        int width = d.blocks().width();
        var b = new com.sack.rpgroll.structures.storage.BuildRecord(UUID.randomUUID(), p.id(), d.id(), p.owner(),
                "world", 100, 64, 200, Rotation.NONE, new Box(100, 64, 200, 99 + width, 64, 200), 1, null);
        assertTrue(repo.complete(p.id(), b).join());
        p.setState(ProjectState.COMPLETED);
        service.finish(p);
        for (int x = 100; x < 100 + width; x++) {
            writer.blocks.put("world:" + FakeWriter.pos(x, 64, 200), new BlockWriter.BlockInfo(Material.STONE, false,
                    stone));
        }
        return b;
    }

    /** Obra de reparación READY con su precio depositado. */
    Project readyRepair(StructureDefinition d, com.sack.rpgroll.structures.storage.BuildRecord b) {
        RepairMath.Result r = RepairMath.of(new PlacementPlan(com.sack.rpgroll.structures.catalog.RotatedCache.get(d,
                b.rotation())), writer, b.world(), b.x(), b.y(), b.z());
        Map<String, Integer> price = RepairMath.price(d.materials(), r.missing(), r.total());
        Project p = service.createRepair(UUID.randomUUID(), d, b, price, r.missing()).join();
        int amount = price.get(KEY);
        assertTrue(repo.applyMovement(new Movement(UUID.randomUUID(), p.id(), p.owner(), Movement.Kind.DEPOSIT, KEY,
                amount)).join());
        p.setMaterials(Map.of(KEY, new MaterialRow(amount, amount, 0)));
        p.setState(ProjectState.READY);
        assertTrue(repo.saveProgress(p.id(), ProjectState.READY, 0, 0, 0, null).join());
        return p;
    }

    @Test void repairNeverOverwritesForeignBlocks() {
        StructureDefinition d = def("casa", 10, 1, true, 0, 0);
        var b = built(d);
        writer.blocks.remove("world:" + FakeWriter.pos(101, 64, 200)); // se rompió
        BlockWriter.BlockInfo theirs = new BlockWriter.BlockInfo(Material.COBBLESTONE, false,
                data(Material.COBBLESTONE));
        writer.blocks.put("world:" + FakeWriter.pos(102, 64, 200), theirs); // alguien puso lo suyo
        writer.blocks.put("world:" + FakeWriter.pos(103, 64, 200),
                new BlockWriter.BlockInfo(Material.SHORT_GRASS, true, data(Material.SHORT_GRASS)));
        assertEquals(new RepairMath.Result(2, 10), RepairMath.of(new PlacementPlan(
                com.sack.rpgroll.structures.catalog.RotatedCache.get(d, Rotation.NONE)), writer, "world", 100, 64, 200));
        Project p = readyRepair(d, b);
        assertEquals(Map.of(KEY, new MaterialRow(2, 2, 0)), p.materials());
        ConstructionEngine e = engine(service);
        assertEquals(ConstructionEngine.StartResult.STARTED, e.start(player(p.owner()), p));
        e.tick();
        settle();
        assertEquals(ProjectState.COMPLETED, p.state());
        assertSame(theirs, writer.blocks.get("world:" + FakeWriter.pos(102, 64, 200)), "nunca se pisa");
        assertEquals(2, writer.writes, "solo lo que faltaba");
        assertEquals(1, p.skipped());
        assertEquals(2, p.placed(), "en una reparación, placed = lo escrito (su presupuesto)");
        assertEquals(2, dbConsumed(p));
    }

    @Test void repairCompletionCreatesNoNewBuild() {
        StructureDefinition d = def("casa", 4, 1, true, 0, 0);
        var b = built(d);
        writer.blocks.remove("world:" + FakeWriter.pos(100, 64, 200));
        Project p = readyRepair(d, b);
        // El módulo ya está en marcha: su precheck (p. ej. el terreno propio) no frena la reparación.
        precheck = (def, owner, w, box) -> Optional.of("module.parcel.no-blocks");
        ConstructionEngine e = engine(service);
        assertEquals(ConstructionEngine.StartResult.STARTED, e.start(player(p.owner()), p));
        e.tick();
        settle();
        assertEquals(ProjectState.COMPLETED, p.state());
        assertEquals(List.of(p), completed);
        assertEquals(java.util.Arrays.asList((com.sack.rpgroll.structures.storage.BuildRecord) null), builtRecords,
                "sin construcción nueva: no se vuelve a llamar a onBuilt");
        var builds = repo.buildsOf(b.owner()).join();
        assertEquals(1, builds.size());
        assertEquals(b.id(), builds.getFirst().id());
        assertEquals(b.box(), builds.getFirst().box());
        assertTrue(builds.getFirst().completedAt() > 1, "se actualiza la fecha de la construcción");
        assertTrue(repo.active().join().isEmpty());
    }

    /** Mejora de {@code from}: fila de {@code width} bloques de {@code block}, con módulo {@code module} (o null). */
    StructureDefinition upgradeOf(String id, String from, int width, BlockData block, String module) {
        List<SourceBlock> blocks = IntStream.range(0, width).mapToObj(i -> new SourceBlock(i, 0, 0, block, null))
                .toList();
        BlockSource src = new BlockSource() {
            public int width() { return width; }
            public int height() { return 1; }
            public int length() { return 1; }
            public List<SourceBlock> blocks() { return blocks; }
        };
        StructureDefinition d = new StructureDefinition(id, Map.of(), "casas", null, null, null, src,
                List.of(new MaterialSpec(List.of(KEY), width, false)), 0, true, BuildOrder.BOTTOM_UP, List.of(), 0, 0,
                new PlacementRules(false, false, List.of(), List.of(), Set.of()), "rpgrollstructures.build." + id,
                null, module, module == null ? null : new org.bukkit.configuration.MemoryConfiguration(), from);
        defs.put(id, d);
        return d;
    }

    /** Obra de mejora READY con su precio depositado. */
    Project readyUpgrade(StructureDefinition target, com.sack.rpgroll.structures.storage.BuildRecord b) {
        Project p = service.createUpgrade(UUID.randomUUID(), target, b, defs.get(target.upgradeFrom())).join();
        int amount = target.materials().getFirst().amount();
        assertTrue(repo.applyMovement(new Movement(UUID.randomUUID(), p.id(), p.owner(), Movement.Kind.DEPOSIT, KEY,
                amount)).join());
        p.setMaterials(Map.of(KEY, new MaterialRow(amount, amount, 0)));
        p.setState(ProjectState.READY);
        assertTrue(repo.saveProgress(p.id(), ProjectState.READY, 0, 0, 0, null).join());
        return p;
    }

    @Test void upgradeReplacesOnlyOwnOriginalBlocks() {
        var b = built(def("casa", 4, 1, true, 0, 0)); // piedra en x 100..103
        BlockData planks = data(Material.OAK_PLANKS);
        StructureDefinition target = upgradeOf("casa2", "casa", 6, planks, null);
        BlockWriter.BlockInfo mine = new BlockWriter.BlockInfo(Material.COBBLESTONE, false, data(Material.COBBLESTONE));
        writer.blocks.put("world:" + FakeWriter.pos(101, 64, 200), mine); // el jugador cambió un bloque a mano
        BlockWriter.BlockInfo outside = new BlockWriter.BlockInfo(Material.DIRT, false, data(Material.DIRT));
        writer.blocks.put("world:" + FakeWriter.pos(105, 64, 200), outside); // fuera de la casa vieja
        Project p = readyUpgrade(target, b);
        precheck = (d, owner, w, box) -> Optional.of("parcel.overlaps-claim");
        ConstructionEngine denied = engine(service);
        assertEquals(ConstructionEngine.StartResult.MODULE_DENIED, denied.start(player(p.owner()), p),
                "la mejora pasa por el precheck de su módulo");
        precheck = (d, owner, w, box) -> Optional.empty();
        ConstructionEngine e = engine(service);
        assertEquals(ConstructionEngine.StartResult.STARTED, e.start(player(p.owner()), p));
        e.tick();
        settle();
        assertEquals(ProjectState.COMPLETED, p.state());
        for (int x : new int[] {100, 102, 103, 104}) {
            assertSame(planks, writer.blocks.get("world:" + FakeWriter.pos(x, 64, 200)).data(), "x=" + x);
        }
        assertSame(mine, writer.blocks.get("world:" + FakeWriter.pos(101, 64, 200)), "lo puesto a mano no se pisa");
        assertSame(outside, writer.blocks.get("world:" + FakeWriter.pos(105, 64, 200)), "lo ajeno no se pisa");
        assertEquals(4, writer.writes);
        assertEquals(2, p.skipped());
    }

    @Test void upgradeNeverOverwritesBlocksWithTheirOwnState() {
        var b = built(def("casa", 4, 1, true, 0, 0));
        BlockData planks = data(Material.OAK_PLANKS);
        Project p = readyUpgrade(upgradeOf("casa2", "casa", 4, planks, null), b);
        writer.states.add("world:" + FakeWriter.pos(102, 64, 200)); // un cofre lleno, igual al del original
        ConstructionEngine e = engine(service);
        assertEquals(ConstructionEngine.StartResult.STARTED, e.start(player(p.owner()), p));
        e.tick();
        settle();
        assertEquals(ProjectState.COMPLETED, p.state());
        assertSame(stone, writer.blocks.get("world:" + FakeWriter.pos(102, 64, 200)).data(), "su contenido no se pierde");
        assertEquals(3, writer.writes);
        assertEquals(1, p.skipped());
    }

    @Test void rotatedUpgradeKeepsTheOriginalCornerInPlace() {
        StructureDefinition old = def("casa", 4, 1, true, 0, 0);
        World w = mock(World.class);
        when(w.getName()).thenReturn("world");
        Project op = service.create(UUID.randomUUID(), old, UUID.randomUUID(), new Location(w, 100, 64, 200),
                Rotation.CW_180).join();
        var b = new com.sack.rpgroll.structures.storage.BuildRecord(UUID.randomUUID(), op.id(), "casa", op.owner(),
                "world", 100, 64, 200, Rotation.CW_180, new Box(100, 64, 200, 103, 64, 200), 1, null);
        assertTrue(repo.complete(op.id(), b).join());
        op.setState(ProjectState.COMPLETED);
        service.finish(op);
        for (int x = 100; x <= 103; x++) {
            writer.blocks.put("world:" + FakeWriter.pos(x, 64, 200), new BlockWriter.BlockInfo(Material.STONE, false,
                    stone));
        }
        // casa2 = la casa (tablones, x local 0..3) + 2 de ampliación (cristal, x local 4..5).
        BlockData planks = data(Material.OAK_PLANKS), glass = data(Material.GLASS);
        StructureDefinition target = upgradeOf("casa2", "casa", 6, planks, null);
        List<SourceBlock> mixed = IntStream.range(0, 6).mapToObj(i -> new SourceBlock(i, 0, 0, i < 4 ? planks : glass,
                null)).toList();
        BlockSource src = new BlockSource() {
            public int width() { return 6; }
            public int height() { return 1; }
            public int length() { return 1; }
            public List<SourceBlock> blocks() { return mixed; }
        };
        target = new StructureDefinition(target.id(), target.names(), target.category(), target.tier(), target.icon(),
                target.schematic(), src, target.materials(), 0, true, target.order(), target.customOrder(), 0, 0,
                target.placement(), target.permission(), null, null, null, "casa");
        defs.put(target.id(), target);
        Project p = readyUpgrade(target, b);
        assertEquals(98, p.x(), "girada 180: la ampliación crece hacia -x");
        ConstructionEngine e = engine(service);
        assertEquals(ConstructionEngine.StartResult.STARTED, e.start(player(p.owner()), p));
        e.tick();
        settle();
        assertEquals(ProjectState.COMPLETED, p.state());
        for (int x = 100; x <= 103; x++) {
            assertSame(planks, writer.blocks.get("world:" + FakeWriter.pos(x, 64, 200)).data(), "x=" + x);
        }
        assertSame(glass, writer.blocks.get("world:" + FakeWriter.pos(98, 64, 200)).data());
        assertSame(glass, writer.blocks.get("world:" + FakeWriter.pos(99, 64, 200)).data());
        assertEquals(new Box(98, 64, 200, 103, 64, 200), repo.buildsOf(b.owner()).join().getFirst().box());
    }

    @Test void upgradeCompletionSwapsBuildRecordAndModules() {
        List<String> calls = new CopyOnWriteArrayList<>();
        com.sack.rpgroll.structures.module.ModuleRegistry registry = new com.sack.rpgroll.structures.module.ModuleRegistry();
        registry.register(new com.sack.rpgroll.structures.api.StructureModule() {
            @Override public String type() { return "fake"; }
            @Override public List<com.sack.rpgroll.structures.catalog.Problem> validate(
                    org.bukkit.configuration.ConfigurationSection c) { return List.of(); }
            @Override public void onBuilt(com.sack.rpgroll.structures.api.BuiltStructure s) {
                calls.add("built " + s.record().structure());
            }
            @Override public void onRemoved(com.sack.rpgroll.structures.api.BuiltStructure s) {
                calls.add("removed " + s.record().structure());
            }
        });
        var modules = new com.sack.rpgroll.structures.module.ModuleService(registry,
                id -> Optional.ofNullable(defs.get(id)), repo, (w, x, z) -> true, Runnable::run, logger);
        upgradeOf("casa", null, 4, stone, "fake"); // la vieja, con módulo
        var b = built(defs.get("casa"));
        modules.load();
        settle();
        StructureDefinition target = upgradeOf("casa2", "casa", 6, stone, "fake");
        Project p = readyUpgrade(target, b);
        ConstructionEngine e = new ConstructionEngine(service, repo, id -> Optional.ofNullable(defs.get(id)),
                () -> perf, writer, Runnable::run, (proj, rec) -> {
                    completed.add(proj);
                    builtRecords.add(rec);
                    modules.completed(proj, rec);
                }, pausedNotices::add, () -> now[0], planner, id -> true, guard, precheck, logger);
        assertEquals(ConstructionEngine.StartResult.STARTED, e.start(player(p.owner()), p));
        e.tick();
        settle();
        assertEquals(ProjectState.COMPLETED, p.state());
        var builds = repo.buildsOf(b.owner()).join();
        assertEquals(1, builds.size(), "la vieja sale y entra la nueva");
        assertEquals("casa2", builds.getFirst().structure());
        assertEquals(new Box(100, 64, 200, 105, 64, 200), builds.getFirst().box());
        assertEquals(builds, builtRecords);
        assertEquals(List.of("removed casa", "built casa2"), calls, "primero el módulo viejo, luego el nuevo");
        assertEquals(2, writer.writes, "la piedra igual no se reescribe");
        assertTrue(repo.active().join().isEmpty());
    }

    @Test void newBuildStoresTheSchematicHash() {
        StructureDefinition base = def("casa", 3, 1, true, 0, 0);
        StructureDefinition d = new StructureDefinition(base.id(), base.names(), base.category(), base.tier(),
                base.icon(), base.schematic(), base.blocks(), base.materials(), 0, true, BuildOrder.BOTTOM_UP,
                List.of(), 0, 0, base.placement(), base.permission(), "abc123");
        defs.put(d.id(), d);
        Project p = ready(d);
        ConstructionEngine e = engine(service);
        e.start(player(p.owner()), p);
        e.tick();
        settle();
        assertEquals("abc123", builtRecords.getFirst().sourceHash());
        assertEquals("abc123", repo.buildsOf(p.owner()).join().getFirst().sourceHash());
    }

    @Test void repairWritesOnlyThePaidBlocksAlsoAfterARestart() {
        StructureDefinition d = def("casa", 10, 1, false, 2, 1); // 2 bloques por tick
        var b = built(d);
        writer.blocks.remove("world:" + FakeWriter.pos(100, 64, 200));
        writer.blocks.remove("world:" + FakeWriter.pos(101, 64, 200));
        Project p = readyRepair(d, b); // se pagan 2
        assertEquals(2, p.repairBlocks());
        // Tras aceptar se rompe más: lo que no se pagó nunca vuelve gratis.
        for (int x = 105; x < 108; x++) {
            writer.blocks.remove("world:" + FakeWriter.pos(x, 64, 200));
        }
        ConstructionEngine e = engine(service);
        assertEquals(ConstructionEngine.StartResult.STARTED, e.start(player(p.owner()), p));
        e.tick();
        assertEquals(2, p.cursor());
        assertEquals(2, writer.writes);
        settle();
        // Reinicio: el presupuesto gastado sale del placed guardado.
        ProjectService fresh = new ProjectService(repo, Runnable::run, Logger.getAnonymousLogger());
        fresh.load();
        Project again = fresh.byId(p.id()).orElseThrow();
        assertEquals(2, again.placed());
        ConstructionEngine e2 = engine(fresh);
        ticks(e2, 10);
        settle();
        assertEquals(ProjectState.COMPLETED, again.state());
        assertEquals(2, writer.writes, "exactamente lo pagado");
        assertEquals(3, again.skipped(), "los huecos sin pagar cuentan como saltados");
        for (int x = 105; x < 108; x++) {
            assertFalse(writer.blocks.containsKey("world:" + FakeWriter.pos(x, 64, 200)));
        }
    }

    @Test void repairWithOneMissingBlockWritesOneEvenIfMoreBreaks() {
        StructureDefinition d = def("casa", 10, 1, true, 0, 0);
        var b = built(d);
        writer.blocks.remove("world:" + FakeWriter.pos(104, 64, 200));
        Project p = readyRepair(d, b);
        for (int x = 100; x < 103; x++) {
            writer.blocks.remove("world:" + FakeWriter.pos(x, 64, 200));
        }
        ConstructionEngine e = engine(service);
        e.start(player(p.owner()), p);
        e.tick();
        settle();
        assertEquals(ProjectState.COMPLETED, p.state());
        assertEquals(1, writer.writes);
    }

    @Test void repairWithoutBuildIdFailsInsteadOfRetrying() {
        def("casa", 3, 1, true, 0, 0);
        UUID owner = UUID.randomUUID();
        var rec = new com.sack.rpgroll.structures.storage.ProjectRecord(UUID.randomUUID(), UUID.randomUUID(), "casa",
                owner, ProjectState.READY, "world", 100, 64, 200, Rotation.NONE, 0, 0, 0, 3, null, 1, 1, null, 0,
                com.sack.rpgroll.structures.project.ProjectKind.REPAIR, null, 3);
        repo.insert(rec, Map.of(KEY, 3), 3, 1, 1).join();
        assertTrue(repo.applyMovement(new Movement(UUID.randomUUID(), rec.id(), owner, Movement.Kind.DEPOSIT, KEY, 3))
                .join());
        ProjectService fresh = new ProjectService(repo, Runnable::run, Logger.getAnonymousLogger());
        fresh.load();
        Project p = fresh.byId(rec.id()).orElseThrow();
        ConstructionEngine e = engine(fresh);
        assertEquals(ConstructionEngine.StartResult.STARTED, e.start(player(owner), p));
        ticks(e, 3);
        settle();
        assertEquals(ProjectState.ERROR, p.state());
        assertTrue(completed.isEmpty());
        assertTrue(repo.buildsOf(owner).join().isEmpty());
    }
}
