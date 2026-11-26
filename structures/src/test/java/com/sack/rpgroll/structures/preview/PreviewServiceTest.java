package com.sack.rpgroll.structures.preview;

import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.storage.Box;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PreviewServiceTest {

    @Test
    void initialOriginIsInFrontOfBench() {
        // Mesa en (100, 64, 200); caja de 5 (ancho) x 3 (largo); el lado cercano queda a 2 bloques de la mesa.
        assertArrayEquals(new int[] { 98, 64, 196 }, PreviewService.initialOrigin(100, 64, 200, BlockFace.NORTH, 5, 3));
        assertArrayEquals(new int[] { 98, 64, 202 }, PreviewService.initialOrigin(100, 64, 200, BlockFace.SOUTH, 5, 3));
        assertArrayEquals(new int[] { 102, 64, 199 }, PreviewService.initialOrigin(100, 64, 200, BlockFace.EAST, 5, 3));
        assertArrayEquals(new int[] { 94, 64, 199 }, PreviewService.initialOrigin(100, 64, 200, BlockFace.WEST, 5, 3));
    }

    @Test
    void blockChangesCapAndRestore() {
        List<SourceBlock> blocks = new ArrayList<>();
        for (int y = 0; y <= 4; y++) { // ya ordenados de abajo arriba, como los da la sesión
            blocks.add(new SourceBlock(0, y, 0, null, null));
        }
        PreviewService.Diff first = PreviewService.diff(blocks, 10, 64, 10, 3, Set.of());
        assertEquals(3, first.send().size());
        assertTrue(first.restore().isEmpty());
        // los más bajos primero
        assertEquals(List.of(64, 65, 66), first.send().keySet().stream().map(PreviewService.Pos::y).toList());

        // Sube 1: la posición y=64 ya no se envía y hay que reponerla; 65 y 66 siguen.
        Set<PreviewService.Pos> sent = new LinkedHashSet<>(first.send().keySet());
        PreviewService.Diff second = PreviewService.diff(blocks, 10, 65, 10, 3, sent);
        assertEquals(Set.of(new PreviewService.Pos(10, 64, 10)), second.restore());
        assertEquals(3, second.send().size());
    }

    @Test
    void refreshCompletesOnlyAfterTheCacheHoldsTheNewBoxes() {
        ProjectRepository repo = mock(ProjectRepository.class);
        CompletableFuture<List<Box>> read = new CompletableFuture<>();
        when(repo.boxesIn("w")).thenReturn(read);
        PreviewService svc = new PreviewService(null, null, repo, com.sack.rpgroll.structures.protect.AreaGuard.allowAll(), id -> java.util.Optional.empty(),
                com.sack.rpgroll.structures.integration.BedrockCheck.none(), java.util.logging.Logger.getAnonymousLogger());
        CompletableFuture<Void> done = svc.refresh("w");
        assertFalse(done.isDone());
        read.complete(List.of(new Box(0, 0, 0, 1, 1, 1), new Box(5, 5, 5, 6, 6, 6)));
        assertTrue(done.isDone());
        assertEquals(2, svc.boxesFor("w").size());

        when(repo.boxesIn("w")).thenReturn(CompletableFuture.failedFuture(new RuntimeException("bd")));
        assertTrue(svc.refresh("w").isCompletedExceptionally(), "el fallo llega a quien confirma");
        assertEquals(2, svc.boxesFor("w").size(), "sin borrar la lista buena");
    }

    @Test
    void invalidateKeepsPreviousBoxesUntilRefresh() {
        ProjectRepository repo = mock(ProjectRepository.class);
        when(repo.boxesIn("w")).thenReturn(CompletableFuture.completedFuture(List.of(new Box(0, 0, 0, 1, 1, 1))));
        PreviewService svc = new PreviewService(null, null, repo, com.sack.rpgroll.structures.protect.AreaGuard.allowAll(), id -> java.util.Optional.empty(),
                com.sack.rpgroll.structures.integration.BedrockCheck.none(), java.util.logging.Logger.getAnonymousLogger());
        svc.refreshBoxes("w");
        assertEquals(1, svc.boxesFor("w").size());

        CompletableFuture<List<Box>> pending = new CompletableFuture<>();
        when(repo.boxesIn("w")).thenReturn(pending);
        svc.invalidate("w");
        assertEquals(1, svc.boxesFor("w").size(), "sigue la lista anterior mientras se lee");
        pending.complete(List.of());
        assertTrue(svc.boxesFor("w").isEmpty());
    }

    /** Vista de {@code n} bloques en fila sobre un mundo de aire que cuenta las lecturas. */
    static final class Fixture {
        final java.util.concurrent.atomic.AtomicInteger reads = new java.util.concurrent.atomic.AtomicInteger();
        final org.bukkit.entity.Player player = mock(org.bukkit.entity.Player.class);
        final PreviewService svc;

        final com.sack.rpgroll.structures.config.PerformanceSettings perf;
        final com.sack.rpgroll.common.lang.LangManager lang = mock(com.sack.rpgroll.common.lang.LangManager.class);

        Fixture(int n, int maxBlocks) {
            this(n, maxBlocks, false);
        }

        Fixture(int n, int maxBlocks, boolean bedrock) {
            this(n, maxBlocks, bedrock, com.sack.rpgroll.structures.protect.AreaGuard.allowAll());
        }

        Fixture(int n, int maxBlocks, boolean bedrock, com.sack.rpgroll.structures.protect.AreaGuard guard) {
            org.bukkit.World w = mock(org.bukkit.World.class);
            when(w.getName()).thenReturn("w");
            when(w.getMinHeight()).thenReturn(-64);
            when(w.getMaxHeight()).thenReturn(320);
            org.bukkit.block.Block air = mock(org.bukkit.block.Block.class);
            when(air.getType()).thenReturn(org.bukkit.Material.AIR);
            when(air.isReplaceable()).thenReturn(true);
            when(w.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(inv -> {
                reads.incrementAndGet();
                return air;
            });
            java.util.UUID id = java.util.UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(id);
            when(player.getWorld()).thenReturn(w);
            when(player.getLocation()).thenReturn(new org.bukkit.Location(w, 0, 64, 0, 0, 0));
            ProjectRepository repo = mock(ProjectRepository.class);
            when(repo.boxesIn("w")).thenReturn(CompletableFuture.completedFuture(List.of()));
            perf = new com.sack.rpgroll.structures.config.PerformanceSettings(256, 10, 5, 2, maxBlocks, 64, 300,
                    100, 60);
            var bench = new com.sack.rpgroll.structures.storage.WorkbenchRecord(java.util.UUID.randomUUID(), id, "w",
                    0, 64, 0, null, 1, false, 0);
            svc = new PreviewService(() -> perf, lang, repo, guard, b -> java.util.Optional.of(bench),
                    uid -> bedrock,
                    java.util.logging.Logger.getAnonymousLogger());
            List<SourceBlock> blocks = new ArrayList<>();
            org.bukkit.block.data.BlockData data = mock(org.bukkit.block.data.BlockData.class);
            when(data.clone()).thenReturn(data); // girar clona
            for (int i = 0; i < n; i++) {
                blocks.add(new SourceBlock(i % 100, i / 100, 0, data, null));
            }
            com.sack.rpgroll.common.structure.BlockSource src = new com.sack.rpgroll.common.structure.BlockSource() {
                public int width() { return 100; }
                public int height() { return Math.max(1, (n + 99) / 100); }
                public int length() { return 1; }
                public List<SourceBlock> blocks() { return blocks; }
            };
            var rules = new com.sack.rpgroll.structures.catalog.PlacementRules(false, false, List.of(), List.of(),
                    Set.of());
            var def = new com.sack.rpgroll.structures.catalog.StructureDefinition("casa", java.util.Map.of(), "casas",
                    null, null, null, src, List.of(), 0, false, null, List.of(), 1, 1, rules, "p");
            svc.start(player, bench.id(), def);
        }
    }

    @Test
    void checkWhileMovingOnlyLooksAtTheSentBlocks() {
        Fixture f = new Fixture(1000, 50);
        assertTrue(f.svc.check(f.player).ok());
        assertTrue(f.reads.get() <= 50, "lecturas: " + f.reads.get());
    }

    @Test
    void fullCheckOnConfirmIsSplitAcrossTicks() {
        Fixture f = new Fixture(10_000, 50);
        CompletableFuture<PreviewService.Checked> full = f.svc.fullCheck(f.player);
        assertFalse(full.isDone(), "no se valida todo en el clic");
        f.svc.scanTick();
        assertFalse(full.isDone());
        assertTrue(f.reads.get() <= PreviewService.SCAN_BLOCKS_PER_TICK);
        f.svc.scanTick();
        f.svc.scanTick();
        assertTrue(full.isDone());
        assertTrue(full.join().result().ok());
        assertEquals(10_000, f.reads.get(), "la validación completa mira todos los bloques");
    }

    @Test
    void stoppingThePreviewEndsItsFullCheck() {
        Fixture f = new Fixture(10_000, 50);
        CompletableFuture<PreviewService.Checked> full = f.svc.fullCheck(f.player);
        f.svc.stop(f.player);
        assertTrue(full.isDone());
        assertFalse(full.join().result().ok());
    }

    @Test
    void movingDuringTheFullCheckDoesNotLetTheOldResultCreateAtTheNewSpot() {
        Fixture f = new Fixture(10_000, 50);
        CompletableFuture<PreviewService.Checked> full = f.svc.fullCheck(f.player);
        org.bukkit.Location checkedAt = f.svc.session(f.player).orElseThrow().minCorner();
        f.svc.scanTick();
        f.svc.move(f.player, 5, 0, 0); // la vista se mueve con la validación en curso
        for (int i = 0; i < 3; i++) {
            f.svc.scanTick();
        }
        PreviewService.Checked c = full.join();
        assertFalse(c.result().ok(), "el resultado viejo no vale para el sitio nuevo");
        assertEquals("preview.moved", c.result().reason());
        assertFalse(f.svc.unchanged(f.player, c));

        // Segundo clic: validación nueva, del sitio nuevo.
        CompletableFuture<PreviewService.Checked> again = f.svc.fullCheck(f.player);
        assertNotSame(full, again);
        for (int i = 0; i < 3; i++) {
            f.svc.scanTick();
        }
        PreviewService.Checked ok = again.join();
        assertTrue(ok.result().ok());
        assertEquals(checkedAt.getBlockX() + 5, ok.origin().getBlockX(), "la obra se crea donde se validó");
        assertTrue(f.svc.unchanged(f.player, ok));
    }

    @Test
    void movingAfterTheCheckFinishedInvalidatesIt() {
        Fixture f = new Fixture(100, 50);
        CompletableFuture<PreviewService.Checked> full = f.svc.fullCheck(f.player);
        f.svc.scanTick();
        PreviewService.Checked c = full.join();
        assertTrue(c.result().ok());
        assertTrue(f.svc.unchanged(f.player, c));
        f.svc.rotate(f.player); // entre el fin de la validación y la vuelta al menú
        assertFalse(f.svc.unchanged(f.player, c), "createChecked rechaza: hay que confirmar otra vez");
        assertNotSame(full, f.svc.fullCheck(f.player), "ni se reutiliza el resultado viejo");
    }

    @Test
    void bedrockPlayerGetsOutlineOnly() {
        Fixture f = new Fixture(100, 50, true);
        PreviewSession s = f.svc.session(f.player).orElseThrow();
        f.svc.sendBlocks(f.player, s, f.perf);
        f.svc.sendBlocks(f.player, s, f.perf);
        f.svc.particles(f.player, s);
        verify(f.player, never()).sendBlockChange(any(org.bukkit.Location.class),
                any(org.bukkit.block.data.BlockData.class));
        verify(f.player, never()).sendMultiBlockChange(anyMap());
        verify(f.player, atLeastOnce()).spawnParticle(any(org.bukkit.Particle.class), any(org.bukkit.Location.class),
                anyInt(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), any());
        verify(f.lang, times(1)).send(f.player, "preview.bedrock");
    }

    @Test
    void javaPlayerStillGetsBlocks() {
        Fixture f = new Fixture(100, 50, false);
        f.svc.sendBlocks(f.player, f.svc.session(f.player).orElseThrow(), f.perf);
        verify(f.player).sendMultiBlockChange(anyMap());
        verify(f.lang, never()).send(eq(f.player), eq("preview.bedrock"));
    }

    /** Guard que apunta con qué se le llamó y niega con WorldGuard. */
    static final class Recording implements com.sack.rpgroll.structures.protect.AreaGuard {
        final List<Object[]> calls = new ArrayList<>();

        @Override
        public java.util.Optional<Denial> check(java.util.UUID owner, String world, Box box) {
            calls.add(new Object[] { owner, world, box });
            return java.util.Optional.of(new Denial(com.sack.rpgroll.structures.integration.Hook.WORLDGUARD,
                    "protect.worldguard"));
        }
    }

    @Test
    void fullCheckUsesTheProjectOwnerAndWholeBox() {
        Recording guard = new Recording();
        Fixture f = new Fixture(250, 50, false, guard); // 100 x 3 x 1
        f.svc.rotate(f.player); // la caja girada: 1 de ancho y 100 de largo
        PreviewSession s = f.svc.session(f.player).orElseThrow();
        Footprint fp = s.footprint();
        assertEquals(0, fp.maxX() - fp.minX());
        assertEquals(99, fp.maxZ() - fp.minZ());
        CompletableFuture<PreviewService.Checked> full = f.svc.fullCheck(f.player);
        f.svc.scanTick();
        assertEquals("protect.worldguard", full.join().result().reason());
        assertEquals(1, guard.calls.size(), "una sola vez para toda la caja");
        Object[] call = guard.calls.getFirst();
        assertEquals(f.player.getUniqueId(), call[0]);
        assertEquals("w", call[1]);
        assertEquals(new Box(fp.minX(), fp.minY(), fp.minZ(), fp.maxX(), fp.maxY(), fp.maxZ()), call[2]);
        assertEquals(2, fp.maxY() - fp.minY());
    }

    @Test
    void cheapCheckDoesNotAskTheGuard() {
        Recording guard = new Recording();
        Fixture f = new Fixture(100, 50, false, guard);
        assertTrue(f.svc.check(f.player).ok());
        assertTrue(guard.calls.isEmpty(), "la protección se mira al confirmar, no en cada movimiento");
    }

    @Test
    void bypassSkipsTheGuard() {
        Recording guard = new Recording();
        Fixture f = new Fixture(100, 50, false, guard);
        when(f.player.hasPermission("rpgrollstructures.bypass")).thenReturn(true);
        CompletableFuture<PreviewService.Checked> full = f.svc.fullCheck(f.player);
        f.svc.scanTick();
        assertTrue(full.join().result().ok());
        assertTrue(guard.calls.isEmpty());
    }
}
