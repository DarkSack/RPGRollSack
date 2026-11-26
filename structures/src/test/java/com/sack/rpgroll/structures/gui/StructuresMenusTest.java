package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.structures.build.ConstructionEngine.StartResult;
import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import com.sack.rpgroll.structures.workbench.WorkbenchService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.sack.rpgroll.structures.config.WorkbenchSettings.Cost;
import com.sack.rpgroll.structures.deposit.DepositResult;
import com.sack.rpgroll.structures.workbench.UpgradeRules;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.sack.rpgroll.structures.workbench.WorkbenchTier.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

class StructuresMenusTest {
    private final List<Cost> costs = List.of(new Cost("minecraft:iron_ingot", 16), new Cost("minecraft:redstone", 8));

    @Test void backgroundFamilyFollowsTier() {
        assertEquals("centro_taller", StructuresMenus.background("centro", CONSTRUCTOR));
        assertEquals("centro_ingenio", StructuresMenus.background("centro", INGENIERO));
        assertEquals("centro_arcano", StructuresMenus.background("centro", ARQUITECTO));
        assertEquals("confirmar_taller", StructuresMenus.background("confirmar", CONSTRUCTOR));
    }

    @Test void upgradeMenuMarksMissingRed() {
        var missing = new UpgradeRules.MissingItems(Map.of("minecraft:iron_ingot", 6)).missing();
        var lines = UpgradeMenu.lines(costs, missing);
        assertEquals(UpgradeMenu.KEY_MISSING, lines.get(0).key());
        assertEquals(6, lines.get(0).missing());
        assertEquals(UpgradeMenu.KEY_HAVE, lines.get(1).key());
    }

    @Test void shortfallOnlyListsWhatIsMissing() {
        assertEquals(Map.of("minecraft:iron_ingot", 6),
                UpgradeMenu.shortfall(costs, Map.of("minecraft:iron_ingot", 10, "minecraft:redstone", 9)));
    }

    @Test void maxTierMessageOnlyOnRealMaxTier() {
        assertEquals(Optional.empty(), UpgradeMenu.resultKey(new UpgradeRules.MaxTier(), CONSTRUCTOR));
        assertEquals(Optional.empty(), UpgradeMenu.resultKey(new UpgradeRules.MaxTier(), INGENIERO));
        assertEquals(Optional.of("upgrade.max-tier"), UpgradeMenu.resultKey(new UpgradeRules.MaxTier(), ARQUITECTO));
        assertEquals(Optional.of("upgrade.done"), UpgradeMenu.resultKey(new UpgradeRules.Ok(), CONSTRUCTOR));
        assertEquals(Optional.of("upgrade.no-money"), UpgradeMenu.resultKey(new UpgradeRules.NotEnoughMoney(1), CONSTRUCTOR));
        assertEquals(Optional.of("upgrade.no-permission"), UpgradeMenu.resultKey(new UpgradeRules.NoPermission(), CONSTRUCTOR));
        assertEquals(Optional.of("upgrade.result-missing"),
                UpgradeMenu.resultKey(new UpgradeRules.MissingItems(Map.of()), CONSTRUCTOR));
    }

    @Test void accessPaging() {
        assertEquals(1, AccessMenu.pageCount(0));
        assertEquals(1, AccessMenu.pageCount(45));
        assertEquals(2, AccessMenu.pageCount(46));
        List<Integer> all = java.util.stream.IntStream.range(0, 50).boxed().toList();
        assertEquals(45, AccessMenu.pageOf(all, 0).size());
        assertEquals(5, AccessMenu.pageOf(all, 1).size());
        assertEquals(5, AccessMenu.pageOf(all, 9).size());
    }

    @Test void genericPaging() {
        assertEquals(1, StructuresMenus.pageCount(0, 28));
        assertEquals(1, StructuresMenus.pageCount(28, 28));
        assertEquals(2, StructuresMenus.pageCount(29, 28));
        List<Integer> all = java.util.stream.IntStream.range(0, 30).boxed().toList();
        assertEquals(28, StructuresMenus.pageOf(all, 0, 28).size());
        assertEquals(List.of(28, 29), StructuresMenus.pageOf(all, 1, 28));
        assertEquals(List.of(28, 29), StructuresMenus.pageOf(all, 7, 28), "la página se acota");
        assertEquals(List.of(0), StructuresMenus.pageOf(List.of(0), -3, 28));
    }

    @Test void timeEstimateRoundsCyclesUp() {
        // ceil(100 / 30) = 4 ciclos de 10 ticks = 40 ticks = 2 s
        assertEquals(2, StructuresMenus.estimateSeconds(100, 30, 10));
        assertEquals(0, StructuresMenus.estimateSeconds(0, 30, 10));
        // ceil(1 / 64) = 1 ciclo de 30 ticks = 1,5 s → 2 (no se promete menos de lo que tarda)
        assertEquals(2, StructuresMenus.estimateSeconds(1, 64, 30));
        assertEquals(5, StructuresMenus.estimateSeconds(5, 0, 20), "0 bloques por ciclo cuenta como 1");
    }

