package com.sack.rpgroll.extras.expression;

import com.sack.rpgroll.extras.activity.ActivityState;
import com.sack.rpgroll.extras.activity.ActivityStateResolver;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateConditionEvaluatorTest {

    private ActivityStateResolver activityStateResolver;
    private RateConditionEvaluator evaluator;
    private Player player;
    private World world;

    @BeforeEach
    void setUp() {
        activityStateResolver = mock(ActivityStateResolver.class);
        evaluator = new RateConditionEvaluator(activityStateResolver);
        player = mock(Player.class);
        world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
    }

    @Test
    void blankConditionAlwaysMatches() {
        assertTrue(evaluator.matches(null, player));
        assertTrue(evaluator.matches("  ", player));
    }

    @Test
    void matchesActivityStateKeywordCaseInsensitively() {
        when(activityStateResolver.resolve(player)).thenReturn(ActivityState.SPRINTING);

        assertTrue(evaluator.matches("sprinting", player));
        assertTrue(evaluator.matches("SPRINTING", player));
        assertFalse(evaluator.matches("resting", player));
    }

    @Test
    void matchesUnderwaterAsSpecialKeyword() {
        Location eyeLocation = mock(Location.class);
        Block eyeBlock = mock(Block.class);
        when(player.isInWater()).thenReturn(true);
        when(player.getEyeLocation()).thenReturn(eyeLocation);
        when(eyeLocation.getBlock()).thenReturn(eyeBlock);
        when(eyeBlock.isLiquid()).thenReturn(true);

        assertTrue(evaluator.matches("underwater", player));
    }

    @Test
    void underwaterRequiresBothInWaterAndLiquidEyeBlock() {
        Location eyeLocation = mock(Location.class);
        Block eyeBlock = mock(Block.class);
        when(player.isInWater()).thenReturn(true);
        when(player.getEyeLocation()).thenReturn(eyeLocation);
        when(eyeLocation.getBlock()).thenReturn(eyeBlock);
        when(eyeBlock.isLiquid()).thenReturn(false);

        assertFalse(evaluator.matches("underwater", player));
    }

    // Nota: no se testea "biome:" acá — org.bukkit.block.Biome (Paper 26) inicializa
    // sus constantes contra un RegistryAccess real en <clinit>, así que tocar cualquier
    // valor de Biome fuera de un servidor vivo revienta con IllegalStateException.

    @Test
    void matchesWeatherClearRainAndThunder() {
        when(world.isThundering()).thenReturn(false);
        when(world.hasStorm()).thenReturn(false);
        assertTrue(evaluator.matches("weather:clear", player));

        when(world.hasStorm()).thenReturn(true);
        assertTrue(evaluator.matches("weather:rain", player));

        when(world.isThundering()).thenReturn(true);
        assertTrue(evaluator.matches("weather:thunder", player));
        assertFalse(evaluator.matches("weather:rain", player));
    }

    @Test
    void matchesWorldNameCaseInsensitively() {
        when(world.getName()).thenReturn("Overworld");

        assertTrue(evaluator.matches("world:overworld", player));
        assertFalse(evaluator.matches("world:nether", player));
    }

    @Test
    void matchesDimensionByEnvironmentName() {
        when(world.getEnvironment()).thenReturn(World.Environment.NETHER);

        assertTrue(evaluator.matches("dimension:nether", player));
        assertFalse(evaluator.matches("dimension:the_end", player));
    }

    @Test
    void unknownPrefixNeverMatches() {
        assertFalse(evaluator.matches("bogus:value", player));
    }
}
