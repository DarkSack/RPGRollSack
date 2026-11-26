package com.sack.rpgroll.structures.project;

import com.sack.rpgroll.common.structure.BlockSource;
import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.catalog.MaterialSpec;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.module.ModuleRegistry;
import com.sack.rpgroll.structures.module.ModuleService;
import com.sack.rpgroll.structures.storage.Box;
import com.sack.rpgroll.structures.storage.BuildRecord;
import com.sack.rpgroll.structures.storage.Database;
import com.sack.rpgroll.structures.storage.Dialect;
import com.sack.rpgroll.structures.storage.MaterialRow;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import com.sack.rpgroll.structures.workbench.WorkbenchState;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectServiceTest {

    @TempDir Path dir;
    Database db;
    ProjectRepository repo;
    ProjectService service;
    final UUID owner = UUID.randomUUID();
    final UUID bench = UUID.randomUUID();
    final World world = mock(World.class);
    final Location origin = new Location(world, 10, 64, 20);

    @BeforeEach void setUp() throws Exception {
        when(world.getName()).thenReturn("world");
        db = Database.open(Dialect.SQLITE, "jdbc:sqlite:" + dir.resolve("data.db"), "", "",
                Logger.getAnonymousLogger());
        repo = new ProjectRepository(db, Dialect.SQLITE);
        service = new ProjectService(repo, Runnable::run, Logger.getAnonymousLogger());
    }

    @AfterEach void tearDown() {
        db.close();
    }

    StructureDefinition def() {
        List<SourceBlock> blocks = java.util.stream.IntStream.range(0, 12)
                .mapToObj(i -> new SourceBlock(i, 0, 0, null, null)).toList();
        BlockSource src = new BlockSource() {
            public int width() { return 4; }
            public int height() { return 1; }
            public int length() { return 3; }
            public List<SourceBlock> blocks() { return blocks; }
        };
        return new StructureDefinition("casa", Map.of(), "casas", null, null, null, src,
                List.of(new MaterialSpec(List.of("minecraft:oak_planks"), 8, false),
                        new MaterialSpec(List.of("minecraft:torch"), 2, true)),
                0, false, null, null, 0, 0, null, null);
    }

    @Test void createStoresProjectAndMaterials() {
        long before = System.currentTimeMillis();
        Project p = service.create(bench, def(), owner, origin, Rotation.CW_90).join();
        assertEquals(ProjectState.WAITING_MATERIALS, p.state());
        assertTrue(p.createdAt() >= before && p.createdAt() <= System.currentTimeMillis(), "fecha de creación");
        assertEquals(12, p.total());
        assertEquals(Map.of("minecraft:oak_planks", new MaterialRow(8, 0, 0),
                "minecraft:torch", new MaterialRow(2, 0, 0)), p.materials());
        assertTrue(service.hasActive(bench));
        assertSame(p, service.byId(p.id()).orElseThrow());
        assertEquals(List.of(p), service.ofPlayer(owner));
    }

    @Test void oneActiveProjectPerBench() {
        service.create(bench, def(), owner, origin, Rotation.NONE).join();
        assertThrows(IllegalStateException.class, () -> service.create(bench, def(), owner, origin, Rotation.NONE));
    }

    @Test void loadRestoresActiveProjects() {
        Project p = service.create(bench, def(), owner, origin, Rotation.NONE).join();
        ProjectService fresh = new ProjectService(repo, Runnable::run, Logger.getAnonymousLogger());
        fresh.load();
        assertTrue(fresh.hasActive(bench));
        assertEquals(p.materials(), fresh.forBench(bench).orElseThrow().materials());
        assertEquals(p.createdAt(), fresh.forBench(bench).orElseThrow().createdAt());
    }

    @Test void loadHealsCompleteWaitingProject() {
        Project p = service.create(bench, def(), owner, origin, Rotation.NONE).join();
        assertTrue(repo.applyMovement(new com.sack.rpgroll.structures.storage.Movement(UUID.randomUUID(), p.id(), owner,
                com.sack.rpgroll.structures.storage.Movement.Kind.DEPOSIT, "minecraft:oak_planks", 8)).join());
        ProjectService fresh = new ProjectService(repo, Runnable::run, Logger.getAnonymousLogger(),
                id -> def().materials());
        fresh.load();
        assertEquals(ProjectState.READY, fresh.forBench(bench).orElseThrow().state());
        assertEquals(ProjectState.READY, repo.active().join().getFirst().state());
    }

    @Test void failedHealSaveDoesNotStopLoading() {
        Project p = service.create(bench, def(), owner, origin, Rotation.NONE).join();
        assertTrue(repo.applyMovement(new com.sack.rpgroll.structures.storage.Movement(UUID.randomUUID(), p.id(), owner,
                com.sack.rpgroll.structures.storage.Movement.Kind.DEPOSIT, "minecraft:oak_planks", 8)).join());
        ProjectRepository bad = org.mockito.Mockito.spy(repo);
        org.mockito.Mockito.doReturn(java.util.concurrent.CompletableFuture.failedFuture(new RuntimeException("bd")))
                .when(bad).saveProgress(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
        ProjectService fresh = new ProjectService(bad, Runnable::run, Logger.getAnonymousLogger(),
                id -> def().materials());
        fresh.load();
        assertTrue(fresh.hasActive(bench));
    }

    @Test void benchStateFollowsProject() {
        assertEquals(WorkbenchState.IDLE, service.benchState(bench));
        Project p = service.create(bench, def(), owner, origin, Rotation.NONE).join();
        assertEquals(WorkbenchState.WAITING_MATERIALS, service.benchState(bench));
        p.setState(ProjectState.BUILDING);
        assertEquals(WorkbenchState.BUILDING, service.benchState(bench));
        p.setState(ProjectState.ERROR);
        assertEquals(WorkbenchState.ERROR, service.benchState(bench));
        p.setState(ProjectState.COMPLETED);
        service.finish(p);
        assertFalse(service.hasActive(bench));
        assertEquals(WorkbenchState.COMPLETED, service.benchState(bench));
        service.acknowledge(bench);
        assertEquals(WorkbenchState.IDLE, service.benchState(bench));
    }

    /** Repositorio cuyo insert queda en vuelo hasta que la prueba lo complete. */
    ProjectRepository slowRepo(List<java.util.concurrent.CompletableFuture<Void>> inserts) {
        ProjectRepository slow = mock(ProjectRepository.class);
        when(slow.insert(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt())).thenAnswer(i -> {
                    var f = new java.util.concurrent.CompletableFuture<Void>();
                    inserts.add(f);
                    return f;
                });
        return slow;
    }

    @Test void overlappingCreatesOnDifferentBenchesOnlyOneWins() {
        List<java.util.concurrent.CompletableFuture<Void>> inserts = new java.util.ArrayList<>();
        ProjectService s = new ProjectService(slowRepo(inserts), Runnable::run, Logger.getAnonymousLogger());
        // casa: 4x1x3 en (10,64,20) → hasta (13,64,22)
        var first = s.create(UUID.randomUUID(), def(), owner, origin, Rotation.NONE);
        assertThrows(ProjectService.OverlapException.class, () -> s.create(UUID.randomUUID(), def(), owner,
                new Location(world, 13, 64, 22), Rotation.NONE), "choca con la que aún se está guardando");
        // Al lado (x = 14) no choca, aunque la primera siga en vuelo.
        s.create(UUID.randomUUID(), def(), owner, new Location(world, 14, 64, 20), Rotation.NONE);
        // Otro mundo, misma esquina: no choca.
        World nether = mock(World.class);
        when(nether.getName()).thenReturn("nether");
        s.create(UUID.randomUUID(), def(), owner, new Location(nether, 10, 64, 20), Rotation.NONE);
        assertEquals(3, inserts.size(), "el rechazado no llegó a la base de datos");

        inserts.get(0).complete(null);
        Project p = first.join();
        assertThrows(ProjectService.OverlapException.class, () -> s.create(UUID.randomUUID(), def(), owner,
                new Location(world, 10, 64, 19), Rotation.NONE), "ya activa en memoria");
        p.setState(ProjectState.CANCELLED);
        s.finish(p);
        s.create(UUID.randomUUID(), def(), owner, new Location(world, 10, 64, 19), Rotation.NONE);
    }

    @Test void failedInsertFreesItsBox() {
        List<java.util.concurrent.CompletableFuture<Void>> inserts = new java.util.ArrayList<>();
        ProjectService s = new ProjectService(slowRepo(inserts), Runnable::run, Logger.getAnonymousLogger());
        var first = s.create(UUID.randomUUID(), def(), owner, origin, Rotation.NONE);
        inserts.get(0).completeExceptionally(new RuntimeException("db"));
        assertThrows(CompletionException.class, first::join);
        s.create(UUID.randomUUID(), def(), owner, origin, Rotation.NONE);
    }

    @Test void loadedProjectsKeepTheirBoxForOverlaps() {
        service.create(bench, def(), owner, origin, Rotation.CW_90).join(); // girada: 3 de ancho x 4 de largo
        ProjectService fresh = new ProjectService(repo, Runnable::run, Logger.getAnonymousLogger());
        fresh.load();
        assertThrows(ProjectService.OverlapException.class, () -> fresh.create(UUID.randomUUID(), def(), owner,
                new Location(world, 12, 64, 23), Rotation.NONE));
        fresh.create(UUID.randomUUID(), def(), owner, new Location(world, 13, 64, 20), Rotation.NONE).join();
    }

    @Test void failedCreateDoesNotLockBench() {
        ProjectRepository bad = mock(ProjectRepository.class);
        when(bad.insert(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new IllegalArgumentException("boom"))
                .thenReturn(java.util.concurrent.CompletableFuture.failedFuture(new RuntimeException("db")))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        ProjectService s = new ProjectService(bad, Runnable::run, Logger.getAnonymousLogger());
        assertThrows(CompletionException.class, () -> s.create(bench, def(), owner, origin, Rotation.NONE).join());
        assertFalse(s.hasActive(bench));
        assertThrows(CompletionException.class, () -> s.create(bench, def(), owner, origin, Rotation.NONE).join());
        assertNotNull(s.create(bench, def(), owner, origin, Rotation.NONE).join());
    }

    @Test void errorWithCursorAtTheEndIsRetriedOnLoad() {
        Project done = service.create(bench, def(), owner, origin, Rotation.NONE).join();
        assertTrue(repo.saveProgress(done.id(), ProjectState.ERROR, 12, 12, 0, "no se pudo registrar").join());
        Project broken = service.create(UUID.randomUUID(), def(), owner, new Location(world, 500, 64, 20),
                Rotation.NONE).join();
        assertTrue(repo.saveProgress(broken.id(), ProjectState.ERROR, 5, 5, 0, "bloque 5").join());
        ProjectService fresh = new ProjectService(repo, Runnable::run, Logger.getAnonymousLogger());
        fresh.load();
        Project again = fresh.byId(done.id()).orElseThrow();
        assertEquals(ProjectState.BUILDING, again.state(), "el motor reintenta registrarla");
        assertNull(again.error());
        assertEquals(ProjectState.ERROR, fresh.byId(broken.id()).orElseThrow().state());
        assertEquals(ProjectState.BUILDING, repo.active().join().stream().filter(r -> r.id().equals(done.id()))
                .findFirst().orElseThrow().state());
    }

    @Test void pausedForProtectionStaysPausedAfterReload() {
        Project p = service.create(bench, def(), owner, origin, Rotation.NONE).join();
        assertTrue(repo.saveProgress(p.id(), ProjectState.PAUSED, 4, 4, 0, "project.paused.protected").join());
        ProjectService fresh = new ProjectService(repo, Runnable::run, Logger.getAnonymousLogger(),
                id -> def().materials());
        fresh.load();
        Project again = fresh.byId(p.id()).orElseThrow();
        assertEquals(ProjectState.PAUSED, again.state());
        assertEquals("project.paused.protected", again.error());
        assertEquals(4, again.cursor());
        // El motor no la toca: ni la planifica, ni la comprueba, ni escribe.
        com.sack.rpgroll.structures.build.BlockWriter writer =
                mock(com.sack.rpgroll.structures.build.BlockWriter.class);
        int[] checks = {0};
        var engine = new com.sack.rpgroll.structures.build.ConstructionEngine(fresh, repo,
                id -> java.util.Optional.of(def()), () -> com.sack.rpgroll.structures.config.PerformanceSettings
                        .from(new org.bukkit.configuration.file.YamlConfiguration(), w -> { }),
                writer, Runnable::run, (x, b) -> { }, x -> { }, () -> 0L, r -> fail("no se planifica"), id -> true,
                (o, w, b) -> {
                    checks[0]++;
                    return java.util.Optional.empty();
                }, (d, o, w, b) -> java.util.Optional.empty(), Logger.getAnonymousLogger());
        for (int i = 0; i < 5; i++) {
            engine.tick();
        }
        org.mockito.Mockito.verifyNoInteractions(writer);
        assertEquals(0, checks[0]);
        assertEquals(ProjectState.PAUSED, again.state());
        assertEquals("project.paused.protected", repo.active().join().getFirst().error());
    }

    @Test void createStoresTheSchematicHash() {
        StructureDefinition d = def();
        StructureDefinition h = new StructureDefinition(d.id(), d.names(), d.category(), d.tier(), d.icon(),
                d.schematic(), d.blocks(), d.materials(), d.money(), d.instant(), d.order(), d.customOrder(),
                d.blocksPerCycle(), d.ticksPerCycle(), d.placement(), d.permission(), "abc123");
        Project p = service.create(bench, h, owner, origin, Rotation.NONE).join();
        assertEquals("abc123", p.sourceHash());
        ProjectService fresh = new ProjectService(repo, Runnable::run, Logger.getAnonymousLogger());
        fresh.load();
        assertEquals("abc123", fresh.byId(p.id()).orElseThrow().sourceHash());
    }
    StructureDefinition def(String hash) {
        StructureDefinition d = def();
        return new StructureDefinition(d.id(), d.names(), d.category(), d.tier(), d.icon(), d.schematic(), d.blocks(),
                d.materials(), d.money(), d.instant(), d.order(), d.customOrder(), d.blocksPerCycle(),
                d.ticksPerCycle(), d.placement(), d.permission(), hash);
    }

    /** Construcción terminada de la casa en {@code at} (sin girar: 4x1x3), con su obra ya fuera de la memoria. */
    BuildRecord built(Location at, String hash) {
        Project p = service.create(UUID.randomUUID(), def(), owner, at, Rotation.NONE).join();
        int x = at.getBlockX(), y = at.getBlockY(), z = at.getBlockZ();
        BuildRecord b = new BuildRecord(UUID.randomUUID(), p.id(), "casa", owner, "world", x, y, z, Rotation.NONE,
                new Box(x, y, z, x + 3, y, z + 2), 1, hash);
        assertTrue(repo.complete(p.id(), b).join());
        p.setState(ProjectState.COMPLETED);
        service.finish(p);
        return b;
    }

    @Test void repairOverlapExemptsOnlyItsOwnBuild() {
        BuildRecord b = built(origin, null);
        assertTrue(repo.boxesIn("world").join().contains(b.box()), "cuenta para la vista previa");
        // Otra obra que pisa una esquina de la construcción sigue chocando con la reparación.
        Project other = service.create(UUID.randomUUID(), def(), owner, new Location(world, 13, 64, 22),
                Rotation.NONE).join();
        Map<String, Integer> price = Map.of("minecraft:oak_planks", 2, "minecraft:torch", 1);
        assertThrows(ProjectService.OverlapException.class, () -> service.createRepair(bench, def(), b, price, 3));
        assertFalse(service.hasActive(bench));
        other.setState(ProjectState.CANCELLED);
        service.finish(other);

        Project r = service.createRepair(bench, def(), b, price, 3).join();
        assertEquals(ProjectKind.REPAIR, r.kind());
        assertEquals(b.id(), r.buildId());
        assertEquals(List.of(10, 64, 20, Rotation.NONE), List.of(r.x(), r.y(), r.z(), r.rotation()));
        assertEquals(12, r.total(), "el plan completo: el motor salta lo que ya está");
        assertEquals(3, r.repairBlocks(), "bloques pagados");
        assertEquals(Map.of("minecraft:oak_planks", new MaterialRow(2, 0, 0),
                "minecraft:torch", new MaterialRow(1, 0, 0)), r.materials());
        // Dos reparaciones de la misma construcción a la vez: la segunda choca con la primera.
        assertThrows(ProjectService.OverlapException.class,
                () -> service.createRepair(UUID.randomUUID(), def(), b, price, 3));

        ProjectService fresh = new ProjectService(repo, Runnable::run, Logger.getAnonymousLogger());
        fresh.load();
        Project again = fresh.byId(r.id()).orElseThrow();
        assertEquals(ProjectKind.REPAIR, again.kind());
        assertEquals(b.id(), again.buildId());
        assertEquals(3, again.repairBlocks());
        assertEquals(ProjectKind.BUILD, service.create(UUID.randomUUID(), def(), owner,
                new Location(world, 500, 64, 20), Rotation.NONE).join().kind());
    }

    @Test void repairOfVanishedBuildRemovesRecord() {
        BuildRecord b = built(origin, null);
        ModuleService modules = new ModuleService(new ModuleRegistry(), id -> Optional.empty(), repo,
                (w, x, z) -> true, Runnable::run, Logger.getAnonymousLogger());
        ProjectService.RepairException nothing = assertThrows(ProjectService.RepairException.class,
                () -> service.repair(bench, def(), b, new RepairMath.Result(0, 12), null, modules::remove));
        assertEquals("repair.nothing", nothing.key());
        assertEquals(List.of(b), repo.buildsOf(owner).join());

        ProjectService.RepairException gone = assertThrows(ProjectService.RepairException.class,
                () -> service.repair(bench, def(), b, new RepairMath.Result(12, 12), null, modules::remove));
        assertEquals("repair.vanished", gone.key());
        assertTrue(repo.buildsOf(owner).join().isEmpty(), "se borra el registro");
        assertFalse(service.hasActive(bench), "no hay reparación");

        // Con parte en pie: obra de reparación por el precio proporcional.
        BuildRecord c = built(new Location(world, 300, 64, 20), null);
        Project r = service.repair(bench, def(), c, new RepairMath.Result(3, 12), null, modules::remove).join();
        assertEquals(Map.of("minecraft:oak_planks", new MaterialRow(2, 0, 0),
                "minecraft:torch", new MaterialRow(1, 0, 0)), r.materials());
        assertEquals(0, r.moneyCents());
        assertEquals(3, r.repairBlocks());
    }

    /** «casa2»: 6x1x3, mejora de «casa», con otro precio. */
    StructureDefinition bigger() {
        List<SourceBlock> blocks = java.util.stream.IntStream.range(0, 18)
                .mapToObj(i -> new SourceBlock(i % 6, 0, i / 6, null, null)).toList();
        BlockSource src = new BlockSource() {
            public int width() { return 6; }
            public int height() { return 1; }
            public int length() { return 3; }
            public List<SourceBlock> blocks() { return blocks; }
        };
        return new StructureDefinition("casa2", Map.of(), "casas", null, null, null, src,
                List.of(new MaterialSpec(List.of("minecraft:stone"), 5, false)), 0, false, null, null, 0, 0, null,
                null, "h2", null, null, "casa");
    }

    @Test void upgradeOverlapExemptsOnlyItsOwnBuild() {
        BuildRecord b = built(origin, null); // 10..13 x 20..22; la mejora llega a x=15
        BuildRecord neighbour = built(new Location(world, 15, 64, 30), null); // fuera
        BuildRecord blocking = built(new Location(world, 15, 64, 22), null); // cruza la caja nueva, no la vieja
        java.util.concurrent.CompletionException e = assertThrows(java.util.concurrent.CompletionException.class,
                () -> service.createUpgrade(bench, bigger(), b, def()).join());
        assertInstanceOf(ProjectService.OverlapException.class, e.getCause(), "otra construcción terminada choca");
        assertFalse(service.hasActive(bench), "la mesa queda libre");
        assertTrue(repo.active().join().isEmpty(), "no queda obra a medias");
        assertNotNull(neighbour);

        assertTrue(repo.deleteBuild(blocking.id()).join());
        // Una obra activa que cruza la caja nueva también choca (en memoria, al momento).
        Project other = service.create(UUID.randomUUID(), def(), owner, new Location(world, 15, 64, 20),
                Rotation.NONE).join();
        assertThrows(ProjectService.OverlapException.class, () -> service.createUpgrade(bench, bigger(), b, def()));
        other.setState(ProjectState.CANCELLED);
        service.finish(other);

        Project u = service.createUpgrade(bench, bigger(), b, def()).join(); // su propia caja no cuenta
        assertEquals(ProjectKind.UPGRADE, u.kind());
        assertEquals(b.id(), u.buildId());
        assertEquals("casa2", u.structure());
        assertEquals(List.of(10, 64, 20, Rotation.NONE), List.of(u.x(), u.y(), u.z(), u.rotation()));
        assertEquals(18, u.total());
        assertEquals("h2", u.sourceHash());
        assertEquals(Map.of("minecraft:stone", new MaterialRow(5, 0, 0)), u.materials(), "precio: el YAML nuevo");
        // Una reparación de la misma construcción mientras se mejora: choca.
        assertThrows(ProjectService.OverlapException.class, () -> service.createRepair(UUID.randomUUID(), def(), b,
                Map.of("minecraft:oak_planks", 1), 1));
        // Una estructura que no mejora esa construcción no vale.
        assertThrows(IllegalArgumentException.class, () -> service.createUpgrade(UUID.randomUUID(), def(), b, def()));

        ProjectService fresh = new ProjectService(repo, Runnable::run, Logger.getAnonymousLogger());
        fresh.load();
        assertEquals(ProjectKind.UPGRADE, fresh.byId(u.id()).orElseThrow().kind());
    }

    @Test void upgradeOfADeletedBuildIsRefused() {
        BuildRecord b = built(origin, null);
        assertTrue(repo.deleteBuild(b.id()).join()); // p. ej. «repair.vanished» con el menú aún abierto
        java.util.concurrent.CompletionException e = assertThrows(java.util.concurrent.CompletionException.class,
                () -> service.createUpgrade(bench, bigger(), b, def()).join());
        ProjectService.RepairException gone = assertInstanceOf(ProjectService.RepairException.class, e.getCause());
        assertEquals("build-upgrade.gone", gone.key());
        assertFalse(service.hasActive(bench));
        assertTrue(repo.active().join().isEmpty());
    }

    @Test void upgradeShiftKeepsTheOriginalCornerInPlace() {
        // Original 4x3, mejora 6x5 (sin girar). La esquina local (0,0) no se mueve en el mundo.
        assertArrayEquals(new int[] {0, 0}, ProjectService.upgradeShift(Rotation.NONE, 4, 3, 6, 5));
        assertArrayEquals(new int[] {-2, 0}, ProjectService.upgradeShift(Rotation.CW_90, 4, 3, 6, 5));
        assertArrayEquals(new int[] {-2, -2}, ProjectService.upgradeShift(Rotation.CW_180, 4, 3, 6, 5));
        assertArrayEquals(new int[] {0, -2}, ProjectService.upgradeShift(Rotation.CCW_90, 4, 3, 6, 5));
        for (Rotation r : Rotation.values()) {
            int[] s = ProjectService.upgradeShift(r, 4, 3, 6, 5);
            int[] old = r.apply(0, 0, 4, 3), neu = r.apply(0, 0, 6, 5);
            assertArrayEquals(old, new int[] {neu[0] + s[0], neu[1] + s[1]}, r.name());
        }
    }

    @Test void repairRefusedWhenHashChanged() {
        BuildRecord b = built(origin, "viejo");
        ProjectService.RepairException e = assertThrows(ProjectService.RepairException.class,
                () -> service.createRepair(bench, def("nuevo"), b, Map.of("minecraft:oak_planks", 1), 1));
        assertEquals("repair.changed", e.key());
        assertFalse(RepairMath.matches(def("nuevo"), b));
        assertFalse(service.hasActive(bench));
        // Mismo hash, o construcción de antes de la v6 (sin hash): se puede.
        assertTrue(RepairMath.matches(def("viejo"), b));
        BuildRecord old = built(new Location(world, 300, 64, 20), null);
        assertTrue(RepairMath.matches(def("nuevo"), old));
        assertEquals(ProjectKind.REPAIR, service.createRepair(bench, def("nuevo"), old,
                Map.of("minecraft:oak_planks", 1), 1).join().kind());
        // Otra medida (el esquemático cambió sin hash que lo diga): tampoco.
        BuildRecord moved = new BuildRecord(UUID.randomUUID(), UUID.randomUUID(), "casa", owner, "world", 600, 64, 20,
                Rotation.NONE, new Box(600, 64, 20, 605, 64, 22), 1, null);
        assertFalse(RepairMath.matches(def(), moved));
    }
}