    @Test void durationIsShortAndReadable() {
        assertEquals("0s", StructuresMenus.duration(0));
        assertEquals("45s", StructuresMenus.duration(45));
        assertEquals("3m 05s", StructuresMenus.duration(185));
        assertEquals("2h 03m", StructuresMenus.duration(2 * 3600 + 3 * 60 + 59));
    }

    @Test void percentIsFloorAndSafe() {
        assertEquals(0, StructuresMenus.percent(0, 10));
        assertEquals(33, StructuresMenus.percent(1, 3));
        assertEquals(100, StructuresMenus.percent(10, 10));
        assertEquals(100, StructuresMenus.percent(0, 0));
        assertEquals(100, StructuresMenus.percent(12, 10));
    }

    @Test void materialColourFollowsWhatIsDeposited() {
        assertEquals(MaterialsMenu.Fill.FULL, MaterialsMenu.fill(256, 256));
        assertEquals(MaterialsMenu.Fill.FULL, MaterialsMenu.fill(300, 256));
        assertEquals(MaterialsMenu.Fill.PARTIAL, MaterialsMenu.fill(128, 256));
        assertEquals(MaterialsMenu.Fill.NONE, MaterialsMenu.fill(0, 256));
        assertEquals("menu.materials.full", MaterialsMenu.Fill.FULL.key());
        assertEquals("menu.materials.partial", MaterialsMenu.Fill.PARTIAL.key());
        assertEquals("menu.materials.none", MaterialsMenu.Fill.NONE.key());
    }

    @Test void everyResultHasItsMessage() {
        assertEquals("deposit.done", MaterialsMenu.depositKey(DepositResult.OK));
        assertEquals("deposit.nothing", MaterialsMenu.depositKey(DepositResult.NOTHING_TO_TAKE));
        assertEquals("deposit.busy", MaterialsMenu.depositKey(DepositResult.BUSY));
        assertEquals("deposit.wrong-state", MaterialsMenu.depositKey(DepositResult.WRONG_STATE));
        assertEquals("deposit.error", MaterialsMenu.depositKey(DepositResult.ERROR));
        assertEquals("build.started", MaterialsMenu.startKey(StartResult.STARTED));
        assertEquals("build.not-ready", MaterialsMenu.startKey(StartResult.NOT_READY));
        assertEquals("build.limit-server", MaterialsMenu.startKey(StartResult.LIMIT_SERVER));
        assertEquals("build.limit-world", MaterialsMenu.startKey(StartResult.LIMIT_WORLD));
        assertEquals("build.limit-player", MaterialsMenu.startKey(StartResult.LIMIT_PLAYER));
        assertEquals("build.no-permission", MaterialsMenu.startKey(StartResult.NO_PERMISSION));
        assertEquals("build.module-denied", MaterialsMenu.startKey(StartResult.MODULE_DENIED));
    }

    // ---- acceso a la mesa en cada acción ----

    final Player player = mock(Player.class);
    final WorkbenchService service = mock(WorkbenchService.class);
    final LangManager lang = mock(LangManager.class);
    final WorkbenchRecord bench = mock(WorkbenchRecord.class);
    final UUID benchId = UUID.randomUUID();
    final Inventory menu = mock(Inventory.class);
    final List<Runnable> mainQueue = new ArrayList<>();
    final int[] ran = {0};

    void openMenu(Inventory top) {
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(top);
        when(player.getOpenInventory()).thenReturn(view);
    }

    void access(CompletableFuture<Boolean> canUse) {
        when(service.byId(benchId)).thenReturn(Optional.of(bench));
        when(service.canUse(player, bench)).thenReturn(canUse);
        openMenu(menu);
        StructuresMenus.withAccess(player, service, lang, mainQueue::add, benchId, menu, () -> ran[0]++);
        assertEquals(0, ran[0], "nunca antes de volver al hilo principal");
        mainQueue.forEach(Runnable::run);
    }

    @Test void actionRunsWithAccessOnTheMainThread() {
        access(CompletableFuture.completedFuture(true));
        assertEquals(1, ran[0]);
        verify(player, never()).closeInventory();
    }

    @Test void lostAccessClosesAndSaysSo() {
        access(CompletableFuture.completedFuture(false));
        assertEquals(0, ran[0]);
        verify(lang).send(player, "workbench.use.denied");
        verify(player).closeInventory();
    }

    @Test void failedAccessCheckCountsAsDenied() {
        access(CompletableFuture.failedFuture(new RuntimeException("bd")));
        assertEquals(0, ran[0]);
        verify(player).closeInventory();
    }

