package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.structures.config.WorkbenchSettings.Cost;
import com.sack.rpgroll.structures.workbench.UpgradeRules;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.sack.rpgroll.structures.workbench.WorkbenchTier.*;
import static org.junit.jupiter.api.Assertions.*;

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
}
