package com.sack.rpgroll.worldforge.regen;

import com.sack.rpgroll.worldforge.protection.Protections;
import com.sack.rpgroll.worldforge.regen.Jobs.Job;
import com.sack.rpgroll.worldforge.regen.RegionFiles.ChunkPos;
import com.sack.rpgroll.worldforge.storage.Database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupsAndJobsTest {

    @TempDir
    Path tmp;

    private Path write(String rel, String content) throws IOException {
        Path f = tmp.resolve("server").resolve(rel);
        Files.createDirectories(f.getParent());
        Files.writeString(f, content);
        return f;
    }

    @Test
    void backup_verify_and_restore_round_trip() throws IOException {
        Path a = write("world/dimensions/minecraft/t/region/r.0.0.mca", "original A");
        write("world/dimensions/minecraft/t/poi/r.0.0.mca", "original P");
        Backups b = new Backups(tmp.resolve("backups"), tmp.resolve("server"));
        List<String> files = List.of("world/dimensions/minecraft/t/region/r.0.0.mca", "world/dimensions/minecraft/t/poi/r.0.0.mca",
                "world/dimensions/minecraft/t/entities/r.0.0.mca"); // the last one does not exist: skipped
        assertEquals(20, b.bytes(files));

        Backups.Manifest m = b.create("bak-00000001", files);
        assertEquals(2, m.entries().size());
        assertEquals(m.sha256(), b.verify("bak-00000001", m.sha256()).sha256());

        Files.writeString(a, "regenerado");
        b.restore(b.verify("bak-00000001", m.sha256()));
        assertEquals("original A", Files.readString(a));
        assertFalse(Files.exists(a.resolveSibling("r.0.0.mca.wf-restore")));
        assertThrows(IOException.class, () -> b.create("bak-00000001", files), "un respaldo no se sobrescribe");
    }

    @Test
    void a_tampered_backup_is_refused() throws IOException {
        write("w/region/r.0.0.mca", "datos");
        Backups b = new Backups(tmp.resolve("backups"), tmp.resolve("server"));
        Backups.Manifest m = b.create("bak-00000002", List.of("w/region/r.0.0.mca"));
        Files.writeString(tmp.resolve("backups/bak-00000002/files/w/region/r.0.0.mca"), "cambiado");
        assertThrows(IOException.class, () -> b.verify("bak-00000002", m.sha256()));

        Backups.Manifest m2 = b.create("bak-00000003", List.of("w/region/r.0.0.mca"));
        Files.writeString(tmp.resolve("backups/bak-00000003/manifest.txt"), "0".repeat(64) + " 5 ../../fuera.txt\n");
        assertThrows(IOException.class, () -> b.verify("bak-00000003", m2.sha256()), "manifiesto cambiado");
        assertThrows(IOException.class, () -> b.verify("bak-00000003", null), "ruta fuera del servidor");
    }

    @Test
    void paths_and_ids_cannot_escape() {
        Backups b = new Backups(tmp.resolve("backups"), tmp.resolve("server"));
        assertThrows(IOException.class, () -> b.create("bak-00000004", List.of("../../etc/passwd")));
        assertThrows(IOException.class, () -> b.create("../x", List.of()));
        assertThrows(IOException.class, () -> b.folder("bak-zzzzzzzz"));
    }

    @Test
    void plan_files_cover_every_kind_and_plans_round_trip() {
        List<ChunkPos> chunks = List.of(new ChunkPos(-1, 0), new ChunkPos(31, 31), new ChunkPos(32, 0));
        assertEquals(chunks, Regeneration.decode(Regeneration.encode(chunks)));
        Set<String> f = Set.copyOf(Regeneration.files("w", chunks));
        assertTrue(f.contains("w/region/r.-1.0.mca"));
        assertTrue(f.contains("w/entities/r.0.0.mca"));
        assertTrue(f.contains("w/poi/r.1.0.mca"));
        assertTrue(f.contains("w/region/c.32.0.mcc"));
        assertEquals(3 * 3 + 3 * 3, f.size(), "3 regiones distintas × 3 tipos + 3 chunks × 3 .mcc");
    }

    @Test
    void scope_selects_squares_of_chunks() {
        Regeneration.Scope r = Regeneration.Scope.radius(-1, 17, 1); // chunk (-1, 1)
        assertTrue(r.contains(new ChunkPos(-2, 0)) && r.contains(new ChunkPos(0, 2)));
        assertFalse(r.contains(new ChunkPos(1, 1)));
        assertEquals(9, r.size());
        Regeneration.Scope a = Regeneration.Scope.area(15, 15, -16, 16);
        assertEquals(new ChunkPos(-1, 0), new ChunkPos(a.minX(), a.minZ()));
        assertEquals(new ChunkPos(0, 1), new ChunkPos(a.maxX(), a.maxZ()));
        assertTrue(Regeneration.Scope.all().contains(new ChunkPos(12345, -999)));
    }

    @Test
    void protections_persist_and_a_broken_file_blocks_everything() throws IOException {
        Path file = tmp.resolve("protections.yml");
        Protections p = new Protections(file);
        assertEquals(List.of(), p.load());
        assertTrue(p.add("ciudad", "world", 100, -40, -100, 40, "la ciudad").isOk());
        assertFalse(p.add("ciudad", "world", 0, 0, 1, 1, "").isOk(), "id repetido");
        assertFalse(p.add("../x", "world", 0, 0, 1, 1, "").isOk(), "id con ruta");

        Protections again = new Protections(file);
        assertEquals(List.of(), again.load());
        Protections.Area area = again.areas().getFirst();
        assertEquals(-100, area.minX());
        assertEquals(40, area.maxZ());
        assertTrue(again.isProtected("world", new ChunkPos(6, 2), null), "el chunk 6 empieza en x=96 ≤ 100");
        assertFalse(again.isProtected("world", new ChunkPos(7, 0), null));
        assertFalse(again.isProtected("otro", new ChunkPos(0, 0), null), "otro mundo");
        assertTrue(again.isProtected("otro", new ChunkPos(2, -2), new ChunkPos(0, 0)), "radio de aparición 2");
        assertFalse(again.isProtected("otro", new ChunkPos(3, 0), new ChunkPos(0, 0)));

        Files.writeString(file, Files.readString(file).replace("x: -100", "x: muchos"));
        assertFalse(again.load().isEmpty());
        assertTrue(again.blockedReason().isPresent(), "un archivo roto bloquea la regeneración");
        assertFalse(again.add("otra", "world", 0, 0, 1, 1, "").isOk(), "y no se reescribe encima");
        assertTrue(Files.readString(file).contains("x: muchos"));
    }

    @Test
    void state_changes_are_conditional_so_a_job_runs_once() throws Exception {
        try (Database db = Database.open(tmp.resolve("j.db"))) {
            Jobs jobs = new Jobs(db);
            long now = System.currentTimeMillis();
            jobs.insert(new Job("job-0000000a", "regen", "t", "w", JobState.PLANNED, "abc123", "0,0", 1, 0, 0, null, null, "a", now, now)).get();
            assertTrue(jobs.move("job-0000000a", JobState.PLANNED, JobState.CONFIRMED, "uno").get());
            assertFalse(jobs.move("job-0000000a", JobState.PLANNED, JobState.CONFIRMED, "dos").get(), "el segundo clic no confirma otra vez");
            assertFalse(jobs.move("job-0000000a", JobState.CONFIRMED, JobState.COMPLETED, null).get(), "transición no permitida");
            assertEquals(List.of("job-0000000a"), jobs.locking("t").get().stream().map(Job::id).toList());
            assertTrue(jobs.move("job-0000000a", JobState.CONFIRMED, JobState.CANCELLED, null).get());
            assertEquals(List.of(), jobs.locking("t").get());
            assertEquals("uno", jobs.get("job-0000000a").get().orElseThrow().message(), "un mensaje nulo no borra el anterior");
        }
    }
}
