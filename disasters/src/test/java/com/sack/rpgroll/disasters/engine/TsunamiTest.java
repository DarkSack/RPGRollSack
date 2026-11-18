package com.sack.rpgroll.disasters.engine;

import com.sack.rpgroll.disasters.core.DisasterPhenomenon;
import com.sack.rpgroll.disasters.core.DisasterType;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TsunamiTest {

    @Test
    void findingWaterDoesNotReadUnloadedChunks() {
        World world = mock(World.class);
        DisasterEngine engine = mock(DisasterEngine.class);
        Player player = mock(Player.class);
        when(player.getLocation()).thenReturn(new Location(world, -0.5, 64, -0.5));
        when(engine.targets(world)).thenReturn(List.of(player));
        Tsunami tsunami = new Tsunami(engine, world,
                new DisasterPhenomenon(DisasterType.TSUNAMI, Map.of("require-water", true)),
                new Phenomenon.Power(2.2, 1.5), 1680);

        tsunami.tick();

        verify(world, never()).getHighestBlockAt(any(Location.class), any(HeightMap.class));
    }

    @Test
    void renderingDoesNotReadChunksThatUnloadedAfterLaunch() {
        World world = mock(World.class);
        DisasterEngine engine = mock(DisasterEngine.class);
        Player player = mock(Player.class);
        Block ocean = mock(Block.class);
        when(ocean.getType()).thenReturn(Material.WATER);
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(engine.targets(world)).thenReturn(List.of(player));
        when(engine.damage()).thenReturn(mock(WorldDamage.class));
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getHighestBlockAt(any(Location.class), any(HeightMap.class))).thenReturn(ocean);
        Tsunami tsunami = new Tsunami(engine, world,
                new DisasterPhenomenon(DisasterType.TSUNAMI, Map.of("require-water", true, "start-sound", "",
                        "sound", "", "roar-sound", "", "breakable-blocks", List.of())),
                new Phenomenon.Power(2.2, 1.5), 1680);
        tsunami.tick();
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);

        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createBlockData(Material.WATER)).thenReturn(mock(Levelled.class));
            tsunami.tick();
        }

        verify(world, never()).getHighestBlockYAt(any(Location.class), any(HeightMap.class));
        verify(world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
    }

    /** La ola de 12 bloques: el frente llega hasta la punta, la punta cuelga y debajo queda el tubo. */
    @Test
    void theWaveCurlsOverAHollowTube() {

        List<Tsunami.Slice> slices = Tsunami.profile(12, 0.5, 1.0);

        double reach = slices.stream().mapToDouble(Tsunami.Slice::forward).max().orElseThrow();
        assertEquals(6, reach, 1e-9);

        // El pie del frente es agua de abajo arriba.
        assertTrue(slices.stream().anyMatch(s -> s.forward() == 0 && s.bottom() == 0 && s.top() == 11));

        // La punta cuelga hasta la mitad, mucho más abajo de donde llega el frente a esa distancia.
        Tsunami.Slice tip = slices.stream().filter(s -> s.tip() && s.forward() == reach).findFirst().orElseThrow();
        assertEquals(6, tip.bottom());

        // A media distancia el agua empieza arriba: debajo está el hueco del tubo.
        Tsunami.Slice middle = slices.stream().filter(s -> !s.tip() && s.forward() > 2.5 && s.forward() < 3.5
                && s.top() == 11).findFirst().orElseThrow();
        assertTrue(middle.bottom() >= 6, "hueco bajo la cresta: empieza en " + middle.bottom());

        // Detrás, el lomo baja hasta el suelo y cada vez es más bajo.
        List<Tsunami.Slice> back = slices.stream().filter(s -> s.forward() < 0).toList();
        assertTrue(back.size() > 10);
        for (int i = 1; i < back.size(); i++) {
            assertTrue(back.get(i).top() <= back.get(i - 1).top());
        }
    }

    /** En las puntas de la ola (columnas bajas) no hay rizo: un poco de agua y ya. */
    @Test
    void shortColumnsDoNotCurl() {
        List<Tsunami.Slice> slices = Tsunami.profile(1, 0.5, 1.0);
        assertTrue(slices.stream().allMatch(s -> s.forward() <= 1));
        assertTrue(slices.stream().anyMatch(s -> s.forward() == 0 && s.bottom() == 0 && s.top() == 0));
    }
}
