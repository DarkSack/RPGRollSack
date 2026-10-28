package com.sack.rpgroll.items.registry;

import com.sack.rpgroll.items.core.ItemAction;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShootActionTest {

    private Player player;
    private World world;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getEyeLocation()).thenReturn(new Location(world, 0, 65, 0));
    }

    @Test
    void damagesTheFirstCreatureOnTheRayAsThePlayer() {
        LivingEntity target = mock(LivingEntity.class);
        when(world.rayTrace(any(Location.class), any(Vector.class), eq(20.0), eq(FluidCollisionMode.NEVER),
                eq(true), anyDouble(), any())).thenReturn(new RayTraceResult(new Vector(0, 65, 5), target));

        BuiltinItemActions.shoot(new ItemAction("SHOOT", Map.of("range", "20", "amount", "9")),
                new ItemActionContext(player, null, null, null), mock(Plugin.class));

        verify(target).damage(9.0, player);
    }

    @Test
    void missingDoesNotFail() {
        BuiltinItemActions.shoot(new ItemAction("SHOOT", Map.of("particle", "DUST", "color", "#33FF66")),
                new ItemActionContext(player, null, null, null), mock(Plugin.class));
    }

    @Test
    void explosiveShotBreaksBlocksOnlyWhenAsked() {
        when(world.rayTrace(any(Location.class), any(Vector.class), eq(40.0), eq(FluidCollisionMode.NEVER),
                eq(true), anyDouble(), any())).thenReturn(new RayTraceResult(new Vector(0, 65, 8)));

        BuiltinItemActions.shoot(new ItemAction("SHOOT", Map.of("range", "40", "explosion", "2.5",
                "break-blocks", "true")), new ItemActionContext(player, null, null, null), mock(Plugin.class));

        verify(world).createExplosion(any(Location.class), eq(2.5f), eq(false), eq(true), eq(player));
    }

}
