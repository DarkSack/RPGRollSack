package com.sack.rpgroll.traps.registry;

import com.sack.rpgroll.traps.core.TrapAction;
import com.sack.rpgroll.traps.core.TrapBlockConfig;
import com.sack.rpgroll.traps.core.TrapDefinition;
import com.sack.rpgroll.traps.core.TrapDisguiseConfig;
import com.sack.rpgroll.traps.core.TrapTrigger;
import com.sack.rpgroll.traps.location.PlacedTrap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class TrapActionContextTest {

    private final TrapDefinition definition = new TrapDefinition("landmine", "Landmine", "", null,
            TrapTrigger.PRESSURE, Map.of(), 1.5, List.of(), List.of(), 0, -1, List.of(),
            TrapBlockConfig.none(), TrapDisguiseConfig.none());

    @Test
    void anchorLocationPrefersPlacedTrapLocationOverOverride() {
        PlacedTrap placed = new PlacedTrap("p1", "landmine", "world", 1, 2, 3, null, null, null, null, -1, 0);
        Location override = mock(Location.class);
        World world = mock(World.class);

        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(world);

            TrapActionContext ctx = new TrapActionContext(placed, definition, mock(LivingEntity.class), null,
                    override, null);

            Location anchor = ctx.anchorLocation();

            assertEquals(world, anchor.getWorld());
            assertEquals(1, anchor.getBlockX());
            assertNotSame(override, anchor);
        }
    }

    @Test
    void anchorLocationFallsBackToOverrideWhenNoPlacedTrap() {
        Location override = mock(Location.class);

        TrapActionContext ctx = new TrapActionContext(null, null, mock(LivingEntity.class), null, override, "turret");

        assertSame(override, ctx.anchorLocation());
    }

    @Test
    void anchorLocationIsNullWhenNeitherPlacedNorOverridePresent() {
        TrapActionContext ctx = new TrapActionContext(null, null, mock(LivingEntity.class), null, null, "turret");

        assertNull(ctx.anchorLocation());
    }

    @Test
    void triggeringPlayerReturnsTargetWhenItIsAPlayer() {
        Player player = mock(Player.class);
        TrapActionContext ctx = new TrapActionContext(null, null, player, null, null, null);

        assertSame(player, ctx.triggeringPlayer());
    }

    @Test
    void triggeringPlayerIsNullWhenTargetIsNonPlayerLivingEntity() {
        LivingEntity mob = mock(LivingEntity.class);
        TrapActionContext ctx = new TrapActionContext(null, null, mob, null, null, null);

        assertNull(ctx.triggeringPlayer());
    }

    @Test
    void sourceIdPrefersDefinitionIdOverSourceLabel() {
        TrapActionContext ctx = new TrapActionContext(null, definition, mock(LivingEntity.class), null, null,
                "turret-1");

        assertEquals("landmine", ctx.sourceId());
    }

    @Test
    void sourceIdFallsBackToSourceLabelWhenNoDefinition() {
        TrapActionContext ctx = new TrapActionContext(null, null, mock(LivingEntity.class), null, null, "turret-1");

        assertEquals("turret-1", ctx.sourceId());
    }
}
