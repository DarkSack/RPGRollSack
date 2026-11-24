package com.sack.rpgroll.structures.config;

import com.sack.rpgroll.structures.config.WorkbenchSettings.Cost;
import com.sack.rpgroll.structures.config.WorkbenchSettings.Size;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class WorkbenchSettingsTest {

    private static YamlConfiguration yaml(String s) {
        try {
            YamlConfiguration y = new YamlConfiguration();
            y.loadFromString(s);
            return y;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void defaultsMatchTheSpec() {
        var s = WorkbenchSettings.from(yaml(""), w -> fail(w));
        assertEquals(new Size(16, 16, 16), s.tiers().get(WorkbenchTier.CONSTRUCTOR).maxSize());
        assertEquals(new Size(32, 32, 32), s.tiers().get(WorkbenchTier.INGENIERO).maxSize());
        assertEquals(new Size(64, 96, 64), s.tiers().get(WorkbenchTier.ARQUITECTO).maxSize());
        assertEquals(Material.CRAFTING_TABLE, s.tiers().get(WorkbenchTier.CONSTRUCTOR).fallbackBlock());
        assertEquals(Material.SMITHING_TABLE, s.tiers().get(WorkbenchTier.INGENIERO).fallbackBlock());
        assertEquals(Material.BEACON, s.tiers().get(WorkbenchTier.ARQUITECTO).fallbackBlock());
        assertEquals(List.of("PPP", "CTC", "L L"), s.recipeShape());
    }

    @Test
    void badSizeFallsBackAndWarns() {
        List<String> warns = new ArrayList<>();
        var s = WorkbenchSettings.from(yaml("tiers:\n  constructor:\n    max-size:\n      width: -3\n      height: 20"),
                warns::add);
        assertEquals(new Size(16, 20, 16), s.tiers().get(WorkbenchTier.CONSTRUCTOR).maxSize());
        assertEquals(1, warns.size());
        assertTrue(warns.get(0).contains("tiers.constructor.max-size"));
    }

    @Test
    void sizeFitsIsPerAxis() {
        assertTrue(new Size(16, 16, 16).fits(new Size(16, 5, 16)));
        assertFalse(new Size(16, 16, 16).fits(new Size(17, 1, 1)));
    }

    @Test
    void tierLevelsAndNext() {
        assertEquals(Optional.of(WorkbenchTier.INGENIERO), WorkbenchTier.CONSTRUCTOR.next());
        assertEquals(Optional.empty(), WorkbenchTier.ARQUITECTO.next());
        assertEquals(Optional.empty(), WorkbenchTier.ofLevel(4));
        assertEquals(3, WorkbenchTier.ARQUITECTO.level());
    }

    @Test
    void shippedWorkbenchYmlLoadsCleanWithTheFactoryCosts() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/workbench.yml")) {
            var y = YamlConfiguration.loadConfiguration(new InputStreamReader(in));
            var s = WorkbenchSettings.from(y, w -> fail(w));
            assertEquals(List.of(new Cost("minecraft:iron_ingot", 16), new Cost("minecraft:redstone", 8),
                    new Cost("minecraft:redstone_lamp", 4)), s.tiers().get(WorkbenchTier.INGENIERO).upgradeCost());
            assertEquals(3, s.tiers().get(WorkbenchTier.ARQUITECTO).upgradeCost().size());
            assertEquals("minecraft:crafting_table", s.recipeKeys().get('T'));
            assertEquals(Material.BEACON, s.tiers().get(WorkbenchTier.ARQUITECTO).fallbackBlock());
        }
    }
}
