package com.sack.rpgroll.enchantments.condition;

import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConditionEvaluatorTest {

    private Player player;
    private World world;
    private ConditionEvaluator evaluator;
    private ConditionContext context;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        world = mock(World.class);
        when(player.getWorld()).thenReturn(world);

        evaluator = new ConditionEvaluator();
        context = new ConditionContext(player, null);
    }

    @Test
    void evaluateAllReturnsTrueForEmptyConditionList() {
        assertTrue(evaluator.evaluateAll(List.of(), context));
    }

    @Test
    void numericComparisonSupportsAllOperators() {
        when(player.getLevel()).thenReturn(20);

        assertTrue(evaluator.evaluateAll(List.of("player.level == 20"), context));
        assertTrue(evaluator.evaluateAll(List.of("player.level >= 20"), context));
        assertTrue(evaluator.evaluateAll(List.of("player.level <= 20"), context));
        assertTrue(evaluator.evaluateAll(List.of("player.level > 10"), context));
        assertTrue(evaluator.evaluateAll(List.of("player.level < 30"), context));
        assertTrue(evaluator.evaluateAll(List.of("player.level != 5"), context));
        assertFalse(evaluator.evaluateAll(List.of("player.level < 20"), context));
    }

    @Test
    void healthComparisonUsesRawValueNotPercentage() {
        when(player.getHealth()).thenReturn(8.0);

        assertTrue(evaluator.evaluateAll(List.of("player.health < 10"), context));
        assertFalse(evaluator.evaluateAll(List.of("player.health > 10"), context));
    }

    @Test
    void evaluateAllShortCircuitsOnFirstFailingCondition() {
        when(player.getLevel()).thenReturn(5);

        assertFalse(evaluator.evaluateAll(List.of("player.level >= 20", "player.level >= 1"), context));
    }

    @Test
    void worldAndWeatherResolveFromPlayerWorld() {
        when(world.getName()).thenReturn("world_nether");
        when(world.isThundering()).thenReturn(true);

        assertTrue(evaluator.evaluateAll(List.of("world == world_nether"), context));
        assertTrue(evaluator.evaluateAll(List.of("weather == STORM"), context));
    }

    @Test
    void weatherResolvesRainWhenStormingWithoutThunder() {
        when(world.isThundering()).thenReturn(false);
        when(world.hasStorm()).thenReturn(true);

        assertTrue(evaluator.evaluateAll(List.of("weather == RAIN"), context));
    }

    @Test
    void weatherResolvesClearWhenNoStormOrThunder() {
        when(world.isThundering()).thenReturn(false);
        when(world.hasStorm()).thenReturn(false);

        assertTrue(evaluator.evaluateAll(List.of("weather == CLEAR"), context));
    }

    @Test
    void targetPathsResolveNullWhenTargetAbsent() {
        assertFalse(evaluator.evaluateAll(List.of("target.type == ZOMBIE"), context));
    }

    @Test
    void targetPathsResolveWhenTargetPresent() {
        LivingEntity target = mock(LivingEntity.class);
        when(target.getType()).thenReturn(EntityType.ZOMBIE);
        ConditionContext withTarget = new ConditionContext(player, target);

        assertTrue(evaluator.evaluateAll(List.of("target.type == zombie"), withTarget));
    }

    @Test
    void functionCallHasPermissionDelegatesToPlayer() {
        when(player.hasPermission("rpg.vip")).thenReturn(true);

        assertTrue(evaluator.evaluateAll(List.of("player.hasPermission(rpg.vip)"), context));
        assertFalse(evaluator.evaluateAll(List.of("player.hasPermission(rpg.other)"), context));
    }

    @Test
    void unknownPathAndMalformedConditionEvaluateToFalse() {
        assertFalse(evaluator.evaluateAll(List.of("player.unknownstat >= 1"), context));
        assertFalse(evaluator.evaluateAll(List.of("garbage condition"), context));
    }
}
