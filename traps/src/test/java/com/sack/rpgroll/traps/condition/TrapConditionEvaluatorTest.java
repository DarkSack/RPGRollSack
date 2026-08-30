package com.sack.rpgroll.traps.condition;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrapConditionEvaluatorTest {

    private TrapConditionEvaluator evaluator;
    private Player player;
    private World world;

    @BeforeEach
    void setUp() {
        evaluator = new TrapConditionEvaluator(new TrapConditionRegistry());
        player = mock(Player.class);
        world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
    }

    @Test
    void allConditionsMustPassForEvaluateAllToBeTrue() {
        when(player.getLevel()).thenReturn(30);
        when(player.isSneaking()).thenReturn(true);

        TrapConditionContext ctx = new TrapConditionContext(player);

        assertTrue(evaluator.evaluateAll(List.of("player.level >= 30", "player.sneaking == true"), ctx));
        assertFalse(evaluator.evaluateAll(List.of("player.level >= 30", "player.sneaking == false"), ctx));
    }

    @Test
    void evaluateAllOnEmptyListIsVacuouslyTrue() {
        assertTrue(evaluator.evaluateAll(List.of(), new TrapConditionContext(player)));
    }

    @Test
    void malformedConditionSyntaxNeverMatches() {
        assertFalse(evaluator.evaluateAll(List.of("not a valid condition"), new TrapConditionContext(player)));
    }

    @Test
    void playerConditionsFailWhenNoPlayerInContext() {
        TrapConditionContext ctx = new TrapConditionContext(null);

        assertFalse(evaluator.evaluateAll(List.of("player.level >= 1"), ctx));
    }

    @Test
    void numericComparisonOperatorsWorkForPlayerLevel() {
        when(player.getLevel()).thenReturn(10);
        TrapConditionContext ctx = new TrapConditionContext(player);

        assertTrue(evaluator.evaluateAll(List.of("player.level == 10"), ctx));
        assertTrue(evaluator.evaluateAll(List.of("player.level != 5"), ctx));
        assertTrue(evaluator.evaluateAll(List.of("player.level < 20"), ctx));
        assertTrue(evaluator.evaluateAll(List.of("player.level <= 10"), ctx));
        assertTrue(evaluator.evaluateAll(List.of("player.level > 5"), ctx));
        assertFalse(evaluator.evaluateAll(List.of("player.level > 10"), ctx));
    }

    @Test
    void stringComparisonWorksForWorldName() {
        when(world.getName()).thenReturn("SkyBlock");
        TrapConditionContext ctx = new TrapConditionContext(player);

        assertTrue(evaluator.evaluateAll(List.of("world == skyblock"), ctx));
        assertTrue(evaluator.evaluateAll(List.of("world != nether"), ctx));
    }

    // Nota: no se testea "player.health <= X%" acá — referenciar la constante
    // org.bukkit.attribute.Attribute.MAX_HEALTH (Paper 26) dispara la carga de
    // org.bukkit.Registry, cuyo <clinit> exige un RegistryAccess real de un
    // servidor vivo. Mismo problema que Biome en RateConditionEvaluatorTest.

    @Test
    void healthPercentageIsFalseWhenNoPlayerInContext() {
        TrapConditionContext ctx = new TrapConditionContext(null);

        assertFalse(evaluator.evaluateAll(List.of("player.health <= 50%"), ctx));
    }

    @Test
    void nightConditionChecksWorldTimeWindow() {
        TrapConditionContext ctx = new TrapConditionContext(player);

        when(world.getTime()).thenReturn(13000L);
        assertTrue(evaluator.evaluateAll(List.of("night == true"), ctx));

        when(world.getTime()).thenReturn(23000L);
        assertTrue(evaluator.evaluateAll(List.of("night == true"), ctx));

        when(world.getTime()).thenReturn(12999L);
        assertFalse(evaluator.evaluateAll(List.of("night == true"), ctx));

        when(world.getTime()).thenReturn(23001L);
        assertFalse(evaluator.evaluateAll(List.of("night == true"), ctx));
    }

    @Test
    void weatherResolvesStormOverRainOverClear() {
        TrapConditionContext ctx = new TrapConditionContext(player);

        when(world.isThundering()).thenReturn(false);
        when(world.hasStorm()).thenReturn(false);
        assertTrue(evaluator.evaluateAll(List.of("weather == clear"), ctx));

        when(world.hasStorm()).thenReturn(true);
        assertTrue(evaluator.evaluateAll(List.of("weather == rain"), ctx));

        when(world.isThundering()).thenReturn(true);
        assertTrue(evaluator.evaluateAll(List.of("weather == storm"), ctx));
    }

    @Test
    void customVariableFromRegistryIsResolvedWhenNotBuiltin() {
        TrapConditionRegistry registry = new TrapConditionRegistry();
        registry.registerVariable("guild.rank", c -> 3.0);
        TrapConditionEvaluator customEvaluator = new TrapConditionEvaluator(registry);

        assertTrue(customEvaluator.evaluateAll(List.of("guild.rank >= 3"), new TrapConditionContext(player)));
    }

    @Test
    void unknownVariableWithNoRegistryMatchNeverMatches() {
        assertFalse(evaluator.evaluateAll(List.of("bogus.path == 1"), new TrapConditionContext(player)));
    }
}
