package com.sack.rpgroll.crafting.condition;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers only the condition branches that don't depend on RPGRollAPI/
 * GuildsAPI/SeasonsAPI (LEVEL_MIN/RACE/CLASS/JOB_MIN/GUILD_MEMBER/SEASON
 * all fail closed to false without a live addon, so they're not
 * interesting to unit-test here) — WORLD, HOUR_RANGE, WEATHER and
 * PERMISSION are pure logic against a Player/World and are worth locking
 * down directly.
 */
class ConditionEvaluatorTest {

    private final ConditionEvaluator evaluator = new ConditionEvaluator();

    @Test
    void evaluateAllOfflineReturnsTrueForEmptyOrNullConditions() {
        assertTrue(evaluator.evaluateAllOffline(null, UUID.randomUUID(), null, null));
        assertTrue(evaluator.evaluateAllOffline(List.of(), UUID.randomUUID(), null, null));
    }

    @Test
    void permissionRequiresOnlinePlayerAndPermission() {
        Player player = mock(Player.class);
        when(player.hasPermission("rpg.craft.forge")).thenReturn(true);

        RecipeCondition condition = RecipeCondition.of(ConditionType.PERMISSION, "rpg.craft.forge");

        assertTrue(evaluator.evaluateOffline(condition, null, player, null));
        assertFalse(evaluator.evaluateOffline(condition, null, null, null));
    }

    @Test
    void worldMatchesFallbackWorldNameCaseInsensitively() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world_nether");

        RecipeCondition condition = RecipeCondition.of(ConditionType.WORLD, "WORLD_NETHER");

        assertTrue(evaluator.evaluateOffline(condition, null, null, world));
    }

    @Test
    void worldFailsWhenFallbackWorldIsNull() {
        RecipeCondition condition = RecipeCondition.of(ConditionType.WORLD, "world");

        assertFalse(evaluator.evaluateOffline(condition, null, null, null));
    }

    @Test
    void hourRangeHandlesWraparoundRanges() {
        World world = mock(World.class);
        // tick 0 -> hour (0/1000 + 6) % 24 = 6
        when(world.getTime()).thenReturn(0L);

        RecipeCondition wraparound = RecipeCondition.of(ConditionType.HOUR_RANGE, "22-4");
        RecipeCondition normal = RecipeCondition.of(ConditionType.HOUR_RANGE, "5-10");

        assertTrue(evaluator.evaluateOffline(normal, null, null, world));
        assertFalse(evaluator.evaluateOffline(wraparound, null, null, world));
    }

    @Test
    void hourRangeFailsOnMalformedRange() {
        World world = mock(World.class);
        when(world.getTime()).thenReturn(0L);

        assertFalse(evaluator.evaluateOffline(RecipeCondition.of(ConditionType.HOUR_RANGE, "not-a-range"), null, null,
                world));
        assertFalse(
                evaluator.evaluateOffline(RecipeCondition.of(ConditionType.HOUR_RANGE, "no-dash-here"), null, null,
                        world));
    }

    @Test
    void weatherResolvesThunderRainAndClear() {
        World thunder = mock(World.class);
        when(thunder.isThundering()).thenReturn(true);
        World rain = mock(World.class);
        when(rain.isThundering()).thenReturn(false);
        when(rain.hasStorm()).thenReturn(true);
        World clear = mock(World.class);
        when(clear.hasStorm()).thenReturn(false);

        assertTrue(evaluator.evaluateOffline(RecipeCondition.of(ConditionType.WEATHER, "THUNDER"), null, null,
                thunder));
        assertTrue(evaluator.evaluateOffline(RecipeCondition.of(ConditionType.WEATHER, "rain"), null, null, rain));
        assertTrue(evaluator.evaluateOffline(RecipeCondition.of(ConditionType.WEATHER, "clear"), null, null, clear));
    }

    @Test
    void weatherFailsOnUnknownValueOrNullWorld() {
        assertFalse(evaluator.evaluateOffline(RecipeCondition.of(ConditionType.WEATHER, "clear"), null, null, null));
    }

    @Test
    void evaluateAllOfflineShortCircuitsOnFirstFailure() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");

        List<RecipeCondition> conditions = List.of(
                RecipeCondition.of(ConditionType.WORLD, "world"),
                RecipeCondition.of(ConditionType.WORLD, "other_world"));

        assertFalse(evaluator.evaluateAllOffline(conditions, null, null, world));
    }
}
