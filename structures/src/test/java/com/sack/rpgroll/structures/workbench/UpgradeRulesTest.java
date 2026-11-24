package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.structures.config.WorkbenchSettings.Cost;
import com.sack.rpgroll.structures.config.WorkbenchSettings.Size;
import com.sack.rpgroll.structures.config.WorkbenchSettings.TierSettings;
import com.sack.rpgroll.structures.workbench.UpgradeRules.*;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.sack.rpgroll.structures.workbench.WorkbenchTier.*;
import static org.junit.jupiter.api.Assertions.*;

class UpgradeRulesTest {
    private final TierSettings ingeniero = new TierSettings(new Size(32, 32, 32),
            List.of(new Cost("minecraft:iron_ingot", 16), new Cost("minecraft:redstone", 8),
                    new Cost("minecraft:redstone_lamp", 4)), 100,
            "", Material.SMITHING_TABLE);

    @Test void upgradeListsExactlyWhatIsMissing() {
        var r = UpgradeRules.check(CONSTRUCTOR, ingeniero, Map.of("minecraft:iron_ingot", 10, "minecraft:redstone", 8), 500, true);
        assertEquals(new MissingItems(Map.of("minecraft:iron_ingot", 6, "minecraft:redstone_lamp", 4)), r);
    }

    @Test void surplusItemIsNotListedEither() {
        var r = UpgradeRules.check(CONSTRUCTOR, ingeniero, Map.of("minecraft:iron_ingot", 20, "minecraft:redstone", 8), 500, true);
        assertEquals(new MissingItems(Map.of("minecraft:redstone_lamp", 4)), r);
    }

    @Test void arquitectoIsMax() {
        assertInstanceOf(MaxTier.class, UpgradeRules.check(ARQUITECTO, null, Map.of(), 0, true));
    }

    @Test void noPermissionBeforeItemsAndMoneyBeforeOk() {
        assertInstanceOf(NoPermission.class, UpgradeRules.check(CONSTRUCTOR, ingeniero, Map.of(), 0, false));
        var has = Map.of("minecraft:iron_ingot", 16, "minecraft:redstone", 8, "minecraft:redstone_lamp", 4);
        assertEquals(new NotEnoughMoney(40), UpgradeRules.check(CONSTRUCTOR, ingeniero, has, 60, true));
        assertInstanceOf(Ok.class, UpgradeRules.check(CONSTRUCTOR, ingeniero, has, 100, true));
    }
}
