package com.sack.rpgroll.mobs.condition;

import com.sack.rpgroll.mobs.registry.ConditionRegistry;

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

class MobConditionEvaluatorTest {

    private ConditionRegistry registry;
    private MobConditionEvaluator evaluator;
    private LivingEntity mob;
    private World world;

    @BeforeEach
    void setUp() {
        registry = new ConditionRegistry();
        evaluator = new MobConditionEvaluator(registry);

        world = mock(World.class);
        when(world.getName()).thenReturn("world");

        mob = mock(LivingEntity.class);
        when(mob.getWorld()).thenReturn(world);
    }

    @Test
    void malformedConditionIsFalse() {
        MobConditionContext context = new MobConditionContext(mob, null);
        assertFalse(evaluate("not a comparison", context));
    }

    private boolean evaluate(String condition, MobConditionContext context) {
        return evaluator.evaluateAll(List.of(condition), context);
    }

    @Test
    void evaluatesWorldNameEquality() {
        MobConditionContext context = new MobConditionContext(mob, null);
        assertTrue(evaluate("world == world", context));
        assertTrue(evaluate("world != other", context));
        assertFalse(evaluate("world == other", context));
    }

    @Test
    void rawHealthComparisonUsesGetHealthDirectly() {
        when(mob.getHealth()).thenReturn(10.0);

        MobConditionContext context = new MobConditionContext(mob, null);

        assertTrue(evaluate("mob.health < 15", context));
        assertFalse(evaluate("mob.health < 5", context));
        assertTrue(evaluate("mob.health >= 10", context));
    }

    @Test
    void healthPercentageAgainstNullTargetIsFalse() {
        MobConditionContext context = new MobConditionContext(mob, null);
        assertFalse(evaluate("target.health < 50%", context));
    }

    @Test
    void evaluatesTargetTypeComparison() {
        LivingEntity target = mock(Player.class);
        when(target.getType()).thenReturn(EntityType.PLAYER);

        MobConditionContext context = new MobConditionContext(mob, target);

        assertTrue(evaluate("target.type == PLAYER", context));
        assertFalse(evaluate("target.type == ZOMBIE", context));
    }

    @Test
    void nullTargetTypeIsFalse() {
        MobConditionContext context = new MobConditionContext(mob, null);
        assertFalse(evaluate("target.type == PLAYER", context));
    }

    @Test
    void evaluatesWeatherFromWorldState() {
        when(world.isThundering()).thenReturn(true);
        assertTrue(evaluate("weather == STORM", new MobConditionContext(mob, null)));

        when(world.isThundering()).thenReturn(false);
        when(world.hasStorm()).thenReturn(true);
        assertTrue(evaluate("weather == RAIN", new MobConditionContext(mob, null)));

        when(world.hasStorm()).thenReturn(false);
        assertTrue(evaluate("weather == CLEAR", new MobConditionContext(mob, null)));
    }

    @Test
    void evaluatesNightBoundaryUsingWorldTime() {
        when(world.getTime()).thenReturn(13000L);
        assertTrue(evaluate("night == true", new MobConditionContext(mob, null)));

        when(world.getTime()).thenReturn(23000L);
        assertTrue(evaluate("night == true", new MobConditionContext(mob, null)));

        when(world.getTime()).thenReturn(12999L);
        assertFalse(evaluate("night == true", new MobConditionContext(mob, null)));

        when(world.getTime()).thenReturn(23001L);
        assertFalse(evaluate("night == true", new MobConditionContext(mob, null)));
    }

    @Test
    void unresolvedNumericComparisonWithNonNumericRightIsFalse() {
        when(mob.getHealth()).thenReturn(10.0);

        assertFalse(evaluate("mob.health > notanumber", new MobConditionContext(mob, null)));
    }

    @Test
    void unknownPathFallsBackToConditionRegistryAndDefaultsToFalseWhenUnregistered() {
        assertFalse(evaluate("guild.rank == 3", new MobConditionContext(mob, null)));
    }

    @Test
    void registeredConditionVariableIsResolvedThroughRegistry() {
        registry.registerVariable("guild.rank", ctx -> 3.0);

        assertTrue(evaluate("guild.rank == 3", new MobConditionContext(mob, null)));
        assertFalse(evaluate("guild.rank == 4", new MobConditionContext(mob, null)));
    }

    @Test
    void evaluateAllRequiresEveryConditionToPass() {
        MobConditionContext context = new MobConditionContext(mob, null);
        assertTrue(evaluator.evaluateAll(List.of("world == world"), context));
        assertFalse(evaluator.evaluateAll(List.of("world == world", "world == other"), context));
    }
}
