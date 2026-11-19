package com.sack.rpgroll.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sack.rpgroll.machines.area.AreaMachine;
import com.sack.rpgroll.machines.area.AreaMenu;
import com.sack.rpgroll.machines.area.AreaSettings;
import com.sack.rpgroll.machines.area.AreaStore;
import com.sack.rpgroll.machines.area.Crops;
import com.sack.rpgroll.machines.area.FishLoot;
import com.sack.rpgroll.machines.area.TreeScan;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

class AreaTest {

    @Test
    void settingsReadUpgradesAndLimits() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("max-per-player", 3);
        yaml.set("upgrades.speed.levels", List.of(java.util.Map.of("value", 0.1),
                java.util.Map.of("value", 0.2, "cost", java.util.Map.of("money", 500))));
        AreaSettings settings = AreaSettings.from(yaml, "fishers.yml", org.bukkit.Material.PRISMARINE_BRICKS,
                List.of("speed", "luck"), text -> { });
        assertEquals(3, settings.maxPerPlayer());
        assertEquals(1, settings.track("speed").max());
        assertEquals(0.2, settings.track("speed").value(1), 1e-9);
        assertEquals(500.0, settings.track("speed").step(1).cost().money(), 1e-9);
        // Una mejora que falta en el fichero: un solo nivel con valor 0, sin nada que comprar.
        assertEquals(0, settings.track("luck").max());
        assertEquals(0.0, settings.track("luck").value(0), 1e-9);
    }

    @Test
    void storeRoundTrip(@TempDir File dir) {
        File file = new File(dir, "machines.yml");
        AreaStore store = new AreaStore(file, Logger.getAnonymousLogger());
        UUID owner = UUID.randomUUID();
        AreaMachine machine = new AreaMachine("farm", "world", 10, 64, -5, owner, "Sack");
        machine.level("area", 2);
        machine.enabled(false);
        store.add(machine);
        store.save(false);

        AreaStore again = new AreaStore(file, Logger.getAnonymousLogger());
        again.load();
        AreaMachine read = again.at(machine.key()).orElseThrow();
        assertEquals("farm", read.kind());
        assertEquals(2, read.level("area"));
        assertFalse(read.enabled());
        assertEquals("Sack", read.ownerName());
        assertEquals(1, again.count(owner, "farm"));
        assertEquals(0, again.count(owner, "fisher"));
        assertTrue(read.buffer().isEmpty());
    }

    @Test
    void gaugeLevels() {
        assertEquals(0, AreaMenu.gauge(0));
        assertEquals(4, AreaMenu.gauge(0.5));
        assertEquals(8, AreaMenu.gauge(1));
        assertEquals(8, AreaMenu.gauge(3));
    }

    // ---------------------------------------------------------------- Pescadora

    @Test
    void fishCategoriesFollowLuck() {
        assertEquals(FishLoot.Category.JUNK, FishLoot.category(0.05, 0));
        assertEquals(FishLoot.Category.TREASURE, FishLoot.category(0.12, 0));
        assertEquals(FishLoot.Category.FISH, FishLoot.category(0.5, 0));
        // Con suerte IV ya no sale basura.
        assertEquals(FishLoot.Category.TREASURE, FishLoot.category(0.0, 4));
    }

    @Test
    void fishPickIsWeighted() {
        List<FishLoot.Entry> entries = List.of(new FishLoot.Entry(Material.COD, 60), new FishLoot.Entry(Material.SALMON, 25));
        assertEquals(Material.COD, FishLoot.pick(entries, 0.5));
        assertEquals(Material.SALMON, FishLoot.pick(entries, 0.75));
        assertEquals(List.of(new FishLoot.Entry(Material.COD, 60)), FishLoot.parse(List.of("COD:60", "nada:3", "SALMON:x"), t -> { }));
    }

    // ---------------------------------------------------------------- Granja

    @Test
    void cropSeeds() {
        assertEquals(Material.WHEAT_SEEDS, Crops.seed(Material.WHEAT));
        assertEquals(Material.CARROT, Crops.seed(Material.CARROTS));
        assertEquals(Material.NETHER_WART, Crops.seed(Material.NETHER_WART));
        assertEquals(Material.COCOA_BEANS, Crops.seed(Material.COCOA));
        assertEquals(null, Crops.seed(Material.PUMPKIN));
        assertFalse(Crops.replant(Material.PUMPKIN));
        assertTrue(Crops.replant(Material.SWEET_BERRY_BUSH));
        assertEquals(1, Crops.replantAge(Material.SWEET_BERRY_BUSH));
        assertEquals(0, Crops.replantAge(Material.WHEAT));
    }

    // ---------------------------------------------------------------- Leñadora

    private static boolean is(java.util.Set<List<Integer>> set, int[] p) {
        return set.contains(List.of(p[0], p[1], p[2]));
    }

    @Test
    void treeScanFollowsConnectedLogs() {
        java.util.Set<List<Integer>> logs = new java.util.HashSet<>();
        for (int y = 0; y < 5; y++) {
            logs.add(List.of(0, y, 0));
        }
        logs.add(List.of(1, 5, 1));    // rama en diagonal
        logs.add(List.of(4, 0, 0));    // otro árbol, sin tocar
        logs.add(List.of(0, 5, -1));   // fuera del área
        java.util.function.Predicate<int[]> inside = p -> p[2] >= 0;
        assertEquals(6, TreeScan.collect(new int[] {0, 0, 0}, p -> is(logs, p), inside, 128).size());
        assertEquals(3, TreeScan.collect(new int[] {0, 0, 0}, p -> is(logs, p), inside, 3).size());
    }

    @Test
    void saplingsForLogs() {
        assertEquals(Material.OAK_SAPLING, TreeScan.sapling(Material.OAK_LOG));
        assertEquals(Material.MANGROVE_PROPAGULE, TreeScan.sapling(Material.MANGROVE_LOG));
        assertEquals(Material.CRIMSON_FUNGUS, TreeScan.sapling(Material.CRIMSON_STEM));
        assertEquals(Material.CHERRY_SAPLING, TreeScan.sapling(Material.CHERRY_LOG));
        assertEquals(null, TreeScan.sapling(Material.STONE));
    }
}