    @Test void goneBenchClosesWithoutAsking() {
        when(service.byId(benchId)).thenReturn(Optional.empty());
        StructuresMenus.withAccess(player, service, lang, mainQueue::add, benchId, menu, () -> ran[0]++);
        mainQueue.forEach(Runnable::run);
        assertEquals(0, ran[0]);
        verify(service, never()).canUse(any(), any());
        verify(player).closeInventory();
    }

    @Test void actionIsDroppedIfThatMenuIsNoLongerOpen() {
        when(service.byId(benchId)).thenReturn(Optional.of(bench));
        when(service.canUse(player, bench)).thenReturn(CompletableFuture.completedFuture(true));
        openMenu(mock(Inventory.class)); // ya mira otro menú
        StructuresMenus.withAccess(player, service, lang, mainQueue::add, benchId, menu, () -> ran[0]++);
        mainQueue.forEach(Runnable::run);
        assertEquals(0, ran[0]);
        verify(player, never()).closeInventory();
    }

    @Test void isOpenComparesTheTopInventory() {
        openMenu(menu);
        assertTrue(StructuresMenus.isOpen(player, menu));
        assertFalse(StructuresMenus.isOpen(player, mock(Inventory.class)));
        assertTrue(StructuresMenus.isOpen(player, null), "sin menú que esperar, siempre");
    }

    @Test void withdrawAndCancelForProjectOwnerBenchOwnerOrBypass() {
        UUID me = UUID.randomUUID(), other = UUID.randomUUID(), third = UUID.randomUUID();
        assertTrue(ProjectMenu.mayManage(me, me, other, false), "dueño de la obra");
        assertTrue(ProjectMenu.mayManage(me, other, me, false), "dueño de la mesa");
        assertTrue(ProjectMenu.mayManage(me, other, third, true), "bypass");
        assertFalse(ProjectMenu.mayManage(me, other, third, false), "solo de confianza");
        assertFalse(ProjectMenu.mayManage(me, other, null, false), "mesa desconocida");
    }

    @Test void structureLockReasons() {
        assertNull(ListMenu.lockReason(CONSTRUCTOR, INGENIERO, true));
        assertNull(ListMenu.lockReason(ARQUITECTO, null, true), "sin mesa no hay nivel que comparar");
        assertEquals("catalog.locked-tier", ListMenu.lockReason(ARQUITECTO, INGENIERO, true));
        assertEquals("catalog.locked-permission", ListMenu.lockReason(CONSTRUCTOR, ARQUITECTO, false));
    }

    @Test void confirmChecksTheCategoryPermissionToo() {
        var def = new com.sack.rpgroll.structures.catalog.StructureDefinition("casa", Map.of(), "casas", CONSTRUCTOR,
                null, null, null, List.of(), 0, false, null, List.of(), 1, 1, null, "rpgrollstructures.build.casa");
        Player p = mock(Player.class);
        when(p.hasPermission(anyString())).thenReturn(true);
        assertNull(PreviewMenu.confirmReason(p, def, CONSTRUCTOR, true));
        when(p.isPermissionSet("rpgrollstructures.category.casas")).thenReturn(true);
        when(p.hasPermission("rpgrollstructures.category.casas")).thenReturn(false);
        assertEquals("catalog.locked-category", PreviewMenu.confirmReason(p, def, CONSTRUCTOR, true));
    }

    @Test void moneyStructureWithoutVaultShowsReason() {
        var paid = new com.sack.rpgroll.structures.catalog.StructureDefinition("casa", Map.of(), "casas", CONSTRUCTOR,
                null, null, null, List.of(), 10, false, null, List.of(), 1, 1, null, "rpgrollstructures.build.casa");
        var free = new com.sack.rpgroll.structures.catalog.StructureDefinition("casa", Map.of(), "casas", CONSTRUCTOR,
                null, null, null, List.of(), 0, false, null, List.of(), 1, 1, null, "rpgrollstructures.build.casa");
        Player p = mock(Player.class);
        when(p.hasPermission(anyString())).thenReturn(true);
        assertEquals("structure.needs-vault", PreviewMenu.confirmReason(p, paid, CONSTRUCTOR, false));
        assertNull(PreviewMenu.confirmReason(p, paid, CONSTRUCTOR, true));
        assertNull(PreviewMenu.confirmReason(p, free, CONSTRUCTOR, false));
        assertEquals("structure.needs-vault", DetailMenu.reason(p, paid, CONSTRUCTOR, false));
        assertNull(DetailMenu.reason(p, paid, CONSTRUCTOR, true));
        assertNull(DetailMenu.reason(p, free, CONSTRUCTOR, false));
        // El nivel o el permiso se dicen antes que Vault.
        assertEquals("catalog.locked-tier", DetailMenu.reason(p, new com.sack.rpgroll.structures.catalog
                .StructureDefinition("casa", Map.of(), "casas", ARQUITECTO, null, null, null, List.of(), 10, false,
                null, List.of(), 1, 1, null, "x"), CONSTRUCTOR, false));
    }
}
