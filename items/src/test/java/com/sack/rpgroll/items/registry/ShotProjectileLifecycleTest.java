package com.sack.rpgroll.items.registry;

import org.bukkit.*;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;

class ShotProjectileLifecycleTest {
    @Test
    void disablingRemovesTheDisplayAndCancelsTheTaskWithoutImpactEvenBeforeFirstTick() {
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        World world = mock(World.class);
        Player shooter = mock(Player.class);
        ItemDisplay display = mock(ItemDisplay.class);
        ShotImpact impact = mock(ShotImpact.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(1L), eq(1L))).thenReturn(task);
        when(world.spawn(any(Location.class), eq(ItemDisplay.class), any(java.util.function.Consumer.class)))
                .thenReturn(display);
        try (var items = mockConstruction(ItemStack.class, (item, ctx) ->
                when(item.getItemMeta()).thenReturn(mock(ItemMeta.class)))) {
            ShotProjectile.launch(plugin, shooter, new Location(world, 0, 65, 0), new Vector(0, 0, 1),
                    1, 0, 40, "demo:plasma", mock(ShotVisuals.class), impact);
        }
        ArgumentCaptor<Runnable> tick = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTaskTimer(eq(plugin), tick.capture(), eq(1L), eq(1L));
        BuiltinItemActions.shutdown(plugin);
        BuiltinItemActions.shutdown(plugin);
        tick.getValue().run(); // un callback ya encolado tampoco vuelve a mover el proyectil
        verify(display).remove();
        verify(display, never()).teleport(any(Location.class));
        verify(task).cancel();
        verifyNoInteractions(impact);
    }
}
