package com.sack.rpgroll.structures.deposit;

import com.sack.rpgroll.common.structure.BlockSource;
import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.catalog.MaterialSpec;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.deposit.MoneyService.ChargeResult;
import com.sack.rpgroll.structures.project.ProjectService;
import com.sack.rpgroll.structures.storage.PendingMoneyRow;
import com.sack.rpgroll.structures.storage.ProjectRecord;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

class MoneyServiceTest {

    final ProjectRepository repo = mock(ProjectRepository.class);
    final Economy eco = mock(Economy.class);
    final Player player = mock(Player.class);
    final OfflinePlayer offline = mock(OfflinePlayer.class);
    final UUID owner = UUID.randomUUID();
    final UUID bench = UUID.randomUUID();
    final World world = mock(World.class);
    final List<String> severe = new ArrayList<>();
    final Logger log = Logger.getAnonymousLogger();
    boolean active = true;
    Economy present = eco;
    MoneyService money;

    static EconomyResponse ok(double amount) {
        return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.SUCCESS, null);
    }

    static EconomyResponse fail(double amount) {
        return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.FAILURE, "no");
    }

    @BeforeEach void setUp() {
        when(player.getUniqueId()).thenReturn(owner);
        when(world.getName()).thenReturn("world");
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            public void publish(LogRecord r) {
                if (r.getLevel() == Level.SEVERE) severe.add(r.getMessage());
            }
            public void flush() { }
            public void close() { }
        });
        when(eco.has(any(OfflinePlayer.class), anyDouble())).thenReturn(true);
        when(eco.withdrawPlayer(any(OfflinePlayer.class), anyDouble())).thenAnswer(i -> ok(i.getArgument(1)));
        when(eco.depositPlayer(any(OfflinePlayer.class), anyDouble())).thenAnswer(i -> ok(i.getArgument(1)));
        when(repo.addPendingMoney(any(), anyLong())).thenReturn(CompletableFuture.completedFuture(1L));
        money = new MoneyService(repo, () -> active, () -> Optional.ofNullable(present),
                id -> id.equals(owner) ? offline : mock(OfflinePlayer.class), Runnable::run, log);
    }

    StructureDefinition def(double price) {
        List<SourceBlock> blocks = List.of(new SourceBlock(0, 0, 0, null, null));
        BlockSource src = new BlockSource() {
            public int width() { return 1; }
            public int height() { return 1; }
            public int length() { return 1; }
            public List<SourceBlock> blocks() { return blocks; }
        };
        return new StructureDefinition("casa", Map.of(), "casas", null, null, null, src,
                List.of(new MaterialSpec(List.of("minecraft:oak_planks"), 8, false)),
                price, false, null, null, 0, 0, null, null);
    }

    ProjectService projects() {
        return new ProjectService(repo, Runnable::run, log, id -> List.of(), money);
    }

    // ---- cobro ----

    @Test void chargeFailsWithoutEconomyAndCreatesNothing() {
        present = null;
        ProjectService ps = projects();
        Location at = new Location(world, 0, 64, 0);
        ProjectService.ChargeException e = assertThrows(ProjectService.ChargeException.class,
                () -> ps.create(bench, def(10), player, at, Rotation.NONE));
        assertEquals(ChargeResult.UNAVAILABLE, e.result());
        // Vault instalado pero la integración apagada: igual.
        present = eco;
        active = false;
        assertEquals(ChargeResult.UNAVAILABLE, assertThrows(ProjectService.ChargeException.class,
                () -> ps.create(bench, def(10), player, at, Rotation.NONE)).result());
        assertFalse(money.available());
        verify(repo, never()).insert(any(), anyMap(), anyInt(), anyInt(), anyInt());
        verify(eco, never()).withdrawPlayer(any(OfflinePlayer.class), anyDouble());
        assertFalse(ps.hasActive(bench), "la mesa queda libre");
        // Sin dinero en la estructura se crea sin Vault.
        when(repo.insert(any(), anyMap(), anyInt(), anyInt(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(null));
        assertNotNull(ps.create(bench, def(0), player, at, Rotation.NONE).join());
    }

    @Test void chargeNotEnough() {
        when(eco.has(any(OfflinePlayer.class), anyDouble())).thenReturn(false);
        assertEquals(ChargeResult.NOT_ENOUGH, money.charge(player, 1000));
        verify(eco, never()).withdrawPlayer(any(OfflinePlayer.class), anyDouble());
        ProjectService ps = projects();
        assertEquals(ChargeResult.NOT_ENOUGH, assertThrows(ProjectService.ChargeException.class,
                () -> ps.create(bench, def(10), player, new Location(world, 0, 64, 0), Rotation.NONE)).result());
        verify(repo, never()).insert(any(), anyMap(), anyInt(), anyInt(), anyInt());
        // Retirada rechazada por la economía (no por saldo): ERROR, nada cobrado.
        when(eco.has(any(OfflinePlayer.class), anyDouble())).thenReturn(true);
        when(eco.withdrawPlayer(any(OfflinePlayer.class), anyDouble())).thenReturn(fail(10));
        assertEquals(ChargeResult.ERROR, money.charge(player, 1000));
        assertEquals(ChargeResult.OK, money.charge(player, 0), "nada que cobrar");
    }

    @Test void chargeIsTakenBeforeTheInsertAndTheCentsAreStored() {
        when(repo.insert(any(), anyMap(), anyInt(), anyInt(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(null));
        var p = projects().create(bench, def(10.01), player, new Location(world, 0, 64, 0), Rotation.NONE).join();
        assertEquals(1001, p.moneyCents(), "Math.round(money * 100)");
        InOrder order = inOrder(eco, repo);
        order.verify(eco).withdrawPlayer(player, 10.01);
        ArgumentCaptor<ProjectRecord> rec = ArgumentCaptor.forClass(ProjectRecord.class);
        order.verify(repo).insert(rec.capture(), anyMap(), anyInt(), anyInt(), anyInt());
        assertEquals(1001, rec.getValue().moneyCents());
        verify(eco, never()).depositPlayer(any(OfflinePlayer.class), anyDouble());
    }

    @Test void createFailureRefundsTheCharge() {
        when(repo.insert(any(), anyMap(), anyInt(), anyInt(), anyInt()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("bd")));
        when(repo.hasLedger(any())).thenReturn(CompletableFuture.completedFuture(false));
        ProjectService ps = projects();
        assertThrows(CompletionException.class,
                () -> ps.create(bench, def(10), player, new Location(world, 0, 64, 0), Rotation.NONE).join());
        verify(eco).withdrawPlayer(player, 10.0);
        verify(eco).depositPlayer(offline, 10.0);
        verify(repo, never()).addPendingMoney(any(), anyLong());
        assertFalse(ps.hasActive(bench));
        assertTrue(severe.stream().noneMatch(m -> m.contains("DINERO PERDIDO")));
    }

    @Test void refundFailureLeavesPendingAndSevere() {
        when(repo.insert(any(), anyMap(), anyInt(), anyInt(), anyInt()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("bd")));
        when(repo.hasLedger(any())).thenReturn(CompletableFuture.completedFuture(false));
        when(eco.depositPlayer(any(OfflinePlayer.class), anyDouble())).thenReturn(fail(10));
        assertThrows(CompletionException.class, () -> projects().create(bench, def(10), player,
                new Location(world, 0, 64, 0), Rotation.NONE).join());
        verify(repo).addPendingMoney(owner, 1000);
        assertTrue(severe.stream().anyMatch(m -> m.contains("DINERO PERDIDO") && m.contains(owner.toString())
                && m.contains("10.00")), severe.toString());
        // Una economía que lanza tampoco pierde nada.
        severe.clear();
        when(eco.depositPlayer(any(OfflinePlayer.class), anyDouble())).thenThrow(new IllegalStateException("eco"));
        money.refund(owner, 250, "x");
        verify(repo).addPendingMoney(owner, 250);
        assertTrue(severe.stream().anyMatch(m -> m.contains("DINERO PERDIDO")));
    }

    @Test void insertThatCommitsAndThenThrowsIsNotRefunded(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
            throws Exception {
        var db = com.sack.rpgroll.structures.storage.Database.open(com.sack.rpgroll.structures.storage.Dialect.SQLITE,
                "jdbc:sqlite:" + dir.resolve("data.db"), "", "", log);
        try {
            ProjectRepository real = new ProjectRepository(db, com.sack.rpgroll.structures.storage.Dialect.SQLITE);
            // El alta llega a la base y luego el driver lanza (commit ambiguo).
            when(repo.insert(any(), anyMap(), anyInt(), anyInt(), anyInt())).thenAnswer(i -> {
                real.insert(i.getArgument(0), i.getArgument(1), i.getArgument(2), i.getArgument(3),
                        i.getArgument(4)).join();
                return CompletableFuture.failedFuture(new RuntimeException("tras el commit"));
            });
            when(repo.hasLedger(any())).thenAnswer(i -> real.hasLedger(i.getArgument(0)));
            ProjectService ps = projects();
            var p = ps.create(bench, def(10), player, new Location(world, 0, 64, 0), Rotation.NONE).join();
            verify(eco).withdrawPlayer(player, 10.0);
            verify(eco, never()).depositPlayer(any(OfflinePlayer.class), anyDouble());
            verify(repo, never()).addPendingMoney(any(), anyLong());
            assertTrue(ps.hasActive(bench), "la obra existe en la base: se recupera en memoria");
            assertEquals(1000, p.moneyCents());
            assertEquals(List.of(p.id()), real.active().join().stream().map(ProjectRecord::id).toList());
            assertTrue(severe.stream().anyMatch(m -> m.contains(p.id().toString())), severe.toString());
        } finally {
            db.close();
        }
    }

    @Test void uncheckableInsertIsNeitherRefundedNorCreated() {
        when(repo.insert(any(), anyMap(), anyInt(), anyInt(), anyInt()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("bd")));
        when(repo.hasLedger(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("bd caída")));
        ProjectService ps = projects();
        assertThrows(CompletionException.class,
                () -> ps.create(bench, def(10), player, new Location(world, 0, 64, 0), Rotation.NONE).join());
        // No se sabe si la obra entró: devolver podría pagar dos veces; queda el SEVERE para un admin.
        verify(eco, never()).depositPlayer(any(OfflinePlayer.class), anyDouble());
        assertTrue(severe.stream().anyMatch(m -> m.contains("COBRO SIN VERIFICAR") && m.contains(owner.toString())
                && m.contains("10.00")), severe.toString());
        assertFalse(ps.hasActive(bench));
    }

    // ---- pendientes ----

    @Test void payPendingPaysOnceEvenIfCalledTwice() {
        PendingMoneyRow row = new PendingMoneyRow(7, owner, 750, 1L);
        when(repo.pendingMoney(owner)).thenReturn(CompletableFuture.completedFuture(List.of(row)));
        // La marca aún no ha vuelto de la base de datos cuando llega la segunda llamada.
        CompletableFuture<Boolean> mark = new CompletableFuture<>();
        when(repo.markMoneyPaid(7)).thenReturn(mark);
        assertEquals(1, money.payPending(owner).join());
        assertEquals(0, money.payPending(owner).join());
        mark.complete(true);
        assertEquals(0, money.payPending(owner).join());
        verify(eco, times(1)).depositPlayer(offline, 7.5);
        verify(repo, times(1)).markMoneyPaid(7);
    }

    @Test void pendingPaidButNotMarkedIsNeverPaidAgainThisSession() {
        PendingMoneyRow row = new PendingMoneyRow(8, owner, 100, 1L);
        when(repo.pendingMoney(owner)).thenReturn(CompletableFuture.completedFuture(List.of(row)));
        when(repo.markMoneyPaid(8)).thenReturn(CompletableFuture.failedFuture(new RuntimeException("bd")));
        assertEquals(1, money.payPending(owner).join());
        assertEquals(0, money.payPending(owner).join());
        verify(eco, times(1)).depositPlayer(offline, 1.0);
        assertTrue(severe.stream().anyMatch(m -> m.contains("8")), severe.toString());
    }

    @Test void failedDepositStaysPendingAndIsRetried() {
        PendingMoneyRow row = new PendingMoneyRow(9, owner, 100, 1L);
        when(repo.pendingMoney(owner)).thenReturn(CompletableFuture.completedFuture(List.of(row)));
        when(repo.markMoneyPaid(9)).thenReturn(CompletableFuture.completedFuture(true));
        when(eco.depositPlayer(any(OfflinePlayer.class), anyDouble())).thenReturn(fail(1), ok(1));
        assertEquals(0, money.payPending(owner).join());
        verify(repo, never()).markMoneyPaid(anyLong());
        assertEquals(1, money.payPending(owner).join());
        verify(repo).markMoneyPaid(9);
    }

    @Test void payPendingWithoutVaultLeavesEverythingPending() {
        active = false;
        assertEquals(0, money.payPending(owner).join());
        verify(repo, never()).pendingMoney(any());
        verify(eco, never()).depositPlayer(any(OfflinePlayer.class), anyDouble());
    }
}
