package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.structure.BlockSource;
import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.build.BlockWriter;
import com.sack.rpgroll.structures.build.ConstructionEngine;
import com.sack.rpgroll.structures.catalog.BuildOrder;
import com.sack.rpgroll.structures.catalog.MaterialSpec;
import com.sack.rpgroll.structures.catalog.PlacementRules;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.config.PerformanceSettings;
import com.sack.rpgroll.structures.deposit.ReturnService;
import com.sack.rpgroll.structures.integration.Hook;
import com.sack.rpgroll.structures.project.Project;
import com.sack.rpgroll.structures.project.ProjectService;
import com.sack.rpgroll.structures.project.ProjectState;
import com.sack.rpgroll.structures.protect.AreaGuard;
import com.sack.rpgroll.structures.storage.ProjectRecord;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import com.sack.rpgroll.structures.workbench.BreakRules.BreakDecision;
import com.sack.rpgroll.structures.config.StructuresSettings;
import com.sack.rpgroll.structures.config.WorkbenchSettings;
import com.sack.rpgroll.structures.storage.Database;
import com.sack.rpgroll.structures.storage.Dialect;
import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import com.sack.rpgroll.structures.storage.WorkbenchRepository;
import com.sack.rpgroll.structures.workbench.UpgradeRules.UpgradeCheck;
import com.sack.rpgroll.structures.workbench.WorkbenchService.PlaceResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static com.sack.rpgroll.structures.workbench.WorkbenchTier.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WorkbenchServiceTest {

    @TempDir Path dir;
    Database db;
    WorkbenchRepository repo;
    WorkbenchService service;
    WorkbenchDisplay display;
    final SerialSigner signer = new SerialSigner(new byte[32]);
    final WorkbenchItems items = mock(WorkbenchItems.class);
    final Map<ItemStack, WorkbenchStamp> stamps = new HashMap<>();
    final UUID ownerId = UUID.randomUUID();
    final Player p = mock(Player.class);
    final PlayerInventory inv = mock(PlayerInventory.class);
    final World world = mock(World.class);
    final Block block = mock(Block.class);
    final AtomicInteger planks = new AtomicInteger(64);
    final ItemStack planksStack = mock(ItemStack.class);
    boolean worldExists = true;
    volatile boolean queued;
    StructuresSettings.BreakPolicy policy = StructuresSettings.BreakPolicy.BLOCK;
    ProjectsView projectsView = ProjectsView.none();
    final java.util.Queue<Runnable> queue = new java.util.concurrent.ConcurrentLinkedQueue<>();

    /** Ejecuta las tareas del hilo principal simulado hasta que el futuro termine. */
    <T> T pump(java.util.concurrent.CompletableFuture<T> f) {
        long end = System.currentTimeMillis() + 5000;
        while (!f.isDone() && System.currentTimeMillis() < end) {
            Runnable r = queue.poll();
            if (r != null) {
                r.run();
            } else {
                Thread.onSpinWait();
            }
        }
        return f.join();
    }

    @BeforeEach void setUp() throws Exception {
        db = Database.open(Dialect.SQLITE, "jdbc:sqlite:" + dir.resolve("data.db"), "", "", Logger.getAnonymousLogger());
        repo = new WorkbenchRepository(db, Dialect.SQLITE);
        display = mock(WorkbenchDisplay.class);

        when(items.read(any())).thenAnswer(i -> Optional.ofNullable(stamps.get(i.<ItemStack>getArgument(0))));
        when(items.isGenuine(any(), any())).thenAnswer(i -> signer.verify(i.<WorkbenchStamp>getArgument(0).id(), i.<WorkbenchStamp>getArgument(0).tier(),
                i.<WorkbenchStamp>getArgument(0).serial(), i.<WorkbenchStamp>getArgument(0).signature()));
        when(items.create(any(), any(), any())).thenAnswer(i -> item(i.getArgument(0)));

        when(p.getUniqueId()).thenReturn(ownerId);
        when(p.hasPermission(anyString())).thenReturn(true);
        when(p.getInventory()).thenReturn(inv);
        when(world.getName()).thenReturn("world");
        when(block.getWorld()).thenReturn(world);
        when(block.getX()).thenReturn(10);
        when(block.getY()).thenReturn(64);
        when(block.getZ()).thenReturn(10);

        when(planksStack.getType()).thenReturn(Material.OAK_PLANKS);
        when(planksStack.getAmount()).thenAnswer(i -> planks.get());
        doAnswer(i -> {
            planks.set(i.getArgument(0));
            return null;
        }).when(planksStack).setAmount(anyInt());
        when(inv.getContents()).thenReturn(new ItemStack[] {planksStack});

        service = newService(settings(""));
    }

    @AfterEach void tearDown() {
        db.close();
    }

    WorkbenchService newService(WorkbenchSettings ws) {
        WorkbenchService s = new WorkbenchService(repo, items, signer, mock(LangManager.class), () -> ws,
                () -> new StructuresSettings("es", "", policy), projectsView,
                display, name -> worldExists, r -> { if (queued) queue.add(r); else r.run(); }, Logger.getAnonymousLogger());
        s.stackFactory = (m, n) -> mock(ItemStack.class);
        return s;
    }

    static WorkbenchSettings settings(String extra) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(extra);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return WorkbenchSettings.from(y, s -> {}, m -> true);
    }

    /** El nivel que mostraría un menú recién abierto. */
    WorkbenchTier shown(WorkbenchStamp s) {
        return service.byId(s.id()).orElseThrow().tier();
    }

    ItemStack item(WorkbenchStamp s) {
        ItemStack stack = mock(ItemStack.class);
        stamps.put(stack, s);
        return stack;
    }

    WorkbenchStamp genuine(WorkbenchTier tier, long serial) {
        UUID id = UUID.randomUUID();
        return new WorkbenchStamp(id, tier, serial, signer.sign(id, tier, serial));
    }

    @Test void forgedItemIsNotPlaced() {
        WorkbenchStamp s = genuine(CONSTRUCTOR, 1);
        ItemStack forged = item(new WorkbenchStamp(s.id(), ARQUITECTO, 1, s.signature()));
        assertEquals(PlaceResult.NOT_GENUINE, service.place(p, forged, block).join());
        assertTrue(repo.all().join().isEmpty());
        assertTrue(service.at(block).isEmpty());
    }

    @Test void breakReturnsTheSameIdentity() {
        WorkbenchStamp s = genuine(INGENIERO, 42);
        assertEquals(PlaceResult.OK, service.place(p, item(s), block).join());
        assertEquals(1, repo.all().join().size());

        ItemStack back = service.breakBench(p, block).join().dropped().orElseThrow();
        assertEquals(Optional.of(s), items.read(back));
        assertTrue(repo.all().join().isEmpty());
        assertTrue(service.at(block).isEmpty());
    }

    @Test void breakingTwiceGivesOneItem() {
        service.place(p, item(genuine(CONSTRUCTOR, 1)), block).join();
        assertTrue(service.breakBench(p, block).join().dropped().isPresent());
        assertTrue(service.breakBench(p, block).join().dropped().isEmpty());
    }

    @Test void sameItemCannotBePlacedTwice() {
        WorkbenchStamp s = genuine(CONSTRUCTOR, 3);
        assertEquals(PlaceResult.OK, service.place(p, item(s), block).join());
        Block other = mock(Block.class);
        when(other.getWorld()).thenReturn(world);
        when(other.getX()).thenReturn(99);
        assertEquals(PlaceResult.OCCUPIED, service.place(p, item(s), other).join());
        assertEquals(1, repo.all().join().size());
    }

    @Test void nonOwnerCannotBreak() {
        service.place(p, item(genuine(CONSTRUCTOR, 1)), block).join();
        Player other = mock(Player.class);
        when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        when(other.hasPermission(WorkbenchService.PERM_BREAK)).thenReturn(true);
        assertTrue(service.breakBench(other, block).join().dropped().isEmpty());
        assertEquals(1, repo.all().join().size());
    }

    static final String COST = """
            tiers:
              arquitecto:
                upgrade-cost:
                  - {item: "minecraft:oak_planks", amount: 2}
            """;

    @Test void doubleUpgradeChargesOnce() {
        service = newService(settings(COST));
        WorkbenchStamp s = genuine(INGENIERO, 5);
        service.place(p, item(s), block).join();

        assertInstanceOf(UpgradeRules.Ok.class, service.upgrade(p, s.id(), shown(s)).join());
        assertInstanceOf(UpgradeRules.MaxTier.class, service.upgrade(p, s.id(), shown(s)).join());

        assertEquals(62, planks.get());
        assertEquals(ARQUITECTO, repo.find(s.id()).join().orElseThrow().tier());
        verify(display, times(1)).refresh(any());
    }

    @Test void staleMenuDoesNotUpgradeTwice() {
        service = newService(settings(COST_INGENIERO));
        WorkbenchStamp s = genuine(CONSTRUCTOR, 5);
        service.place(p, item(s), block).join();

        // Dos gestores con el menú abierto en CONSTRUCTOR confirman uno detrás de otro.
        assertInstanceOf(UpgradeRules.Ok.class, service.upgrade(p, s.id(), CONSTRUCTOR).join());
        assertInstanceOf(UpgradeRules.MaxTier.class, service.upgrade(p, s.id(), CONSTRUCTOR).join());

        assertEquals(62, planks.get());
        assertEquals(INGENIERO, repo.find(s.id()).join().orElseThrow().tier());
        assertEquals(INGENIERO, service.byId(s.id()).orElseThrow().tier());
        verify(display, times(1)).refresh(any());
    }

    @Test void failedUpgradeWriteRefunds() {
        service = newService(settings(COST));
        WorkbenchStamp s = genuine(INGENIERO, 5);
        service.place(p, item(s), block).join();
        // Otro proceso ya cambió el nivel en la base: la escritura condicional no toca nada.
        repo.upgrade(s.id(), INGENIERO, ARQUITECTO).join();

        UpgradeCheck result = service.upgrade(p, s.id(), shown(s)).join();

        assertInstanceOf(UpgradeRules.MaxTier.class, result);
        verify(inv).addItem(any(ItemStack.class));
        verify(display, never()).refresh(any());
    }

    Block blockIn(String worldName, int x, int y, int z) {
        World w = mock(World.class);
        when(w.getName()).thenReturn(worldName);
        Block b = mock(Block.class);
        when(b.getWorld()).thenReturn(w);
        when(b.getX()).thenReturn(x);
        when(b.getY()).thenReturn(y);
        when(b.getZ()).thenReturn(z);
        return b;
    }

    @Test void worldNotLoadedYetKeepsTheRowIntact() {
        WorkbenchRecord r = new WorkbenchRecord(UUID.randomUUID(), ownerId, "late", 1, 2, 3, CONSTRUCTOR, 9, false, 1);
        repo.insert(r).join();
        worldExists = false;

        service.load();

        // Mundos de WorldForge o Multiverse cargan después de onEnable: ni la base ni la caché la dan por huérfana.
        assertFalse(repo.find(r.id()).join().orElseThrow().orphan());
        assertFalse(service.byId(r.id()).orElseThrow().orphan());
        assertTrue(service.at(blockIn("late", 1, 2, 3)).isEmpty());

        service.worldLoaded("late");

        assertEquals(Optional.of(r), service.at(blockIn("late", 1, 2, 3)));
        assertEquals(1, service.inChunk("late", 0, 0).size());
    }

    @Test void corruptRowIsSkippedOnLoad() {
        WorkbenchRecord good = new WorkbenchRecord(UUID.randomUUID(), ownerId, "world", 1, 2, 3, CONSTRUCTOR, 9, false, 1);
        repo.insert(good).join();
        db.submit(c -> {
            try (var st = c.createStatement()) {
                st.executeUpdate("INSERT INTO workbenches(id, owner, world, x, y, z, tier, serial, orphan, created_at)"
                        + " VALUES ('no-uuid', '" + ownerId + "', 'world', 4, 5, 6, 'MAESTRO', 77, 0, 1)");
            }
            return null;
        }).join();

        assertDoesNotThrow(service::load);
        assertEquals(Optional.of(good), service.at(blockIn("world", 1, 2, 3)));
    }

    static final String COST_INGENIERO = """
            tiers:
              ingeniero:
                upgrade-cost:
                  - {item: "minecraft:oak_planks", amount: 2}
            """;

    @Test void upgradeCostOnlyCountsAndTakesPlainItems() {
        service = newService(settings(COST_INGENIERO));
        // Un ítem de otro plugin (p. ej. una reliquia) con el mismo material: tiene meta y no cuenta.
        ItemStack relic = mock(ItemStack.class);
        when(relic.getType()).thenReturn(Material.OAK_PLANKS);
        when(relic.getAmount()).thenReturn(64);
        when(relic.hasItemMeta()).thenReturn(true);
        when(inv.getContents()).thenReturn(new ItemStack[] {relic, planksStack});
        planks.set(1);
        WorkbenchStamp s = genuine(CONSTRUCTOR, 5);
        service.place(p, item(s), block).join();

        assertInstanceOf(UpgradeRules.MissingItems.class, service.upgrade(p, s.id(), shown(s)).join());

        planks.set(64);
        assertInstanceOf(UpgradeRules.Ok.class, service.upgrade(p, s.id(), shown(s)).join());
        assertEquals(62, planks.get());
        verify(relic, never()).setAmount(anyInt());
        verify(inv, never()).setItem(eq(0), any());
    }

    @Test void breakIsRefusedWhileUpgradeIsInFlight() {
        service = newService(settings(COST));
        WorkbenchStamp s = genuine(INGENIERO, 5);
        service.place(p, item(s), block).join();
        queued = true;

        var upgrade = service.upgrade(p, s.id(), shown(s));
        var refused = service.breakBench(p, block).join();
        assertTrue(refused.dropped().isEmpty());
        // El listener avisa al jugador en vez de callar.
        assertEquals(WorkbenchService.BreakStatus.UPGRADING, refused.status());
        assertInstanceOf(UpgradeRules.Ok.class, pump(upgrade));

        assertEquals(ARQUITECTO, service.byId(s.id()).orElseThrow().tier());
        assertEquals(1, repo.all().join().size());
    }

    @Test void upgradeIsRefusedWhileBreakIsInFlight() {
        service = newService(settings(COST));
        WorkbenchStamp s = genuine(INGENIERO, 5);
        service.place(p, item(s), block).join();
        queued = true;

        var broken = service.breakBench(p, block);
        assertInstanceOf(UpgradeRules.MaxTier.class, service.upgrade(p, s.id(), shown(s)).join());
        assertTrue(pump(broken).dropped().isPresent());

        assertEquals(64, planks.get());
        verify(display, never()).refresh(any());
    }

    @Test void benchStaysProtectedWhileBreaking() {
        WorkbenchStamp s = genuine(CONSTRUCTOR, 1);
        service.place(p, item(s), block).join();
        queued = true;

        var broken = service.breakBench(p, block);
        assertTrue(service.at(block).isPresent());
        assertTrue(service.breakBench(p, block).join().dropped().isEmpty());
        assertTrue(pump(broken).dropped().isPresent());
        assertTrue(service.at(block).isEmpty());
    }

    @Test void nonBlockFallbackResolvesToCraftingTable() {
        assertEquals(Material.CRAFTING_TABLE, WorkbenchSettings.TierSettings.resolve(Material.DIAMOND, false));
        assertEquals(Material.BEACON, WorkbenchSettings.TierSettings.resolve(Material.BEACON, true));
    }

    @Test void cancelledPlacementIsRolledBack() {
        WorkbenchStamp s = genuine(CONSTRUCTOR, 1);
        service.place(p, item(s), block).join();
        service.cancelPlacement(s.id()).join();
        assertTrue(repo.all().join().isEmpty());
        assertTrue(service.at(block).isEmpty());
    }

    @Test void breakWithTheRowAlreadyGoneTreatsTheBenchAsNonexistent() {
        WorkbenchStamp s = genuine(CONSTRUCTOR, 1);
        service.place(p, item(s), block).join();
        repo.remove(s.id()).join(); // otro proceso borró la fila

        var gone = service.breakBench(p, block).join();
        // Sin ítem (no hay duplicado) y fuera de la caché: el listener quita bloque, display y entrada del chunk.
        assertEquals(WorkbenchService.BreakStatus.GONE, gone.status());
        assertTrue(gone.dropped().isEmpty());
        assertTrue(service.at(block).isEmpty());
        assertTrue(service.byId(s.id()).isEmpty());
        assertTrue(service.inChunk("world", 0, 0).isEmpty());
        // Un segundo clic ya no la encuentra: un solo aviso.
        assertEquals(WorkbenchService.BreakStatus.NOTHING, service.breakBench(p, block).join().status());
    }

    @Test void anOrphanedBenchCanBePlacedAgainWithItsItem() {
        WorkbenchStamp s = genuine(INGENIERO, 7);
        ItemStack it = item(s);
        service.place(p, it, block).join();
        // Como tras un cancelPlacement fallido (o un reinicio): fila y caché huérfanas, el ítem vuelve al jugador.
        service.markOrphan(service.byId(s.id()).orElseThrow());

        Block other = blockIn("world", 40, 70, 40);
        assertEquals(PlaceResult.OK, service.validate(p, it, other));
        assertEquals(PlaceResult.OK, service.place(p, it, other).join());

        WorkbenchRecord now = repo.find(s.id()).join().orElseThrow();
        assertFalse(now.orphan());
        assertEquals(40, now.x());
        assertEquals(1, repo.all().join().size());
        assertTrue(service.at(other).isPresent());
        assertFalse(service.byId(s.id()).orElseThrow().orphan());
        // Una mesa viva con ese id sigue ocupando: no se coloca dos veces.
        assertEquals(PlaceResult.OCCUPIED, service.place(p, it, blockIn("world", 50, 70, 50)).join());
    }

    @Test void failedCancelPlacementLeavesTheCacheCoherent() {
        WorkbenchStamp s = genuine(CONSTRUCTOR, 1);
        service.place(p, item(s), block).join();
        db.close(); // la base falla al deshacer

        service.cancelPlacement(s.id()).join();

        // La fila sigue en la base: en la caché queda como huérfana, sin posición, y no como mesa viva ni borrada.
        assertTrue(service.byId(s.id()).orElseThrow().orphan());
        assertTrue(service.at(block).isEmpty());
        assertTrue(service.inChunk("world", 0, 0).isEmpty());
    }

    static final String SAME_MATERIAL_TWICE = """
            tiers:
              ingeniero:
                upgrade-cost:
                  - {item: "oak_planks", amount: 16}
                  - {item: "minecraft:oak_planks", amount: 8}
            """;

    @Test void twoCostsOfTheSameMaterialAreCountedTogether() {
        service = newService(settings(SAME_MATERIAL_TWICE));
        planks.set(20);
        WorkbenchStamp s = genuine(CONSTRUCTOR, 5);
        service.place(p, item(s), block).join();

        // 20 tablones no pagan 16 + 8: antes cada coste veía los 20 y se cobraban 24.
        assertInstanceOf(UpgradeRules.MissingItems.class, service.upgrade(p, s.id(), shown(s)).join());
        assertEquals(20, planks.get());

        planks.set(30);
        assertInstanceOf(UpgradeRules.Ok.class, service.upgrade(p, s.id(), shown(s)).join());
        assertEquals(6, planks.get());
    }

    @Test void failedTrustedReadStillCompletesOnTheMainThread() {
        WorkbenchStamp s = genuine(CONSTRUCTOR, 1);
        service.place(p, item(s), block).join();
        db.close();
        queued = true;

        var f = service.trusted(s.id());
        // Sin pasar por el hilo principal el menú mandaría mensajes desde el hilo de la base.
        assertFalse(f.isDone(), "el fallo no debe terminar fuera del hilo principal");
        assertThrows(java.util.concurrent.CompletionException.class, () -> pump(f));
    }

    @Test void inChunkOnlyReturnsThatChunk() {
        WorkbenchRecord a = new WorkbenchRecord(UUID.randomUUID(), ownerId, "world", -1, 64, -17, CONSTRUCTOR, 1, false, 1);
        WorkbenchRecord b = new WorkbenchRecord(UUID.randomUUID(), ownerId, "world", 15, 64, 0, CONSTRUCTOR, 2, false, 1);
        WorkbenchRecord c = new WorkbenchRecord(UUID.randomUUID(), ownerId, "other", -1, 64, -17, CONSTRUCTOR, 3, false, 1);
        repo.insert(a).join();
        repo.insert(b).join();
        repo.insert(c).join();
        service.load();

        assertEquals(java.util.List.of(a), service.inChunk("world", -1, -2));
        assertEquals(java.util.List.of(b), service.inChunk("world", 0, 0));
        service.markOrphan(a);
        assertTrue(service.inChunk("world", -1, -2).isEmpty());
    }

    @Test void trustingTwiceSaysItWasAlreadyThere() {
        WorkbenchStamp s = genuine(CONSTRUCTOR, 1);
        service.place(p, item(s), block).join();
        UUID friend = UUID.randomUUID();

        assertEquals(WorkbenchService.AccessChange.DONE, service.trust(p, s.id(), friend).join());
        assertEquals(WorkbenchService.AccessChange.UNCHANGED, service.trust(p, s.id(), friend).join());
        assertEquals(WorkbenchService.AccessChange.DONE, service.untrust(p, s.id(), friend).join());
        assertEquals(WorkbenchService.AccessChange.UNCHANGED, service.untrust(p, s.id(), friend).join());

        Player other = mock(Player.class);
        when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        assertEquals(WorkbenchService.AccessChange.DENIED, service.trust(other, s.id(), friend).join());
    }

    @Test void breakDenialSaysTheRealReason() {
        WorkbenchStamp s = genuine(CONSTRUCTOR, 1);
        service.place(p, item(s), block).join();
        WorkbenchRecord rec = service.at(block).orElseThrow();

        Player owner = mock(Player.class);
        when(owner.getUniqueId()).thenReturn(ownerId);
        assertEquals("workbench.break.no-permission", service.breakDeniedKey(owner, rec));

        Player stranger = mock(Player.class);
        when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());
        when(stranger.hasPermission(WorkbenchService.PERM_BREAK)).thenReturn(true);
        assertEquals("workbench.break.denied", service.breakDeniedKey(stranger, rec));

        assertEquals("workbench.break.has-project", service.breakDeniedKey(p, rec));
    }

    // ---- romper con obra activa y volver a colocar (plan 3, Task 4) ----

    static final String KEY = "minecraft:stone";
    ProjectRepository projectRepo;
    ProjectService projectService;
    ConstructionEngine engine;
    final ReturnService returns = mock(ReturnService.class);
    AreaGuard guard = AreaGuard.allowAll();
    /** Jugadores conectados (por UUID) y lo que se les dice: {@code uuid:clave}. */
    final Map<UUID, Player> online = new HashMap<>();
    final List<String> told = new ArrayList<>();
    StructureDefinition def;
    int nextX = 100;

    /** Servicio con obras reales (ProjectService y motor) y la devolución simulada. */
    void withProjects(StructuresSettings.BreakPolicy breakPolicy) {
        projectRepo = new ProjectRepository(db, Dialect.SQLITE);
        projectService = new ProjectService(projectRepo, Runnable::run, Logger.getAnonymousLogger());
        List<SourceBlock> blocks = List.of(new SourceBlock(0, 0, 0, mock(BlockData.class), null));
        BlockSource src = new BlockSource() {
            public int width() { return 1; }
            public int height() { return 1; }
            public int length() { return 1; }
            public List<SourceBlock> blocks() { return blocks; }
        };
        def = new StructureDefinition("casa", Map.of(), "casas", null, null, null, src,
                List.of(new MaterialSpec(List.of(KEY), 1, false)), 0, true, BuildOrder.BOTTOM_UP, List.of(), 0, 0,
                new PlacementRules(false, false, List.of(), List.of(), Set.of()), "rpgrollstructures.build.casa");
        engine = new ConstructionEngine(projectService, projectRepo, id -> Optional.of(def),
                () -> new PerformanceSettings(256, 10, 5, 2, 4096, 64, 300, 75, 60), mock(BlockWriter.class),
                Runnable::run, (x, b) -> { }, x -> { }, () -> 0L, Runnable::run, id -> true,
                (owner, w, box) -> guard.check(owner, w, box), (d, owner, w, box) -> Optional.empty(),
                Logger.getAnonymousLogger());
        projectsView = new BenchProjects(projectService, engine, returns, () -> 75, online::get,
                (pl, key) -> told.add(pl.getUniqueId() + ":" + key));
        policy = breakPolicy;
        online.put(ownerId, p);
        service = newService(settings(""));
    }

    Project project(UUID bench, UUID owner, ProjectState state) {
        World w = mock(World.class);
        when(w.getName()).thenReturn("world");
        Project pr = projectService.create(bench, def, owner, new Location(w, nextX, 64, 200), Rotation.NONE).join();
        nextX += 100;
        pr.setState(state);
        return pr;
    }

    Block blockAt(int x, int y, int z) {
        Block b = mock(Block.class);
        when(b.getWorld()).thenReturn(world);
        when(b.getX()).thenReturn(x);
        when(b.getY()).thenReturn(y);
        when(b.getZ()).thenReturn(z);
        return b;
    }

    ProjectRecord dbRow(Project pr) {
        return projectRepo.active().join().stream().filter(r -> r.id().equals(pr.id())).findFirst().orElseThrow();
    }

    @Test void breakWithPauseKeepsRowAsOrphanAndPausesProject() {
        withProjects(StructuresSettings.BreakPolicy.PAUSE);
        WorkbenchStamp s = genuine(CONSTRUCTOR, 7);
        assertEquals(PlaceResult.OK, service.place(p, item(s), block).join());
        Project building = project(s.id(), ownerId, ProjectState.BUILDING);
        // Otra mesa con la obra esperando materiales: no cambia de estado.
        WorkbenchStamp s2 = genuine(CONSTRUCTOR, 8);
        Block block2 = blockAt(20, 64, 20);
        assertEquals(PlaceResult.OK, service.place(p, item(s2), block2).join());
        Project waiting = project(s2.id(), ownerId, ProjectState.WAITING_MATERIALS);

        assertEquals(BreakDecision.ALLOW_PAUSE, service.breakDecision(p, service.at(block).orElseThrow()));
        WorkbenchService.BreakResult r = service.breakBench(p, block).join();
        assertEquals(WorkbenchService.BreakStatus.PAUSED, r.status());
        assertEquals(Optional.of(s), items.read(r.dropped().orElseThrow()), "mismo sello, mismo UUID");
        assertTrue(repo.find(s.id()).join().orElseThrow().orphan(), "la fila se conserva como huérfana");
        assertTrue(service.byId(s.id()).orElseThrow().orphan());
        assertTrue(service.at(block).isEmpty());
        assertEquals(ProjectState.PAUSED, building.state());
        assertEquals(ConstructionEngine.PAUSED_BENCH, building.error());
        assertEquals(ProjectState.PAUSED, dbRow(building).state());
        assertEquals(ConstructionEngine.PAUSED_BENCH, dbRow(building).error());
        assertEquals(Optional.of(building), projectService.forBench(s.id()), "sigue atada a su mesa");

        WorkbenchService.BreakResult r2 = service.breakBench(p, block2).join();
        assertEquals(WorkbenchService.BreakStatus.PAUSED, r2.status());
        assertEquals(ProjectState.WAITING_MATERIALS, waiting.state());
        assertNull(waiting.error());
        assertEquals(Optional.of(waiting), projectService.forBench(s2.id()));
    }

    @Test void replacingTheItemReconnectsAndResumes() {
        withProjects(StructuresSettings.BreakPolicy.PAUSE);
        WorkbenchStamp s = genuine(INGENIERO, 7);
        service.place(p, item(s), block).join();
        Project building = project(s.id(), ownerId, ProjectState.BUILDING);
        ItemStack back = service.breakBench(p, block).join().dropped().orElseThrow();

        Block other = blockAt(50, 70, 50);
        assertEquals(PlaceResult.OK, service.place(p, back, other).join());
        WorkbenchRecord row = repo.find(s.id()).join().orElseThrow();
        assertFalse(row.orphan());
        assertEquals(50, row.x());
        assertEquals(INGENIERO, row.tier());
        assertEquals(s.id(), service.at(other).orElseThrow().id());
        assertEquals(ProjectState.BUILDING, building.state());
        assertNull(building.error());
        assertEquals(100, building.x(), "la obra sigue en su sitio original");
        assertTrue(told.contains(ownerId + ":build.resumed"), told.toString());
    }

    /** Review Focus 1: quien vuelve a colocar la mesa (con bypass) no presta sus derechos; manda el dueño de la obra. */
    @Test void reconnectByTrustedPlayerChecksTheProjectOwner() {
        guard = (owner, w, box) -> owner.equals(ownerId)
                ? Optional.of(new AreaGuard.Denial(Hook.WORLDGUARD, "protect.worldguard")) : Optional.empty();
        withProjects(StructuresSettings.BreakPolicy.PAUSE);
        WorkbenchStamp s = genuine(CONSTRUCTOR, 7);
        service.place(p, item(s), block).join();
        Project building = project(s.id(), ownerId, ProjectState.BUILDING);
        ItemStack back = service.breakBench(p, block).join().dropped().orElseThrow();
        online.remove(ownerId); // el dueño se desconecta

        Player friend = mock(Player.class);
        UUID friendId = UUID.randomUUID();
        when(friend.getUniqueId()).thenReturn(friendId);
        when(friend.hasPermission(anyString())).thenReturn(true); // también rpgrollstructures.bypass
        assertEquals(PlaceResult.OK, service.place(friend, back, blockAt(50, 70, 50)).join());
        assertEquals(ProjectState.PAUSED, building.state());
        assertEquals(ConstructionEngine.PAUSED_PROTECTED, building.error());
        assertTrue(told.contains(friendId + ":" + ConstructionEngine.PAUSED_PROTECTED), told.toString());
    }

    @Test void breakWithCancelRefundsOwnerThenBreaks() {
        withProjects(StructuresSettings.BreakPolicy.CANCEL);
        WorkbenchStamp s = genuine(CONSTRUCTOR, 7);
        service.place(p, item(s), block).join();
        // La obra es de otro (de confianza): la devolución va a su dueño, lo hace ReturnService.cancel.
        Project pr = project(s.id(), UUID.randomUUID(), ProjectState.READY);
        java.util.concurrent.CompletableFuture<Boolean> cancelled = new java.util.concurrent.CompletableFuture<>();
        when(returns.cancel(any(), any(), anyInt())).thenReturn(cancelled);

        assertEquals(BreakDecision.ALLOW_CANCEL, service.breakDecision(p, service.at(block).orElseThrow()));
        java.util.concurrent.CompletableFuture<WorkbenchService.BreakResult> f = service.breakBench(p, block);
        verify(returns).cancel(p, pr, 75);
        assertFalse(f.isDone(), "la mesa espera a que la base confirme la cancelación");
        assertTrue(repo.find(s.id()).join().isPresent());
        assertTrue(service.at(block).isPresent());

        cancelled.complete(true);
        WorkbenchService.BreakResult r = f.join();
        assertEquals(WorkbenchService.BreakStatus.CANCELLED, r.status());
        assertEquals(Optional.of(s), items.read(r.dropped().orElseThrow()));
        assertTrue(repo.find(s.id()).join().isEmpty());
        assertTrue(service.at(block).isEmpty());
    }

    @Test void cancelFailureKeepsTheBench() {
        withProjects(StructuresSettings.BreakPolicy.CANCEL);
        WorkbenchStamp s = genuine(CONSTRUCTOR, 7);
        service.place(p, item(s), block).join();
        project(s.id(), ownerId, ProjectState.BUILDING);
        when(returns.cancel(any(), any(), anyInt()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(false));

        WorkbenchService.BreakResult r = service.breakBench(p, block).join();
        assertEquals(WorkbenchService.BreakStatus.CANCEL_FAILED, r.status());
        assertTrue(r.dropped().isEmpty());
        assertFalse(repo.find(s.id()).join().orElseThrow().orphan());
        assertTrue(service.at(block).isPresent());
        // El candado se suelta: se puede reintentar.
        assertEquals(WorkbenchService.BreakStatus.CANCEL_FAILED, service.breakBench(p, block).join().status());
        verify(returns, times(2)).cancel(any(), any(), anyInt());
    }

    /** Arreglo de la memoria: una huérfana (p. ej. quitada con WorldEdit) no bloquea su bloque, ni tras reiniciar. */
    @Test void orphanDoesNotOccupyItsOldPosition() {
        WorkbenchStamp gone = genuine(CONSTRUCTOR, 1);
        service.place(p, item(gone), block).join();
        service.markOrphan(service.at(block).orElseThrow());

        WorkbenchStamp fresh = genuine(CONSTRUCTOR, 2);
        assertEquals(PlaceResult.OK, service.place(p, item(fresh), block).join());
        assertEquals(fresh.id(), service.at(block).orElseThrow().id());
        assertEquals(2, repo.all().join().size());

        WorkbenchService again = newService(settings(""));
        again.load();
        assertEquals(fresh.id(), again.at(block).orElseThrow().id());
        assertTrue(again.byId(gone.id()).orElseThrow().orphan());

        // Tras reiniciar, una huérfana tampoco impide poner una mesa nueva en su bloque.
        Block third = blockAt(30, 64, 30);
        WorkbenchStamp later = genuine(CONSTRUCTOR, 3);
        again.place(p, item(later), third).join();
        again.markOrphan(again.at(third).orElseThrow());
        repo.all().join(); // espera a la base
        WorkbenchService reloaded = newService(settings(""));
        reloaded.load();
        assertTrue(reloaded.at(third).isEmpty());
        assertEquals(PlaceResult.OK, reloaded.place(p, item(genuine(INGENIERO, 4)), third).join());
        // La huérfana sigue pudiendo volver con su ítem, en otro sitio.
        assertEquals(PlaceResult.OK, reloaded.place(p, item(later), blockAt(31, 64, 30)).join());
    }
}
