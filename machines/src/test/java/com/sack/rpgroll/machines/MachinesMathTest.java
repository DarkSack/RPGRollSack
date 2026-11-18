package com.sack.rpgroll.machines;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sack.rpgroll.machines.core.Cost;
import com.sack.rpgroll.machines.core.Ui;
import com.sack.rpgroll.machines.furnace.FurnaceMath;
import com.sack.rpgroll.machines.quarry.Quarry;
import com.sack.rpgroll.machines.quarry.QuarrySettings;
import com.sack.rpgroll.machines.quarry.QuarryUpgradesMenu;
import com.sack.rpgroll.machines.spawner.SpawnerData;
import com.sack.rpgroll.machines.spawner.SpawnerSettings;
import com.sack.rpgroll.machines.spawner.SpawnerSettings.Level;
import com.sack.rpgroll.machines.spawner.SpawnerSettings.Track;
import com.sack.rpgroll.machines.spawner.SpawnerSettings.Upgrade;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

class MachinesMathTest {

    @Test
    void furnaceCooksFasterButFuelYieldsTheSamePerItemUnlessFuelUpgraded() {
        assertEquals(100, FurnaceMath.cookTime(200, 2.0));
        assertEquals(1, FurnaceMath.cookTime(200, 1000.0));
        // Carbón: 1600 ticks. A ×2 de velocidad dura la mitad (mismos ítems por carbón)...
        assertEquals(800, FurnaceMath.burnTime(1600, 2.0, 1.0));
        // ...y con fuel 1.5, un 50 % más.
        assertEquals(1200, FurnaceMath.burnTime(1600, 2.0, 1.5));
        assertEquals(0, FurnaceMath.burnTime(0, 2.0, 1.5));
    }

    @Test
    void doubleResultOnlyWhenItFits() {
        assertEquals(2, FurnaceMath.result(1, 0, 64, 0.5, 0.1));
        assertEquals(1, FurnaceMath.result(1, 0, 64, 0.5, 0.9));
        assertEquals(1, FurnaceMath.result(1, 63, 64, 1.0, 0.0));
        assertEquals(2, FurnaceMath.result(1, 62, 64, 1.0, 0.0));
        assertEquals(1, FurnaceMath.result(1, 0, 64, 0.0, 0.0));
    }

    @Test
    void quarryAreaIsCenteredAndCursorWalksLayerByLayer() {
        // Lado 16 centrado en x=100: de 92 a 107.
        assertEquals(92, Quarry.min(100, 16));
        assertArrayEquals(new int[] {92, -8}, Quarry.column(100, 0, 16, 0));
        assertArrayEquals(new int[] {107, 7}, Quarry.column(100, 0, 16, 255));
        assertArrayEquals(new int[] {64, 1}, Quarry.advance(64, 0, 16, -64));
        assertArrayEquals(new int[] {63, 0}, Quarry.advance(64, 255, 16, -64));
        assertNull(Quarry.advance(-64, 255, 16, -64));
    }

    @Test
    void quarryProgress() {
        Quarry quarry = new Quarry("w", 0, 1, 0, UUID.randomUUID(), "x");
        // Desde y=0 hasta y=-1 (minHeight -1): dos capas de 2×2.
        assertEquals(0.0, quarry.progress(2, -1));
        quarry.cursor(-1, 0);
        assertEquals(0.5, quarry.progress(2, -1));
        quarry.finished(true);
        assertEquals(1.0, quarry.progress(2, -1));
    }

