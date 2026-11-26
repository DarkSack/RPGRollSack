package com.sack.rpgroll.structures.deposit;

import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.structures.api.MaterialProvider;
import com.sack.rpgroll.structures.catalog.MaterialSpec;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.catalog.StructureRegistry;
import com.sack.rpgroll.structures.material.MaterialRegistry;
import com.sack.rpgroll.structures.project.Project;
import com.sack.rpgroll.structures.project.ProjectState;
import com.sack.rpgroll.structures.storage.MaterialRow;
import com.sack.rpgroll.structures.storage.Movement;
import com.sack.rpgroll.structures.storage.ProjectRecord;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DepositServiceTest {

    final Map<ItemStack, String> idOf = new HashMap<>();
    final ItemStack[] slots = new ItemStack[6];
    final ProjectRepository repo = mock(ProjectRepository.class);
    final ReturnService returns = mock(ReturnService.class);
    final StructureRegistry structures = mock(StructureRegistry.class);
    final Player player = mock(Player.class);
    final PlayerInventory inv = mock(PlayerInventory.class);
    final UUID playerId = UUID.randomUUID();
    final MaterialRegistry materials = new MaterialRegistry();
    List<MaterialSpec> specs = List.of(new MaterialSpec(List.of("minecraft:oak_planks"), 10, false),
            new MaterialSpec(List.of("minecraft:stone"), 4, false));
    final ItemStack armor = mock(ItemStack.class);
    Player online = player;
    DepositService service;
    Project project;

    @BeforeEach void setUp() {
        materials.register(new MaterialProvider() {
            public String type() { return "minecraft"; }
            public boolean knows(String id) { return true; }
            public boolean matches(String id, ItemStack s) { return ("minecraft:" + id).equals(idOf.get(s)); }
            public Optional<ItemStack> sample(String id) { return Optional.empty(); }
        });
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getInventory()).thenReturn(inv);
        when(player.isOnline()).thenReturn(true);
        when(inv.getStorageContents()).thenAnswer(i -> slots);
        // getContents incluye la armadura puesta: el servicio no debe mirarla.
        when(inv.getContents()).thenAnswer(i -> {
            ItemStack[] all = java.util.Arrays.copyOf(slots, slots.length + 1);
            all[slots.length] = armor;
            return all;
        });
        doAnswer(i -> { slots[i.getArgument(0)] = i.getArgument(1); return null; })
                .when(inv).setItem(anyInt(), any());
        StructureDefinition def = mock(StructureDefinition.class);
        when(def.materials()).thenAnswer(i -> specs);
        when(structures.get("house")).thenReturn(Optional.of(def));
        when(repo.saveProgress(any(), any(), anyInt(), anyInt(), anyInt(), any()))
                .thenReturn(CompletableFuture.completedFuture(true));
        service = new DepositService(repo, structures, materials, returns, id -> online, Runnable::run,
                Logger.getAnonymousLogger());
        project = project(10, 0, 4, 0);
    }

    Project project(int planksReq, int planksDep, int stoneReq, int stoneDep) {
        Map<String, MaterialRow> rows = new LinkedHashMap<>();
        rows.put("minecraft:oak_planks", new MaterialRow(planksReq, planksDep, 0));
        rows.put("minecraft:stone", new MaterialRow(stoneReq, stoneDep, 0));
        return new Project(new ProjectRecord(UUID.randomUUID(), UUID.randomUUID(), "house", playerId,
                ProjectState.WAITING_MATERIALS, "world", 0, 0, 0, Rotation.values()[0], 0, 0, 0, 100, null, 0, 0), rows);
    }

    /** Pila falsa con cantidad mutable; {@code id} es lo que el proveedor reconoce (null = renombrada: no casa). */
    ItemStack stack(String id, int amount) {
        ItemStack s = mock(ItemStack.class);
        AtomicInteger n = new AtomicInteger(amount);
        if (id != null) {
            idOf.put(s, id);
        }
        when(s.getAmount()).thenAnswer(i -> n.get());
        doAnswer(i -> { n.set(i.getArgument(0)); return null; }).when(s).setAmount(anyInt());
        when(s.isEmpty()).thenReturn(false);
        when(s.clone()).thenAnswer(i -> stack(id, n.get()));
        return s;
    }

    int total(String id) {
        int t = 0;
        for (ItemStack s : slots) {
            if (s != null && id.equals(idOf.get(s))) t += s.getAmount();
        }
        return t;
    }

    void applyAll(boolean ok) {
        when(repo.applyMovement(any())).thenReturn(CompletableFuture.completedFuture(ok));
    }

    @Test void depositTakesOnlyWhatIsMissing() {
        project = project(10, 6, 4, 0);
        slots[0] = stack("minecraft:oak_planks", 64);
        applyAll(true);
        DepositResult r = service.deposit(player, project, null, Integer.MAX_VALUE).join();
        assertEquals(DepositResult.OK, r);
        assertEquals(60, total("minecraft:oak_planks"));
        assertEquals(10, project.materials().get("minecraft:oak_planks").deposited());
        ArgumentCaptor<Movement> m = ArgumentCaptor.forClass(Movement.class);
        verify(repo).applyMovement(m.capture());
        assertEquals(4, m.getValue().amount());
        assertEquals(Movement.Kind.DEPOSIT, m.getValue().kind());
        assertFalse(project.busy());
        verify(player).saveData();
    }

    @Test void maxPerKeyCapsTheDeposit() {
        slots[0] = stack("minecraft:oak_planks", 64);
        applyAll(true);
        service.deposit(player, project, null, 1).join();
        assertEquals(63, total("minecraft:oak_planks"));
        assertEquals(1, project.materials().get("minecraft:oak_planks").deposited());
    }

    @Test void secondDepositWhileBusyIsDropped() {
        slots[0] = stack("minecraft:oak_planks", 64);
        CompletableFuture<Boolean> pending = new CompletableFuture<>();
        when(repo.applyMovement(any())).thenReturn(pending);
        CompletableFuture<DepositResult> first = service.deposit(player, project, null, Integer.MAX_VALUE);
        assertFalse(first.isDone());
        assertEquals(DepositResult.BUSY, service.deposit(player, project, null, Integer.MAX_VALUE).join());
        // Otro jugador sobre la misma obra: también BUSY por project.busy().
        Player other = mock(Player.class);
        when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        assertEquals(DepositResult.BUSY, service.deposit(other, project, null, 1).join());
        pending.complete(true);
        assertEquals(DepositResult.OK, first.join());
        assertFalse(project.busy());
    }

    @Test void failedWriteGivesItemsBack() {
        slots[0] = stack("minecraft:oak_planks", 64);
        applyAll(false);
        DepositResult r = service.deposit(player, project, null, Integer.MAX_VALUE).join();
        assertEquals(DepositResult.ERROR, r);
        assertEquals(0, project.materials().get("minecraft:oak_planks").deposited());
        @SuppressWarnings("unchecked") ArgumentCaptor<List<ItemStack>> given = ArgumentCaptor.forClass(List.class);
        verify(returns).give(eq(player), given.capture());
        assertEquals(10, given.getValue().stream().mapToInt(ItemStack::getAmount).sum());
        verify(player, never()).saveData();
        assertFalse(project.busy());
        // El jugador puede volver a intentarlo: el bloqueo se soltó.
        applyAll(true);
        assertEquals(DepositResult.OK, service.deposit(player, project, null, Integer.MAX_VALUE).join());
    }

    @Test void partialFailureOnlyRevertsTheFailedOnes() {
        slots[0] = stack("minecraft:oak_planks", 64);
        slots[1] = stack("minecraft:stone", 64);
        when(repo.applyMovement(any())).thenAnswer(i -> CompletableFuture.completedFuture(
                "minecraft:oak_planks".equals(((Movement) i.getArgument(0)).materialKey())));
        assertEquals(DepositResult.ERROR, service.deposit(player, project, null, Integer.MAX_VALUE).join());
        assertEquals(10, project.materials().get("minecraft:oak_planks").deposited());
        assertEquals(0, project.materials().get("minecraft:stone").deposited());
        verify(returns, times(1)).give(eq(player), any());
        verify(player).saveData();
        assertEquals(ProjectState.WAITING_MATERIALS, project.state());
    }

    @Test void exceptionalWriteCountsAsFailure() {
        slots[0] = stack("minecraft:oak_planks", 64);
        when(repo.applyMovement(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("bd")));
        assertEquals(DepositResult.ERROR, service.deposit(player, project, null, Integer.MAX_VALUE).join());
        assertEquals(0, project.materials().get("minecraft:oak_planks").deposited());
        assertFalse(project.busy());
    }

    @Test void reachingHundredPercentMakesReady() {
        project = project(10, 0, 4, 4);
        slots[0] = stack("minecraft:oak_planks", 10);
        applyAll(true);
        assertEquals(DepositResult.OK, service.deposit(player, project, null, Integer.MAX_VALUE).join());
        assertEquals(ProjectState.READY, project.state());
        verify(repo).saveProgress(eq(project.id()), eq(ProjectState.READY), eq(0), eq(0), eq(0), any());
        assertNull(slots[0]);
    }

    @Test void wrongStateIsRejected() {
        project.setState(ProjectState.READY);
        slots[0] = stack("minecraft:oak_planks", 64);
        assertEquals(DepositResult.WRONG_STATE, service.deposit(player, project, null, 1).join());
        assertEquals(64, total("minecraft:oak_planks"));
        verify(repo, never()).applyMovement(any());
        assertFalse(project.busy());
    }

    @Test void renamedItemIsNotTaken() {
        slots[0] = stack(null, 64);
        assertEquals(DepositResult.NOTHING_TO_TAKE, service.deposit(player, project, null, 1).join());
        assertEquals(64, slots[0].getAmount());
        verify(repo, never()).applyMovement(any());
        assertFalse(project.busy());
    }

    @Test void onlyKeyLimitsToThatMaterial() {
        slots[0] = stack("minecraft:oak_planks", 64);
        slots[1] = stack("minecraft:stone", 64);
        applyAll(true);
        service.deposit(player, project, "minecraft:stone", Integer.MAX_VALUE).join();
        assertEquals(64, total("minecraft:oak_planks"));
        assertEquals(60, total("minecraft:stone"));
    }

    @Test void overlappingSpecsRecordOnlyWhatWasRemoved() {
        specs = List.of(new MaterialSpec(List.of("minecraft:oak_planks", "minecraft:birch_planks"), 10, false),
                new MaterialSpec(List.of("minecraft:birch_planks"), 4, false));
        Map<String, MaterialRow> rows = new LinkedHashMap<>();
        rows.put("minecraft:oak_planks", new MaterialRow(10, 0, 0));
        rows.put("minecraft:birch_planks", new MaterialRow(4, 0, 0));
        project.setMaterials(rows);
        slots[0] = stack("minecraft:birch_planks", 10);
        applyAll(true);
        assertEquals(DepositResult.OK, service.deposit(player, project, null, Integer.MAX_VALUE).join());
        ArgumentCaptor<Movement> m = ArgumentCaptor.forClass(Movement.class);
        verify(repo, times(1)).applyMovement(m.capture());
        assertEquals("minecraft:oak_planks", m.getValue().materialKey());
        assertEquals(10, m.getValue().amount());
        assertEquals(10, project.materials().get("minecraft:oak_planks").deposited());
        assertEquals(0, project.materials().get("minecraft:birch_planks").deposited());
    }

    @Test void exceptionWhileRemovingGivesBackWhatWasTaken() {
        slots[0] = stack("minecraft:oak_planks", 64);
        slots[1] = stack("minecraft:stone", 64);
        doThrow(new IllegalStateException("setItem")).when(inv).setItem(eq(1), any());
        assertEquals(DepositResult.ERROR, service.deposit(player, project, null, Integer.MAX_VALUE).join());
        @SuppressWarnings("unchecked") ArgumentCaptor<List<ItemStack>> given = ArgumentCaptor.forClass(List.class);
        verify(returns).give(eq(player), given.capture());
        assertEquals(10, given.getValue().stream().mapToInt(ItemStack::getAmount).sum());
        verify(repo, never()).applyMovement(any());
        assertEquals(0, project.materials().get("minecraft:oak_planks").deposited());
        assertFalse(project.busy());
        // El bloqueo del jugador también se soltó.
        doAnswer(i -> null).when(inv).setItem(anyInt(), any());
        applyAll(true);
        assertEquals(DepositResult.OK, service.deposit(player, project, null, Integer.MAX_VALUE).join());
    }

    @Test void nothingToTakeButCompleteBecomesReady() {
        project = project(10, 10, 4, 4);
        assertEquals(DepositResult.OK, service.deposit(player, project, null, Integer.MAX_VALUE).join());
        assertEquals(ProjectState.READY, project.state());
        verify(repo).saveProgress(eq(project.id()), eq(ProjectState.READY), eq(0), eq(0), eq(0), any());
        verify(repo, never()).applyMovement(any());
    }

    @Test void partialFailureStillBecomesReadyIfComplete() {
        specs = List.of(new MaterialSpec(List.of("minecraft:oak_planks"), 10, false),
                new MaterialSpec(List.of("minecraft:stone"), 4, true));
        slots[0] = stack("minecraft:oak_planks", 10);
        slots[1] = stack("minecraft:stone", 4);
        when(repo.applyMovement(any())).thenAnswer(i -> CompletableFuture.completedFuture(
                "minecraft:oak_planks".equals(((Movement) i.getArgument(0)).materialKey())));
        assertEquals(DepositResult.ERROR, service.deposit(player, project, null, Integer.MAX_VALUE).join());
        assertEquals(ProjectState.READY, project.state());
    }

    @Test void giveThrowingStillRevertsAllMemory() {
        slots[0] = stack("minecraft:oak_planks", 64);
        slots[1] = stack("minecraft:stone", 64);
        applyAll(false);
        doThrow(new IllegalStateException("give")).doNothing().when(returns).give(any(), any());
        assertEquals(DepositResult.ERROR, service.deposit(player, project, null, Integer.MAX_VALUE).join());
        assertEquals(0, project.materials().get("minecraft:oak_planks").deposited());
        assertEquals(0, project.materials().get("minecraft:stone").deposited());
        verify(returns, times(2)).give(eq(player), any());
        assertFalse(project.busy());
    }

    @Test void equippedArmorIsNotTaken() {
        idOf.put(armor, "minecraft:oak_planks");
        when(armor.getAmount()).thenReturn(1);
        when(armor.isEmpty()).thenReturn(false);
        assertEquals(DepositResult.NOTHING_TO_TAKE, service.deposit(player, project, null, 1).join());
    }

    @Test void failingSetItemOnPartialRemovalLeavesTheStackUntouched() {
        ItemStack original = stack("minecraft:oak_planks", 64);
        slots[0] = original;
        doThrow(new IllegalStateException("setItem")).when(inv).setItem(eq(0), any());
        assertEquals(DepositResult.ERROR, service.deposit(player, project, null, Integer.MAX_VALUE).join());
        assertSame(original, slots[0]);
        assertEquals(64, original.getAmount());
        verify(returns, never()).give(any(), any());
        assertFalse(project.busy());
    }

    @Test void partialRemovalLeavesTheRestInTheSlot() {
        slots[0] = stack("minecraft:oak_planks", 64);
        applyAll(true);
        service.deposit(player, project, null, Integer.MAX_VALUE).join();
        assertEquals(54, slots[0].getAmount());
    }

    @Test void refundGoesToTheReconnectedPlayer() {
        slots[0] = stack("minecraft:oak_planks", 64);
        applyAll(false);
        Player again = mock(Player.class);
        when(again.isOnline()).thenReturn(true);
        online = again;
        service.deposit(player, project, null, Integer.MAX_VALUE).join();
        verify(returns).give(eq(again), any());
        verify(returns, never()).give(eq(player), any());
    }

    @Test void refundGoesToPendingWhenPlayerIsGone() {
        slots[0] = stack("minecraft:oak_planks", 64);
        applyAll(false);
        online = null;
        service.deposit(player, project, null, Integer.MAX_VALUE).join();
        verify(returns).givePending(eq(playerId), any());
        verify(returns, never()).give(any(), any());
    }

    @Test void saveDataRunsOnTheReconnectedPlayer() {
        slots[0] = stack("minecraft:oak_planks", 64);
        applyAll(true);
        Player again = mock(Player.class);
        when(again.isOnline()).thenReturn(true);
        online = again;
        service.deposit(player, project, null, Integer.MAX_VALUE).join();
        verify(again).saveData();
        verify(player, never()).saveData();
    }
}
