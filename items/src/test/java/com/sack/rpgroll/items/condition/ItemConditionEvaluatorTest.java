package com.sack.rpgroll.items.condition;

import com.sack.rpgroll.items.registry.ConditionRegistry;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItemConditionEvaluatorTest {

    private Player player;
    private ItemConditionEvaluator evaluator;
    private ItemConditionContext context;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);

        evaluator = new ItemConditionEvaluator(new ConditionRegistry());
        context = new ItemConditionContext(player, null);
    }

    @Test
    void evaluateAllReturnsTrueForEmptyList() {
        assertTrue(evaluator.evaluateAll(List.of(), context));
    }

    @Test
    void evaluateAllReturnsFalseAsSoonAsOneConditionFails() {
        when(player.getLevel()).thenReturn(10);

        assertFalse(evaluator.evaluateAll(List.of("player.level >= 25"), context));
    }

    @Test
    void numericComparisonSupportsAllOperators() {
        when(player.getLevel()).thenReturn(25);

        assertTrue(evaluator.evaluateAll(List.of("player.level == 25"), context));
        assertTrue(evaluator.evaluateAll(List.of("player.level >= 25"), context));
        assertTrue(evaluator.evaluateAll(List.of("player.level <= 25"), context));
        assertTrue(evaluator.evaluateAll(List.of("player.level > 10"), context));
        assertTrue(evaluator.evaluateAll(List.of("player.level < 30"), context));
        assertTrue(evaluator.evaluateAll(List.of("player.level != 1"), context));
        assertFalse(evaluator.evaluateAll(List.of("player.level > 25"), context));
    }

    @Test
    void stringComparisonIsCaseInsensitive() {
        LivingEntity target = mock(LivingEntity.class);
        when(target.getType()).thenReturn(org.bukkit.entity.EntityType.ZOMBIE);
        context = new ItemConditionContext(player, target);

        assertTrue(evaluator.evaluateAll(List.of("target.type == zombie"), context));
        assertFalse(evaluator.evaluateAll(List.of("target.type != zombie"), context));
    }

    @Test
    void unknownVariablePathEvaluatesToFalse() {
        assertFalse(evaluator.evaluateAll(List.of("player.unknownstat >= 1"), context));
    }

    @Test
    void functionCallHasPermissionDelegatesToPlayer() {
        when(player.hasPermission("rpgroll.vip")).thenReturn(true);

        assertTrue(evaluator.evaluateAll(List.of("player.hasPermission(rpgroll.vip)"), context));
        assertFalse(evaluator.evaluateAll(List.of("player.hasPermission(rpgroll.other)"), context));
    }

    @Test
    void malformedConditionEvaluatesToFalse() {
        assertFalse(evaluator.evaluateAll(List.of("not a condition at all"), context));
    }
}
