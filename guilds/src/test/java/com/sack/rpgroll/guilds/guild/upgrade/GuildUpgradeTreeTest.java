package com.sack.rpgroll.guilds.guild.upgrade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuildUpgradeTreeTest {

    @Test
    void newBranchStartsAtLevelZero() {
        GuildUpgradeTree tree = new GuildUpgradeTree();
        assertEquals(0, tree.level(GuildUpgradeBranch.BANK));
    }

    @Test
    void upgradeIncrementsLevelUntilMax() {
        GuildUpgradeTree tree = new GuildUpgradeTree();

        for (int i = 0; i < GuildUpgradeBranch.MAX_LEVEL; i++) {
            assertTrue(tree.upgrade(GuildUpgradeBranch.BANK));
        }

        assertEquals(GuildUpgradeBranch.MAX_LEVEL, tree.level(GuildUpgradeBranch.BANK));
        assertFalse(tree.upgrade(GuildUpgradeBranch.BANK));
    }

    @Test
    void restoreClampsToValidRange() {
        GuildUpgradeTree tree = new GuildUpgradeTree();

        tree.restore(GuildUpgradeBranch.BANK, -5);
        assertEquals(0, tree.level(GuildUpgradeBranch.BANK));

        tree.restore(GuildUpgradeBranch.BANK, 999);
        assertEquals(GuildUpgradeBranch.MAX_LEVEL, tree.level(GuildUpgradeBranch.BANK));
    }

    @Test
    void vaultSlotsGrowWithBankLevelButCapAtFortyFive() {
        GuildUpgradeTree tree = new GuildUpgradeTree();
        assertEquals(9, tree.vaultSlots());

        tree.restore(GuildUpgradeBranch.BANK, GuildUpgradeBranch.MAX_LEVEL);
        assertEquals(45, tree.vaultSlots());
    }

    @Test
    void maxActiveBuffsStartsAtOneAndGrowsWithBuffsBranch() {
        GuildUpgradeTree tree = new GuildUpgradeTree();
        assertEquals(1, tree.maxActiveBuffs());

        tree.upgrade(GuildUpgradeBranch.BUFFS);
        assertEquals(2, tree.maxActiveBuffs());
    }

    @Test
    void economyBonusPercentScalesWithEconomyLevel() {
        GuildUpgradeTree tree = new GuildUpgradeTree();
        tree.restore(GuildUpgradeBranch.ECONOMY, 4);

        assertEquals(0.20, tree.economyBonusPercent(), 0.0001);
    }

    @Test
    void upgradeCostGrowsWithCurrentLevel() {
        assertEquals(1000.0, GuildUpgradeBranch.BANK.upgradeCost(0));
        assertTrue(GuildUpgradeBranch.BANK.upgradeCost(5) > GuildUpgradeBranch.BANK.upgradeCost(0));
    }
}
