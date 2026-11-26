package com.sack.rpgroll.structures.deposit;

import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.structures.api.MaterialProvider;
import com.sack.rpgroll.structures.material.MaterialRegistry;
import com.sack.rpgroll.structures.project.Project;
import com.sack.rpgroll.structures.project.ProjectState;
import com.sack.rpgroll.structures.storage.MaterialRow;
import com.sack.rpgroll.structures.storage.Movement;
import com.sack.rpgroll.structures.storage.PendingRow;
import com.sack.rpgroll.structures.storage.ProjectRecord;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReturnServiceTest {

    final ProjectRepository repo = mock(ProjectRepository.class);
    final Player player = mock(Player.class);
    final PlayerInventory inv = mock(PlayerInventory.class);
    final UUID playerId = UUID.randomUUID();
    final MaterialRegistry materials = new MaterialRegistry();
    final List<Integer> given = new ArrayList<>();
    final List<String> messages = new ArrayList<>();
    final List<Project> finished = new ArrayList<>();
    boolean hasProvider = true;
    /** Id (sin tipo) cuyo proveedor lanza al crear el ítem. */
    String broken;
    Player online = player;
    /** Otros jugadores conectados (por ejemplo, el dueño de la mesa que pulsa por el de la obra). */
    final Map<UUID, Player> others = new HashMap<>();
    final MoneyService money = mock(MoneyService.class);
    ReturnService service;

    @BeforeEach void setUp() {
        materials.register(new MaterialProvider() {
            public String type() { return "minecraft"; }
            public boolean knows(String id) { return true; }
            public boolean matches(String id, ItemStack s) { return false; }
            public Optional<ItemStack> sample(String id) {
                if (id.equals(broken)) {
                    throw new IllegalStateException("create roto");
                }
                return hasProvider ? Optional.of(stack(1)) : Optional.empty();
            }
        });
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.isOnline()).thenReturn(true);
        when(player.getInventory()).thenReturn(inv);
        // todo cabe y se anota la cantidad entregada
        when(inv.addItem(any(ItemStack.class))).thenAnswer(i -> {
            given.add(((ItemStack) i.getArgument(0)).getAmount());
            return new HashMap<Integer, ItemStack>();
        });
        when(repo.addPending(any(), any())).thenReturn(CompletableFuture.completedFuture(1L));
        when(money.payPending(any())).thenReturn(CompletableFuture.completedFuture(1));
        when(repo.saveProgress(any(), any(), anyInt(), anyInt(), anyInt(), any()))
                .thenReturn(CompletableFuture.completedFuture(true));
        service = service(s -> new byte[]{7}, b -> mock(ItemStack.class));
    }

    ReturnService service(java.util.function.Function<ItemStack, byte[]> ser,
                          java.util.function.Function<byte[], ItemStack> de) {
        return new ReturnService(repo, materials, finished::add,
                id -> id.equals(playerId) ? online : others.get(id), Runnable::run,
                (p, key) -> messages.add(key), ser, de, money, Logger.getAnonymousLogger());
    }

    /** Pila falsa de máximo 4 con cantidad mutable. */
    ItemStack stack(int amount) {
        ItemStack s = mock(ItemStack.class);
        AtomicInteger n = new AtomicInteger(amount);
        when(s.getMaxStackSize()).thenReturn(4);
        when(s.getAmount()).thenAnswer(i -> n.get());
        doAnswer(i -> { n.set(i.getArgument(0)); return null; }).when(s).setAmount(anyInt());
        when(s.clone()).thenAnswer(i -> stack(n.get()));
        return s;
    }

    Project project(ProjectState state, int planksDep, int planksUsed, int stoneDep) {
        Map<String, MaterialRow> rows = new LinkedHashMap<>();
        rows.put("minecraft:oak_planks", new MaterialRow(200, planksDep, planksUsed));
        rows.put("minecraft:stone", new MaterialRow(4, stoneDep, 0));
        return new Project(new ProjectRecord(UUID.randomUUID(), UUID.randomUUID(), "house", playerId, state, "world",
                0, 0, 0, Rotation.values()[0], 0, 0, 0, 100, null, 0, 0), rows);
    }

    void applyAll(boolean ok) {
        when(repo.applyMovement(any())).thenReturn(CompletableFuture.completedFuture(ok));
    }

    List<Movement> movements(int times) {
        ArgumentCaptor<Movement> c = ArgumentCaptor.forClass(Movement.class);
        verify(repo, times(times)).applyMovement(c.capture());
        return c.getAllValues();
    }

    // ---- give ----

    @Test void refundOverflowGoesToPending() {
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(playerId);
        when(p.isOnline()).thenReturn(true);
        PlayerInventory pinv = mock(PlayerInventory.class);
        when(p.getInventory()).thenReturn(pinv);
        ItemStack fits = mock(ItemStack.class);
        ItemStack full = mock(ItemStack.class);
        ItemStack rest = mock(ItemStack.class);
        when(pinv.addItem(fits)).thenReturn(new HashMap<>());
        when(pinv.addItem(full)).thenReturn(new HashMap<>(Map.of(0, rest)));
        service.give(p, List.of(fits, full));
        verify(repo, times(1)).addPending(playerId, new byte[]{7});
    }

    @Test void offlinePlayerRefundGoesToPending() {
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(playerId);
        when(p.isOnline()).thenReturn(false);
        service.give(p, List.of(mock(ItemStack.class), mock(ItemStack.class)));
        verify(repo, times(2)).addPending(playerId, new byte[]{7});
        verify(p, never()).getInventory();
    }

    // ---- withdraw ----

    @Test void withdrawResetsToWaiting() {
        Project pr = project(ProjectState.READY, 10, 0, 4);
        applyAll(true);
        assertTrue(service.withdraw(player, pr).join());
        List<Movement> m = movements(2);
        assertTrue(m.stream().allMatch(x -> x.kind() == Movement.Kind.WITHDRAW && x.project().equals(pr.id())));
        assertEquals(ProjectState.WAITING_MATERIALS, pr.state());
        assertEquals(0, pr.materials().get("minecraft:oak_planks").deposited());
        assertEquals(0, pr.materials().get("minecraft:stone").deposited());
        // planks 10 -> 4+4+2, stone 4 -> 4
        assertEquals(List.of(4, 4, 2, 4), given);
        verify(repo).saveProgress(eq(pr.id()), eq(ProjectState.WAITING_MATERIALS), anyInt(), anyInt(), anyInt(), any());
        verify(player).saveData();
        assertFalse(pr.busy());
    }

    @Test void withdrawFailedMovementGivesNothingForThatMaterial() {
        Project pr = project(ProjectState.WAITING_MATERIALS, 10, 0, 4);
        when(repo.applyMovement(any())).thenAnswer(i -> CompletableFuture.completedFuture(
                !((Movement) i.getArgument(0)).materialKey().equals("minecraft:stone")));
        assertFalse(service.withdraw(player, pr).join());
        assertEquals(4, pr.materials().get("minecraft:stone").deposited());
        assertEquals(0, pr.materials().get("minecraft:oak_planks").deposited());
        assertEquals(List.of(4, 4, 2), given);
        assertFalse(pr.busy());
    }

    @Test void withdrawWhileBusyOrWrongStateDoesNothing() {
        Project pr = project(ProjectState.READY, 10, 0, 4);
        pr.setBusy(true);
        assertFalse(service.withdraw(player, pr).join());
        pr.setBusy(false);
        pr.setState(ProjectState.BUILDING);
        assertFalse(service.withdraw(player, pr).join());
        verify(repo, never()).applyMovement(any());
    }

    @Test void withdrawOfflinePlayerGoesToPending() {
        Project pr = project(ProjectState.WAITING_MATERIALS, 0, 0, 4);
        applyAll(true);
        online = null;
        assertTrue(service.withdraw(player, pr).join());
        verify(repo, times(1)).addPending(playerId, new byte[]{7});
        verify(inv, never()).addItem(any(ItemStack.class));
    }

    /** El dueño de la mesa (no de la obra) pulsa: lo devuelto es del dueño de la obra. */
    Player benchOwner() {
        Player admin = mock(Player.class);
        UUID adminId = UUID.randomUUID();
        when(admin.getUniqueId()).thenReturn(adminId);
        when(admin.isOnline()).thenReturn(true);
        PlayerInventory adminInv = mock(PlayerInventory.class);
        when(admin.getInventory()).thenReturn(adminInv);
        others.put(adminId, admin);
        return admin;
    }

    @Test void refundGoesToProjectOwnerNotClicker() {
        Player admin = benchOwner();
        Project pr = project(ProjectState.WAITING_MATERIALS, 10, 0, 0);
        cancelResult(true);
        assertTrue(service.cancel(admin, pr, 100).join());
        assertEquals(List.of(4, 4, 2), given, "al inventario del dueño de la obra");
        verify(admin.getInventory(), never()).addItem(any(ItemStack.class));
        assertEquals(admin.getUniqueId(), refunds(1).get(0).player(), "el libro apunta quién canceló");

        Project pw = project(ProjectState.READY, 0, 0, 4);
        applyAll(true);
        assertTrue(service.withdraw(admin, pw).join());
        assertEquals(List.of(4, 4, 2, 4), given);
        verify(admin.getInventory(), never()).addItem(any(ItemStack.class));
    }

    @Test void refundToOfflineProjectOwnerGoesToTheirPending() {
        Player admin = benchOwner();
        online = null;
        Project pr = project(ProjectState.WAITING_MATERIALS, 0, 0, 4);
        cancelResult(true);
        assertTrue(service.cancel(admin, pr, 100).join());
        Project pw = project(ProjectState.WAITING_MATERIALS, 0, 0, 4);
        applyAll(true);
        assertTrue(service.withdraw(admin, pw).join());
        verify(repo, times(2)).addPending(playerId, new byte[]{7});
        verify(repo, never()).addPending(eq(admin.getUniqueId()), any());
        verify(admin.getInventory(), never()).addItem(any(ItemStack.class));
    }

    // ---- cancel ----

    void cancelResult(boolean ok) {
        when(repo.cancel(any(), anyList())).thenReturn(CompletableFuture.completedFuture(ok));
    }

    @SuppressWarnings("unchecked")
    List<Movement> refunds(int times) {
        ArgumentCaptor<List<Movement>> c = ArgumentCaptor.forClass(List.class);
        verify(repo, times(times)).cancel(any(), c.capture());
        return c.getValue();
    }

    @Test void cancelRefundsUnspentPercent() {
        Project pr = project(ProjectState.PAUSED, 100, 40, 0);
        cancelResult(true);
        assertTrue(service.cancel(player, pr, 50).join());
        List<Movement> m = refunds(1);
        assertEquals(1, m.size());
        assertEquals(Movement.Kind.REFUND, m.get(0).kind());
        assertEquals(30, m.get(0).amount());
        assertEquals(ProjectState.CANCELLED, pr.state());
        assertEquals(70, pr.materials().get("minecraft:oak_planks").deposited());
        assertEquals(List.of(4, 4, 4, 4, 4, 4, 4, 2), given);
        assertEquals(List.of(pr), finished);
        verify(repo, never()).applyMovement(any());
        verify(player).saveData();
        assertFalse(pr.busy());
    }

    /** Obra con dinero cobrado y el cursor a mitad. */
    Project paid(ProjectState state, int cursor, int total, long cents) {
        Map<String, MaterialRow> rows = new LinkedHashMap<>();
        rows.put("minecraft:stone", new MaterialRow(4, 4, 0));
        return new Project(new ProjectRecord(UUID.randomUUID(), UUID.randomUUID(), "house", playerId, state, "world",
                0, 0, 0, Rotation.values()[0], cursor, cursor, 0, total, null, 0, 0, null, cents), rows);
    }

    @Test void cancelRefundsUnspentMoneyProportionally() {
        Project pr = paid(ProjectState.PAUSED, 25, 100, 1000);
        cancelResult(true);
        assertTrue(service.cancel(player, pr, 100).join());
        List<Movement> m = refunds(1);
        Movement cash = m.stream().filter(x -> x.kind() == Movement.Kind.MONEY_REFUND).findFirst().orElseThrow();
        assertEquals(750, cash.amount(), "10,00 con 25 de 100 colocados -> 7,50");
        assertEquals(Movement.MONEY_KEY, cash.materialKey());
        assertEquals(List.of(4), given, "el dinero no se entrega como ítem");
        verify(money).payPending(playerId);

        // Con la mitad de devolución y obra sin empezar: 1000 x 1 x 50 % = 500.
        Project fresh = paid(ProjectState.WAITING_MATERIALS, 0, 100, 1000);
        assertTrue(service.cancel(player, fresh, 50).join());
        ArgumentCaptor<List<Movement>> c = ArgumentCaptor.forClass(List.class);
        verify(repo, times(2)).cancel(any(), c.capture());
        assertEquals(500, c.getValue().stream().filter(x -> x.kind() == Movement.Kind.MONEY_REFUND)
                .findFirst().orElseThrow().amount());
    }

    @Test void moneyRefundMath() {
        assertEquals(750, com.sack.rpgroll.structures.project.DepositMath.moneyRefund(1000, 100, 25, 100));
        assertEquals(375, com.sack.rpgroll.structures.project.DepositMath.moneyRefund(1000, 100, 25, 50));
        assertEquals(800, com.sack.rpgroll.structures.project.DepositMath.moneyRefund(1000, 0, 0, 80), "total 0");
        assertEquals(0, com.sack.rpgroll.structures.project.DepositMath.moneyRefund(1000, 100, 100, 100));
        assertEquals(0, com.sack.rpgroll.structures.project.DepositMath.moneyRefund(0, 100, 0, 100));
        assertEquals(333, com.sack.rpgroll.structures.project.DepositMath.moneyRefund(1000, 3, 2, 100), "redondeo");
    }

    @Test void cancelRefundGoesToOwnerNotCanceller() {
        Player admin = benchOwner();
        online = null;
        Project pr = paid(ProjectState.BUILDING, 50, 100, 1000);
        cancelResult(true);
        assertTrue(service.cancel(admin, pr, 100).join());
        verify(money).payPending(playerId);
        verify(money, never()).payPending(admin.getUniqueId());
        Movement cash = refunds(1).stream().filter(x -> x.kind() == Movement.Kind.MONEY_REFUND)
                .findFirst().orElseThrow();
        assertEquals(500, cash.amount());
        assertEquals(playerId, cash.player(), "el apunte de dinero nombra al dueño de la obra, no a quien cancela");
    }

    @Test void failedCancelPaysNoMoney() {
        Project pr = paid(ProjectState.BUILDING, 50, 100, 1000);
        cancelResult(false);
        assertFalse(service.cancel(player, pr, 100).join());
        verify(money, never()).payPending(any());
        // Sin dinero no hay MONEY_REFUND.
        Project free = paid(ProjectState.BUILDING, 50, 100, 0);
        cancelResult(true);
        assertTrue(service.cancel(player, free, 100).join());
        ArgumentCaptor<List<Movement>> c = ArgumentCaptor.forClass(List.class);
        verify(repo, times(2)).cancel(any(), c.capture());
        assertTrue(c.getValue().stream().noneMatch(x -> x.kind() == Movement.Kind.MONEY_REFUND));
        verify(money, never()).payPending(any());
    }

    @Test void cancelFailedKeepsProjectAndItemsUntouched() {
        Project pr = project(ProjectState.WAITING_MATERIALS, 10, 0, 0);
        cancelResult(false);
        assertFalse(service.cancel(player, pr, 100).join());
        assertEquals(ProjectState.WAITING_MATERIALS, pr.state());
        assertEquals(10, pr.materials().get("minecraft:oak_planks").deposited());
        assertTrue(given.isEmpty());
        assertTrue(finished.isEmpty());
        assertFalse(pr.busy());
    }

    @Test void cancelRetryAfterFailureRefundsPolicyOnce() {
        Project pr = project(ProjectState.WAITING_MATERIALS, 100, 0, 4);
        when(repo.cancel(any(), anyList())).thenReturn(CompletableFuture.completedFuture(false),
                CompletableFuture.completedFuture(true));
        assertFalse(service.cancel(player, pr, 50).join());
        assertTrue(service.cancel(player, pr, 50).join());
        ArgumentCaptor<List<Movement>> c = ArgumentCaptor.forClass(List.class);
        verify(repo, times(2)).cancel(any(), c.capture());
        for (List<Movement> l : c.getAllValues()) {
            assertEquals(List.of(50, 2), l.stream().map(Movement::amount).toList());
        }
        assertEquals(50, pr.materials().get("minecraft:oak_planks").deposited());
        assertEquals(1, finished.size());
    }

    @Test void cancelFinalStatesAreRejected() {
        Project pr = project(ProjectState.COMPLETED, 10, 0, 0);
        assertFalse(service.cancel(player, pr, 100).join());
        pr.setState(ProjectState.CANCELLED);
        assertFalse(service.cancel(player, pr, 100).join());
        verify(repo, never()).cancel(any(), anyList());
    }

    @Test void cancelWithThrowingProviderStillFinishesAndDeliversTheRest() {
        broken = "oak_planks";
        Project pr = project(ProjectState.WAITING_MATERIALS, 10, 0, 4);
        cancelResult(true);
        assertTrue(service.cancel(player, pr, 100).join());
        assertEquals(List.of(pr), finished, "la obra sale de la memoria aunque un material falle");
        assertEquals(ProjectState.CANCELLED, pr.state());
        assertEquals(List.of(4), given, "la piedra se entrega");
        assertFalse(pr.busy());
    }

    @Test void withdrawWithThrowingProviderDeliversTheOtherMaterials() {
        broken = "oak_planks";
        Project pr = project(ProjectState.READY, 10, 0, 4);
        applyAll(true);
        assertTrue(service.withdraw(player, pr).join());
        assertEquals(List.of(4), given, "la piedra se entrega");
        assertEquals(ProjectState.WAITING_MATERIALS, pr.state());
        assertEquals(0, pr.materials().get("minecraft:stone").deposited());
        assertFalse(pr.busy());
    }

    @Test void missingProviderIsLoggedNotInvented() {
        hasProvider = false;
        Project pr = project(ProjectState.WAITING_MATERIALS, 0, 0, 4);
        cancelResult(true);
        assertTrue(service.cancel(player, pr, 100).join());
        refunds(1);
        assertEquals(List.of("deposit.provider-missing"), messages);
        verify(inv, never()).addItem(any(ItemStack.class));
        verify(repo, never()).addPending(any(), any());
        assertEquals(0, pr.materials().get("minecraft:stone").deposited());
    }

    @Test void lostPendingIsLoggedWithItemBytes() {
        when(repo.addPending(any(), any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("bd")));
        Logger lg = Logger.getAnonymousLogger();
        List<String> logged = new ArrayList<>();
        lg.addHandler(new java.util.logging.Handler() {
            public void publish(java.util.logging.LogRecord r) {
                if (r.getLevel() == java.util.logging.Level.SEVERE) logged.add(r.getMessage());
            }
            public void flush() { }
            public void close() { }
        });
        lg.setUseParentHandlers(false);
        ReturnService s = new ReturnService(repo, materials, finished::add, id -> online, Runnable::run,
                (p, key) -> { }, x -> new byte[]{7}, b -> null, null, lg);
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(playerId);
        when(p.isOnline()).thenReturn(false);
        s.give(p, List.of(mock(ItemStack.class)));
        assertEquals(1, logged.size());
        assertTrue(logged.get(0).contains("PENDIENTE PERDIDO"));
        assertTrue(logged.get(0).contains(playerId.toString()));
        assertTrue(logged.get(0).contains("Bw=="));
    }

    // ---- claimAll ----

    @Test void claimTwiceGivesOnce() {
        ItemStack item = mock(ItemStack.class);
        service = service(s -> new byte[]{7}, b -> item);
        when(repo.pending(playerId)).thenReturn(CompletableFuture.completedFuture(
                List.of(new PendingRow(5, playerId, new byte[]{1}, 0))));
        when(repo.claim(5)).thenReturn(CompletableFuture.completedFuture(true), CompletableFuture.completedFuture(false));
        assertEquals(1, service.claimAll(player).join());
        assertEquals(0, service.claimAll(player).join());
        verify(inv, times(1)).addItem(item);
        verify(player, times(1)).saveData();
    }

    @Test void concurrentClaimAllIsBusy() {
        CompletableFuture<List<PendingRow>> slow = new CompletableFuture<>();
        when(repo.pending(playerId)).thenReturn(slow);
        CompletableFuture<Integer> first = service.claimAll(player);
        assertEquals(0, service.claimAll(player).join());
        verify(repo, times(1)).pending(playerId);
        slow.complete(List.of());
        assertEquals(0, first.join());
        // el bloqueo se soltó
        assertEquals(0, service.claimAll(player).join());
        verify(repo, times(2)).pending(playerId);
    }

    @Test void throwingClaimDoesNotStrandTheOthers() {
        ItemStack item = mock(ItemStack.class);
        service = service(s -> new byte[]{7}, b -> item);
        when(repo.pending(playerId)).thenReturn(CompletableFuture.completedFuture(
                List.of(new PendingRow(5, playerId, new byte[]{1}, 0), new PendingRow(6, playerId, new byte[]{2}, 0))));
        when(repo.claim(5)).thenThrow(new IllegalStateException("sync"));
        when(repo.claim(6)).thenReturn(CompletableFuture.completedFuture(true));
        assertEquals(1, service.claimAll(player).join());
        verify(inv, times(1)).addItem(item);
    }

    @Test void unreadablePendingStaysUnclaimed() {
        service = service(s -> new byte[]{7}, b -> { throw new IllegalArgumentException("mala"); });
        when(repo.pending(playerId)).thenReturn(CompletableFuture.completedFuture(
                List.of(new PendingRow(5, playerId, new byte[]{1}, 0))));
        assertEquals(0, service.claimAll(player).join());
        verify(repo, never()).claim(anyLong());
    }
}
