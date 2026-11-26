package com.sack.rpgroll.structures.storage;

import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.structures.project.ProjectState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static com.sack.rpgroll.structures.storage.Movement.Kind.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectRepositoryTest {

    @TempDir Path dir;
    Database db;
    ProjectRepository repo;
    final UUID player = UUID.randomUUID();
    final String KEY = "minecraft:oak_planks";

    @BeforeEach void setUp() throws Exception {
        db = Database.open(Dialect.SQLITE, "jdbc:sqlite:" + dir.resolve("data.db"), "", "",
                Logger.getAnonymousLogger());
        repo = new ProjectRepository(db, Dialect.SQLITE);
    }

    @AfterEach void tearDown() {
        db.close();
    }

    ProjectRecord insert(ProjectState state, int x) {
        ProjectRecord p = new ProjectRecord(UUID.randomUUID(), UUID.randomUUID(), "casa", player, state, "world", x,
                64, 0, Rotation.CW_90, 0, 0, 0, 100, null, 1L, 1L);
        repo.insert(p, Map.of(KEY, 10), 5, 3, 4).join();
        return p;
    }

    Movement mv(ProjectRecord p, Movement.Kind kind, int amount) {
        return new Movement(UUID.randomUUID(), p.id(), player, kind, KEY, amount);
    }

    @Test void insertAndActiveRoundTrip() {
        ProjectRecord p = insert(ProjectState.WAITING_MATERIALS, 0);
        insert(ProjectState.CANCELLED, 50);
        assertEquals(List.of(p), repo.active().join());
        assertEquals(new MaterialRow(10, 0, 0), repo.materials(p.id()).join().get(KEY));
    }

    @Test void depositIsIdempotentById() {
        ProjectRecord p = insert(ProjectState.WAITING_MATERIALS, 0);
        Movement m = mv(p, DEPOSIT, 4);
        assertTrue(repo.applyMovement(m).join());
        assertFalse(repo.applyMovement(m).join());
        assertEquals(4, repo.materials(p.id()).join().get(KEY).deposited());
    }

    @Test void withdrawBelowConsumedIsRejected() {
        ProjectRecord p = insert(ProjectState.BUILDING, 0);
        assertTrue(repo.applyMovement(mv(p, DEPOSIT, 6)).join());
        assertTrue(repo.applyMovement(mv(p, CONSUME, 4)).join());
        assertFalse(repo.applyMovement(mv(p, WITHDRAW, 3)).join());
        assertTrue(repo.applyMovement(mv(p, REFUND, 2)).join());
        assertFalse(repo.applyMovement(mv(p, CONSUME, 1)).join());
        assertEquals(new MaterialRow(10, 4, 4), repo.materials(p.id()).join().get(KEY));
    }

    @Test void rejectedMovementLeavesNoLedgerRow() {
        ProjectRecord p = insert(ProjectState.BUILDING, 0);
        Movement bad = mv(p, WITHDRAW, 1);
        assertFalse(repo.applyMovement(bad).join());
        assertTrue(repo.applyMovement(mv(p, DEPOSIT, 1)).join());
        // El libro se deshizo con el rechazo: el mismo id ya puede aplicarse.
        assertTrue(repo.applyMovement(new Movement(bad.id(), p.id(), player, WITHDRAW, KEY, 1)).join());
        assertFalse(repo.applyMovement(mv(p, DEPOSIT, 0)).join());
    }

    @Test void applyMovementsIsOneTransactionAndIdempotent() {
        ProjectRecord p = insert(ProjectState.BUILDING, 0);
        Movement dep = mv(p, DEPOSIT, 6);
        Movement tooMuch = mv(p, CONSUME, 7);
        Movement ok = mv(p, CONSUME, 4);
        assertEquals(List.of(true, false, true, false, false),
                repo.applyMovements(List.of(dep, tooMuch, ok, dep, mv(p, CONSUME, 0))).join());
        assertEquals(new MaterialRow(10, 6, 4), repo.materials(p.id()).join().get(KEY));
        // El rechazado no dejó apunte: su id puede entrar luego.
        assertTrue(repo.applyMovement(new Movement(tooMuch.id(), p.id(), player, CONSUME, KEY, 2)).join());
        // Un fallo a mitad (aquí, jugador null; INSERT OR IGNORE se traga el NOT NULL) deshace todo el lote.
        Movement more = mv(p, DEPOSIT, 3);
        assertThrows(java.util.concurrent.CompletionException.class, () -> repo.applyMovements(List.of(more,
                new Movement(UUID.randomUUID(), p.id(), null, DEPOSIT, KEY, 1))).join());
        assertEquals(new MaterialRow(10, 6, 6), repo.materials(p.id()).join().get(KEY));
        assertTrue(repo.applyMovement(more).join());
    }

    @Test void completeWritesBuildAndState() {
        ProjectRecord p = insert(ProjectState.BUILDING, 0);
        Box box = new Box(0, 64, 0, 4, 66, 3);
        repo.complete(p.id(), new BuildRecord(UUID.randomUUID(), p.id(), "casa", player, "world", 0, 64, 0,
                Rotation.CW_90, box, 9L)).join();
        assertTrue(repo.active().join().isEmpty());
        assertEquals(List.of(box), repo.boxesIn("world").join());
    }

    @Test void completeIsAtomic() {
        ProjectRecord p = insert(ProjectState.BUILDING, 0);
        BuildRecord b = new BuildRecord(UUID.randomUUID(), p.id(), "casa", player, "world", 0, 64, 0,
                Rotation.NONE, new Box(0, 0, 0, 0, 0, 0), 9L);
        assertTrue(repo.complete(p.id(), b).join());
        ProjectRecord q = insert(ProjectState.BUILDING, 20);
        // Mismo id de construcción: el INSERT falla y el UPDATE de q debe deshacerse.
        assertThrows(java.util.concurrent.CompletionException.class, () -> repo.complete(q.id(), b).join());
        assertEquals(List.of(q), repo.active().join(), "la obra no quedó COMPLETED sin su construcción");
    }

    @Test void buildsOfListsOnlyThatOwnerNewestFirst() {
        ProjectRecord a = insert(ProjectState.BUILDING, 0);
        ProjectRecord b = insert(ProjectState.BUILDING, 20);
        BuildRecord older = new BuildRecord(UUID.randomUUID(), a.id(), "casa", player, "world", 0, 64, 0,
                Rotation.CW_90, new Box(0, 64, 0, 4, 66, 3), 5L);
        BuildRecord newer = new BuildRecord(UUID.randomUUID(), b.id(), "torre", player, "nether", 20, 64, 0,
                Rotation.NONE, new Box(20, 64, 0, 24, 66, 3), 9L);
        assertTrue(repo.complete(a.id(), older).join());
        assertTrue(repo.complete(b.id(), newer).join());
        assertEquals(List.of(newer, older), repo.buildsOf(player).join());
        assertTrue(repo.buildsOf(UUID.randomUUID()).join().isEmpty());
    }

    @Test void allBuildsListsEveryOwner() {
        ProjectRecord a = insert(ProjectState.BUILDING, 0);
        ProjectRecord b = insert(ProjectState.BUILDING, 20);
        BuildRecord mine = new BuildRecord(UUID.randomUUID(), a.id(), "casa", player, "world", 0, 64, 0,
                Rotation.CW_90, new Box(0, 64, 0, 4, 66, 3), 5L);
        UUID other = UUID.randomUUID();
        BuildRecord theirs = new BuildRecord(UUID.randomUUID(), b.id(), "torre", other, "nether", 20, 64, 0,
                Rotation.NONE, new Box(20, 64, 0, 24, 66, 3), 9L);
        assertTrue(repo.complete(a.id(), mine).join());
        assertTrue(repo.complete(b.id(), theirs).join());
        assertEquals(Set.of(mine, theirs), Set.copyOf(repo.allBuilds().join()));
    }

    @Test void deleteBuildRemovesRowAndFreesItsBox() {
        ProjectRecord a = insert(ProjectState.BUILDING, 0);
        BuildRecord b = new BuildRecord(UUID.randomUUID(), a.id(), "casa", player, "world", 0, 64, 0,
                Rotation.NONE, new Box(0, 64, 0, 4, 66, 3), 5L);
        assertTrue(repo.complete(a.id(), b).join());
        assertTrue(repo.deleteBuild(b.id()).join());
        assertFalse(repo.deleteBuild(b.id()).join(), "la segunda vez ya no hay fila");
        assertTrue(repo.allBuilds().join().isEmpty());
        assertTrue(repo.boxesIn("world").join().isEmpty());
    }

    @Test void moduleDataRoundTripAndOverwrite() {
        UUID build = UUID.randomUUID();
        assertTrue(repo.moduleData(build).join().isEmpty());
        repo.saveModuleData(build, "griefprevention:1").join();
        repo.saveModuleData(build, "griefprevention:2").join();
        assertEquals(java.util.Optional.of("griefprevention:2"), repo.moduleData(build).join());
        repo.deleteModuleData(build).join();
        assertTrue(repo.moduleData(build).join().isEmpty());
    }

    @Test void activeBoxesByProjectSkipsFinishedOnes() {
        ProjectRecord a = insert(ProjectState.READY, 10);
        insert(ProjectState.CANCELLED, 50);
        assertEquals(Map.of(a.id(), new Box(10, 64, 0, 14, 66, 3)), repo.activeBoxes().join());
    }

    @Test void boxesInCoversActiveProjectsAndOnlyThatWorld() {
        insert(ProjectState.READY, 10);
        assertEquals(List.of(new Box(10, 64, 0, 14, 66, 3)), repo.boxesIn("world").join());
        assertTrue(repo.boxesIn("nether").join().isEmpty());
    }

    @Test void saveProgressUpdatesRow() {
        ProjectRecord p = insert(ProjectState.READY, 0);
        repo.saveProgress(p.id(), ProjectState.ERROR, 7, 5, 2, "boom").join();
        ProjectRecord r = repo.active().join().get(0);
        assertEquals(ProjectState.ERROR, r.state());
        assertEquals(7, r.cursor());
        assertEquals("boom", r.error());
    }

    @Test void claimOnlyOnce() {
        long id = repo.addPending(player, new byte[]{1, 2, 3}).join();
        assertEquals(1, repo.pending(player).join().size());
        assertArrayEquals(new byte[]{1, 2, 3}, repo.pending(player).join().get(0).item());
        assertTrue(repo.claim(id).join());
        assertFalse(repo.claim(id).join());
        assertTrue(repo.pending(player).join().isEmpty());
    }

    @Test void saveProgressDoesNotReviveTerminalState() {
        ProjectRecord p = insert(ProjectState.BUILDING, 0);
        assertTrue(repo.saveProgress(p.id(), ProjectState.CANCELLED, 1, 1, 0, null).join());
        assertFalse(repo.saveProgress(p.id(), ProjectState.BUILDING, 9, 9, 0, null).join());
        assertTrue(repo.active().join().isEmpty());
        assertFalse(repo.saveProgress(UUID.randomUUID(), ProjectState.BUILDING, 0, 0, 0, null).join());
    }

    @Test void completeTwiceWritesOneBuild() {
        ProjectRecord p = insert(ProjectState.BUILDING, 0);
        Box box = new Box(0, 64, 0, 4, 66, 3);
        assertTrue(repo.complete(p.id(), new BuildRecord(UUID.randomUUID(), p.id(), "casa", player, "world", 0, 64,
                0, Rotation.NONE, box, 9L)).join());
        assertFalse(repo.complete(p.id(), new BuildRecord(UUID.randomUUID(), p.id(), "casa", player, "world", 0, 64,
                0, Rotation.NONE, box, 10L)).join());
        assertEquals(List.of(box), repo.boxesIn("world").join());
        UUID ghost = UUID.randomUUID();
        assertFalse(repo.complete(ghost, new BuildRecord(UUID.randomUUID(), ghost, "casa", player, "world", 0, 0, 0,
                Rotation.NONE, box, 11L)).join());
        assertEquals(List.of(box), repo.boxesIn("world").join());
    }

    @Test void refundSubtractsAndClaimOnlyWritesLedger() {
        ProjectRecord p = insert(ProjectState.BUILDING, 0);
        assertTrue(repo.applyMovement(mv(p, DEPOSIT, 6)).join());
        assertTrue(repo.applyMovement(mv(p, REFUND, 2)).join());
        assertEquals(new MaterialRow(10, 4, 0), repo.materials(p.id()).join().get(KEY));
        assertTrue(repo.applyMovement(mv(p, CLAIM, 3)).join());
        assertEquals(new MaterialRow(10, 4, 0), repo.materials(p.id()).join().get(KEY));
        Movement claim = mv(p, CLAIM, 1);
        assertTrue(repo.applyMovement(claim).join());
        assertFalse(repo.applyMovement(claim).join(), "CLAIM también es idempotente por id");
    }

    @Test void cancelIsAllOrNothing() {
        ProjectRecord p = insert(ProjectState.WAITING_MATERIALS, 0);
        assertTrue(repo.applyMovement(mv(p, DEPOSIT, 6)).join());
        // el segundo reembolso dejaría deposited por debajo de cero: todo se deshace
        assertFalse(repo.cancel(p.id(), List.of(mv(p, REFUND, 3), mv(p, REFUND, 9))).join());
        assertEquals(new MaterialRow(10, 6, 0), repo.materials(p.id()).join().get(KEY));
        assertEquals(List.of(p), repo.active().join());
        Movement ok = mv(p, REFUND, 3);
        assertTrue(repo.cancel(p.id(), List.of(ok)).join());
        assertEquals(new MaterialRow(10, 3, 0), repo.materials(p.id()).join().get(KEY));
        assertTrue(repo.active().join().isEmpty());
        // ya terminal: no se repite ni se reembolsa otra vez
        assertFalse(repo.cancel(p.id(), List.of(mv(p, REFUND, 1))).join());
        assertEquals(3, repo.materials(p.id()).join().get(KEY).deposited());
    }

    @Test void sourceHashRoundTrips() {
        ProjectRecord p = new ProjectRecord(UUID.randomUUID(), UUID.randomUUID(), "casa", player,
                ProjectState.WAITING_MATERIALS, "world", 0, 64, 0, Rotation.NONE, 0, 0, 0, 100, null, 1L, 1L, "h1");
        repo.insert(p, Map.of(KEY, 10), 5, 3, 4).join();
        ProjectRecord old = insert(ProjectState.WAITING_MATERIALS, 50);
        List<ProjectRecord> active = repo.active().join();
        assertEquals("h1", active.stream().filter(r -> r.id().equals(p.id())).findFirst().orElseThrow().sourceHash());
        assertNull(active.stream().filter(r -> r.id().equals(old.id())).findFirst().orElseThrow().sourceHash());
    }

    // ---- dinero ----

    ProjectRecord insertPaid(long cents) {
        ProjectRecord p = new ProjectRecord(UUID.randomUUID(), UUID.randomUUID(), "casa", player,
                ProjectState.WAITING_MATERIALS, "world", 0, 64, 0, Rotation.NONE, 0, 0, 0, 100, null, 1L, 1L, null,
                cents);
        repo.insert(p, Map.of(KEY, 10), 1, 1, 1).join();
        return p;
    }

    /** Filas del libro de una obra: tipo, jugador, material y cantidad. */
    List<String> ledger(UUID project) {
        return db.submit(c -> {
            List<String> out = new java.util.ArrayList<>();
            try (var ps = c.prepareStatement("SELECT kind, player, material_key, amount FROM ledger"
                    + " WHERE project_id = ? ORDER BY kind")) {
                ps.setString(1, project.toString());
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(rs.getString(1) + " " + rs.getString(2) + " " + rs.getString(3) + " " + rs.getLong(4));
                    }
                }
            }
            return out;
        }).join();
    }

    @Test void insertWritesTheMoneyChargeInTheSameTransaction() {
        ProjectRecord p = insertPaid(1000);
        assertEquals(1000, repo.active().join().getFirst().moneyCents());
        assertTrue(repo.hasLedger(ProjectRepository.chargeId(p.id())).join());
        assertFalse(repo.hasLedger(UUID.randomUUID()).join());
        assertEquals(List.of("MONEY_CHARGE " + player + " " + Movement.MONEY_KEY + " 1000"), ledger(p.id()));
        // Sin dinero no hay apunte.
        ProjectRecord free = insert(ProjectState.WAITING_MATERIALS, 50);
        assertTrue(ledger(free.id()).isEmpty());
        // Un alta que falla (id repetido) no deja el cobro apuntado dos veces ni a medias.
        ProjectRecord again = new ProjectRecord(p.id(), UUID.randomUUID(), "casa", player,
                ProjectState.WAITING_MATERIALS, "world", 0, 64, 0, Rotation.NONE, 0, 0, 0, 100, null, 1L, 1L, null,
                500);
        assertThrows(java.util.concurrent.CompletionException.class,
                () -> repo.insert(again, Map.of(KEY, 10), 1, 1, 1).join());
        assertEquals(1, ledger(p.id()).size());
    }

    @Test void cancelWithMoneyRefundLeavesPendingForTheOwner() {
        ProjectRecord p = insertPaid(1000);
        UUID canceller = UUID.randomUUID();
        Movement cash = new Movement(UUID.randomUUID(), p.id(), canceller, MONEY_REFUND, Movement.MONEY_KEY, 750);
        assertTrue(repo.cancel(p.id(), List.of(cash)).join());
        List<PendingMoneyRow> rows = repo.pendingMoney(player).join();
        assertEquals(1, rows.size());
        assertEquals(750, rows.getFirst().amountCents());
        assertTrue(repo.pendingMoney(canceller).join().isEmpty(), "al dueño de la obra, no a quien cancela");
        assertTrue(ledger(p.id()).contains("MONEY_REFUND " + canceller + " " + Movement.MONEY_KEY + " 750"));
        // Repetir la cancelación no deja otra fila: la obra ya es final y todo se deshace.
        assertFalse(repo.cancel(p.id(), List.of(new Movement(UUID.randomUUID(), p.id(), canceller, MONEY_REFUND,
                Movement.MONEY_KEY, 750))).join());
        assertEquals(1, repo.pendingMoney(player).join().size());
    }

    @Test void failedCancelLeavesNoPendingMoney() {
        ProjectRecord p = insertPaid(1000);
        Movement cash = new Movement(UUID.randomUUID(), p.id(), player, MONEY_REFUND, Movement.MONEY_KEY, 750);
        // Un reembolso de material imposible tumba la transacción entera, también el dinero.
        assertFalse(repo.cancel(p.id(), List.of(cash, mv(p, REFUND, 5))).join());
        assertTrue(repo.pendingMoney(player).join().isEmpty());
        assertFalse(ledger(p.id()).stream().anyMatch(l -> l.startsWith("MONEY_REFUND")));
        assertEquals(1, repo.active().join().size());
    }

    @Test void pendingMoneyIsMarkedPaidOnce() {
        long id = repo.addPendingMoney(player, 250).join();
        assertEquals(List.of(250L), repo.pendingMoney(player).join().stream().map(PendingMoneyRow::amountCents).toList());
        assertTrue(repo.markMoneyPaid(id).join());
        assertFalse(repo.markMoneyPaid(id).join());
        assertTrue(repo.pendingMoney(player).join().isEmpty());
    }
}
