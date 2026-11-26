package com.sack.rpgroll.structures.project;

import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.build.BlockWriter;
import com.sack.rpgroll.structures.build.PlacementPlan;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RepairScansTest {

    final AtomicInteger reads = new AtomicInteger();
    final BlockWriter world = new BlockWriter() {
        @Override public boolean chunkLoaded(String w, int x, int z) { return true; }
        @Override public BlockInfo read(String w, int x, int y, int z) {
            reads.incrementAndGet();
            return new BlockInfo(Material.AIR, false, null);
        }
        @Override public void write(String w, int x, int y, int z, SourceBlock b) { fail("no escribe"); }
    };

    PlacementPlan plan(int n) {
        BlockData stone = mock(BlockData.class);
        return new PlacementPlan(IntStream.range(0, n).mapToObj(i -> new SourceBlock(i, 0, 0, stone, null)).toList());
    }

    @Test void readIsSplitAcrossTicks() {
        RepairScans scans = new RepairScans(world);
        CompletableFuture<RepairMath.Result> f = scans.start(UUID.randomUUID(), plan(10_000), Set.of(), "world", 0, 64,
                0).orElseThrow();
        assertEquals(0, reads.get(), "nada en el clic");
        scans.tick();
        assertEquals(RepairScans.BLOCKS_PER_TICK, reads.get());
        assertFalse(f.isDone());
        scans.tick();
        scans.tick();
        assertEquals(new RepairMath.Result(10_000, 10_000), f.join());
        assertEquals(10_000, reads.get());
    }

    @Test void onlyOneReadInFlightPerPlayer() {
        RepairScans scans = new RepairScans(world);
        UUID player = UUID.randomUUID();
        CompletableFuture<RepairMath.Result> f = scans.start(player, plan(5_000), Set.of(), "world", 0, 64, 0)
                .orElseThrow();
        assertEquals(Optional.empty(), scans.start(player, plan(5_000), Set.of(), "world", 0, 64, 0),
                "otro clic mientras se lee: no empieza otra");
        assertTrue(scans.start(UUID.randomUUID(), plan(10), Set.of(), "world", 0, 64, 0).isPresent(), "otro jugador sí");
        scans.tick();
        scans.tick();
        assertTrue(f.isDone());
        assertTrue(scans.start(player, plan(10), Set.of(), "world", 0, 64, 0).isPresent(), "al acabar, otra vez");
    }

    @Test void unloadedChunkEndsTheReadWithItsReason() {
        BlockWriter away = new BlockWriter() {
            @Override public boolean chunkLoaded(String w, int x, int z) { return false; }
            @Override public BlockInfo read(String w, int x, int y, int z) { throw new AssertionError("no lee"); }
            @Override public void write(String w, int x, int y, int z, SourceBlock b) { }
        };
        RepairScans scans = new RepairScans(away);
        UUID player = UUID.randomUUID();
        CompletableFuture<RepairMath.Result> f = scans.start(player, plan(10), Set.of(), "world", 0, 64, 0)
                .orElseThrow();
        scans.tick();
        Throwable e = assertThrows(CompletionException.class, f::join).getCause();
        assertEquals("repair.not-loaded", ((ProjectService.RepairException) e).key());
        assertTrue(scans.start(player, plan(10), Set.of(), "world", 0, 64, 0).isPresent());
    }
}
