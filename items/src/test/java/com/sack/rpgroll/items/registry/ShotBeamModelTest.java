package com.sack.rpgroll.items.registry;

import com.sack.rpgroll.items.core.ItemAction;
import org.bukkit.*;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShotBeamModelTest {
    private static ShotBeamModel.Appearance appearance() {
        return ShotBeamModel.Appearance.from(new ItemAction("SHOOT", Map.of("beam-model", "lasers:beams/orange")));
    }

    @Test void rejectsInvalidWidthsBeforeRendering() {
        for (String width : new String[]{"NaN", "Infinity", "0", "-1", "4.1"}) {
            assertThrows(IllegalArgumentException.class, () -> ShotBeamModel.Appearance.from(
                    new ItemAction("SHOOT", Map.of("beam-model", "lasers:beams/orange", "beam-width", width))));
        }
    }

    @Test void oneDisplaySpansTheActualEndpointsIncludingPitchAndIsReused() {
        World world = mock(World.class);
        ItemDisplay display = mock(ItemDisplay.class);
        when(world.spawn(any(Location.class), eq(ItemDisplay.class), any(java.util.function.Consumer.class)))
                .thenReturn(display);
        ShotBeamModel beam = new ShotBeamModel(appearance());
        try (var items = mockConstruction(ItemStack.class, (item, ctx) ->
                when(item.getItemMeta()).thenReturn(mock(ItemMeta.class)))) {
            beam.update(new Location(world, 1, 70, 2), new Vector(4, 74, 2));
            beam.update(new Location(world, 1, 70, 2), new Vector(4, 74, 2));
        }
        verify(world).spawn(any(Location.class), eq(ItemDisplay.class), any(java.util.function.Consumer.class));
        ArgumentCaptor<Location> location = ArgumentCaptor.forClass(Location.class);
        verify(display, times(2)).teleport(location.capture());
        assertEquals(2.5, location.getValue().getX());
        assertEquals(72, location.getValue().getY());
        ArgumentCaptor<Transformation> transform = ArgumentCaptor.forClass(Transformation.class);
        verify(display, times(2)).setTransformation(transform.capture());
        var t = transform.getValue();
        assertEquals(5, t.getScale().z, 1e-5);
        assertEquals(0.96, t.getScale().x, 1e-5);
        Vector3f end = t.getLeftRotation().transform(new Vector3f(0, 0, 5));
        assertEquals(3, end.x, 1e-5);
        assertEquals(4, end.y, 1e-5);
        beam.remove(); beam.remove();
        verify(display).remove();
    }

    @Test void changingSlotRemovesTheMeshAndQueuedTicksCannotReviveIt() {
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        World world = mock(World.class);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemDisplay display = mock(ItemDisplay.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(1L))).thenReturn(task);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.getWorld()).thenReturn(world);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getEyeLocation()).thenReturn(new Location(world, 0, 70, 0));
        when(world.spawn(any(Location.class), eq(ItemDisplay.class), any(java.util.function.Consumer.class)))
                .thenReturn(display);
        ShotBeam.hold(plugin, player, mock(ShotVisuals.class, RETURNS_SELF), 24, 100, appearance());
        ArgumentCaptor<Runnable> tick = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTaskTimer(eq(plugin), tick.capture(), eq(0L), eq(1L));
        try (var items = mockConstruction(ItemStack.class, (item, ctx) ->
                when(item.getItemMeta()).thenReturn(mock(ItemMeta.class)))) {
            tick.getValue().run();
            when(inventory.getHeldItemSlot()).thenReturn(1);
            tick.getValue().run(); tick.getValue().run();
            ShotBeam.stop(plugin);
        }
        verify(display).remove();
        verify(task).cancel();
        verify(world).spawn(any(Location.class), eq(ItemDisplay.class), any(java.util.function.Consumer.class));
    }
}