    @Test
    void stackedSpawnerMultipliesWavesButNotDelays() {
        EnumMap<Upgrade, Track> tracks = new EnumMap<>(Upgrade.class);
        tracks.put(Upgrade.SPEED, new Track(Material.SUGAR, List.of(new Level(200, 800, 4, 6, 16, Cost.FREE),
                new Level(100, 400, 4, 6, 16, Cost.FREE))));
        tracks.put(Upgrade.COUNT, new Track(Material.SPAWNER, List.of(new Level(200, 800, 4, 6, 16, Cost.FREE),
                new Level(200, 800, 5, 8, 16, Cost.FREE))));
        tracks.put(Upgrade.RANGE, new Track(Material.ENDER_EYE, List.of(new Level(200, 800, 4, 6, 16, Cost.FREE))));
        SpawnerSettings settings = new SpawnerSettings(true, Set.of(), true, true, true, 8, true, 1.35,
                true, "ns:generador_{tier}", 1f, tracks);

        SpawnerData.Applied applied = new SpawnerData(1, 1, 5, 3).applied(settings);
        assertEquals(100, applied.minDelay());
        assertEquals(400, applied.maxDelay());
        assertEquals(15, applied.spawnCount());
        assertEquals(24, applied.maxNearby());
        // Un nivel que ya no existe en el fichero se queda en el último.
        assertEquals(16, applied.range());

        // Dos mejoras posibles en total (velocidad y cantidad, una cada una).
        assertEquals(0, settings.frameTier(SpawnerData.NONE));
        assertEquals(1, settings.frameTier(SpawnerData.NONE.withStack(2)));
        assertEquals(2, settings.frameTier(new SpawnerData(1, 0, 0, 1)));
        assertEquals(3, settings.frameTier(new SpawnerData(1, 1, 0, 1)));
        assertEquals("ns:generador_3", settings.frameModel(new SpawnerData(1, 1, 0, 1)).toString());
    }

    @Test
    void gaugeLevels() {
        assertEquals(0, Quarry.gaugeLevel(0.0));
        assertEquals(0, Quarry.gaugeLevel(0.12));
        assertEquals(4, Quarry.gaugeLevel(0.5));
        assertEquals(8, Quarry.gaugeLevel(1.0));
        assertEquals(8, Quarry.gaugeLevel(1.7));
        assertEquals(0, Quarry.gaugeLevel(-0.2));
    }

    @Test
    void quarryMenuSettings() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("menu.pack", "sackito:menus");
        QuarrySettings.Menu menu = QuarrySettings.Menu.from(yaml.getConfigurationSection("menu"));
        assertTrue(menu.drawn());
        assertEquals("sackito:menus/cantera_mejoras", menu.background("cantera_mejoras"));
        assertEquals("sackito:menus/cantera_profundidad_3", menu.model("cantera_profundidad_3").toString());
        QuarrySettings.Menu none = QuarrySettings.Menu.from(null);
        assertFalse(none.drawn());
        assertEquals("", none.background("cantera"));
        assertNull(none.model("invisible"));
    }

    @Test
    void tierStates() {
        // Nivel 1 de 3 comprado: el 1 hecho, el 2 se puede comprar, el 3 bloqueado; más allá del máximo, nada.
        assertEquals(QuarryUpgradesMenu.Tier.OWNED, QuarryUpgradesMenu.tier(1, 1, 3));
        assertEquals(QuarryUpgradesMenu.Tier.NEXT, QuarryUpgradesMenu.tier(1, 2, 3));
        assertEquals(QuarryUpgradesMenu.Tier.LOCKED, QuarryUpgradesMenu.tier(1, 3, 3));
        assertEquals(QuarryUpgradesMenu.Tier.NONE, QuarryUpgradesMenu.tier(1, 4, 3));
        assertEquals(QuarryUpgradesMenu.Tier.OWNED, QuarryUpgradesMenu.tier(3, 3, 3));
    }

    @Test
    void trackIconOnlyBuysLevelsBeyondTheRow() {
        // Con 5 niveles dibujados el icono no cobra: se compra en la casilla del nivel siguiente.
        assertFalse(QuarryUpgradesMenu.iconBuys(1, 3));
        assertFalse(QuarryUpgradesMenu.iconBuys(4, 7));
        // Del 6 en adelante no hay casilla: el icono compra (y enseña el coste).
        assertTrue(QuarryUpgradesMenu.iconBuys(5, 7));
        assertFalse(QuarryUpgradesMenu.iconBuys(7, 7));
    }

    @Test
    void numbersForLore() {
        assertEquals("×1.5", Ui.times(1.5));
        assertEquals("×2", Ui.times(2.0));
        assertEquals("12.5%", Ui.percent(0.125));
        assertEquals("IV", Ui.roman(4));
        assertEquals("Lingote de cobalto", Ui.humanize("rpgroll-items:lingote_de_cobalto"));
    }
}
