package com.sack.rpgroll.worldforge.regen;

import com.sack.rpgroll.worldforge.regen.RegionFiles.ChunkPos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegenUnitTest {

    @TempDir
    Path dim;

    /** A region file whose header says the given chunks exist (sector 2+i, 1 sector each), plus fake data. */
    private Path region(String kind, int rx, int rz, ChunkPos... chunks) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(8192 + 4096);
        for (ChunkPos c : chunks) {
            int i = RegionFiles.index(c.x(), c.z());
            b.putInt(i * 4, ((2 + i) << 8) | 1);
            b.putInt(4096 + i * 4, 1_700_000_000);
        }
        b.put(8192, (byte) 0x7A); // stands for chunk data that must not be touched
        Path f = dim.resolve(kind).resolve("r." + rx + "." + rz + ".mca");
        Files.createDirectories(f.getParent());
        Files.write(f, b.array());
        return f;
    }

    @Test
    void lists_generated_chunks_with_their_world_coordinates() throws IOException {
        region("region", -1, 0, new ChunkPos(-1, 0), new ChunkPos(-32, 31));
        region("region", 0, 0, new ChunkPos(5, 7));
        Files.writeString(dim.resolve("region/notas.txt"), "no soy una región");
        assertEquals(Set.of(new ChunkPos(-1, 0), new ChunkPos(-32, 31), new ChunkPos(5, 7)), Set.copyOf(RegionFiles.generated(dim)));
        assertTrue(RegionFiles.exists(dim, new ChunkPos(5, 7)));
        assertFalse(RegionFiles.exists(dim, new ChunkPos(6, 7)));
        assertFalse(RegionFiles.exists(dim, new ChunkPos(100, 100)), "sin archivo = no generado");
    }

    @Test
    void drop_forgets_only_that_chunk_in_every_kind() throws IOException {
        ChunkPos target = new ChunkPos(-1, 0), neighbour = new ChunkPos(-2, 0);
        Path r = region("region", -1, 0, target, neighbour);
        Path e = region("entities", -1, 0, target);
        region("poi", -1, 0, neighbour);
        Path mcc = r.resolveSibling("c.-1.0.mcc");
        Files.writeString(mcc, "chunk enorme");
        byte[] dataBefore = java.util.Arrays.copyOfRange(Files.readAllBytes(r), 8192, 12288);

        assertTrue(RegionFiles.drop(dim, target));
        assertEquals(List.of(neighbour), RegionFiles.generated(r, -1, 0));
        assertEquals(List.of(), RegionFiles.generated(e, -1, 0));
        assertFalse(Files.exists(mcc));
        int i = RegionFiles.index(-1, 0);
        assertEquals(0, ByteBuffer.wrap(Files.readAllBytes(r)).getInt(4096 + i * 4), "también la fecha");
        assertArrayEquals(dataBefore, java.util.Arrays.copyOfRange(Files.readAllBytes(r), 8192, 12288), "los sectores no se tocan");

        assertFalse(RegionFiles.drop(dim, target), "dos veces no rompe nada");
        assertFalse(RegionFiles.drop(dim, new ChunkPos(900, 900)), "sin archivos no hace nada");
    }

    @Test
    void job_states_only_move_forward() {
        assertTrue(JobState.PLANNED.canMoveTo(JobState.CONFIRMED));
        assertFalse(JobState.PLANNED.canMoveTo(JobState.RUNNING), "sin confirmar ni respaldo no se ejecuta");
        assertFalse(JobState.CONFIRMED.canMoveTo(JobState.RUNNING), "sin respaldo no se ejecuta");
        assertFalse(JobState.PENDING_RESTART.canMoveTo(JobState.RUNNING), "al arrancar también se respalda antes");
        assertFalse(JobState.BACKING_UP.canMoveTo(JobState.RUNNING), "solo con el respaldo verificado (READY)");
        assertFalse(JobState.INTERRUPTED.canMoveTo(JobState.RUNNING), "un trabajo interrumpido no se reanuda a ciegas");
        assertFalse(JobState.COMPLETED.canMoveTo(JobState.RUNNING));
        for (JobState s : JobState.values()) {
            assertFalse(s.canMoveTo(s), s + " no se repite");
            assertEquals(s.finished(), s.next().isEmpty());
        }
        assertFalse(JobState.PLANNED.locksWorld());
        assertTrue(JobState.NEEDS_RECOVERY.locksWorld());
        assertFalse(JobState.CANCELLED.locksWorld());
    }
}
