package com.sack.rpgroll.extras.stat;

import com.sack.rpgroll.extras.action.ExtrasActionExecutor;
import com.sack.rpgroll.extras.expression.RateConditionEvaluator;
import com.sack.rpgroll.extras.modifier.ModifierResolver;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatEngineTest {

    private StatManager statManager;
    private ExtrasActionExecutor actionExecutor;
    private StatEngine engine;
    private Player player;

    @BeforeEach
    void setUp() {
        Plugin plugin = mock(Plugin.class);
        statManager = mock(StatManager.class);
        RateConditionEvaluator rateConditionEvaluator = mock(RateConditionEvaluator.class);
        actionExecutor = mock(ExtrasActionExecutor.class);
        engine = new StatEngine(plugin, statManager, rateConditionEvaluator, actionExecutor);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    }

    private StatDefinition stat(String id, double max, double start) {
        return new StatDefinition(id, true, max, start, null, null, Map.of(), List.of());
    }

    @Test
    void getReturnsConfiguredStartValueOnFirstAccess() {
        when(statManager.get("thirst")).thenReturn(Optional.of(stat("thirst", 100, 100)));

        assertEquals(100.0, engine.get(player, "thirst"));
    }

    @Test
    void getReturnsZeroWhenStatIsUnknown() {
        when(statManager.get("bogus")).thenReturn(Optional.empty());

        assertEquals(0.0, engine.get(player, "bogus"));
    }

    @Test
    void setClampsToZeroLowerBound() {
        when(statManager.get("thirst")).thenReturn(Optional.of(stat("thirst", 100, 100)));

        engine.set(player, "thirst", -50);

        assertEquals(0.0, engine.get(player, "thirst"));
    }

    @Test
    void setClampsToConfiguredMaxUpperBound() {
        when(statManager.get("thirst")).thenReturn(Optional.of(stat("thirst", 100, 50)));

        engine.set(player, "thirst", 500);

        assertEquals(100.0, engine.get(player, "thirst"));
    }

    @Test
    void setForUnknownStatStillClampsToZeroButHasNoUpperBound() {
        when(statManager.get("bogus")).thenReturn(Optional.empty());

        engine.set(player, "bogus", -5);
        assertEquals(0.0, engine.get(player, "bogus"));
    }

    @Test
    void adjustAddsDeltaToCurrentValue() {
        when(statManager.get("thirst")).thenReturn(Optional.of(stat("thirst", 100, 50)));

        engine.adjust(player, "thirst", 10);

        assertEquals(60.0, engine.get(player, "thirst"));
    }

    @Test
    void maxIsScaledByLinkedModifierResolverStatMaxMultiplier() {
        when(statManager.get("thirst")).thenReturn(Optional.of(stat("thirst", 100, 0)));

        ModifierResolver modifierResolver = mock(ModifierResolver.class);
        when(modifierResolver.multiplier(player, "thirst_max")).thenReturn(1.5);
        engine.linkModifiers(modifierResolver);

        engine.set(player, "thirst", 1000);

        assertEquals(150.0, engine.get(player, "thirst"), 1e-9);
    }

    @Test
    void consumeSubtractsConfiguredAmountForKnownAction() {
        StatDefinition stamina = new StatDefinition("stamina", true, 100, 100, null, null,
                Map.of("jump", 5.0), List.of());
        when(statManager.get("stamina")).thenReturn(Optional.of(stamina));

        engine.consume(player, "stamina", "JUMP");

        assertEquals(95.0, engine.get(player, "stamina"));
    }

    @Test
    void consumeDoesNothingForActionNotInConsumptionMap() {
        StatDefinition stamina = new StatDefinition("stamina", true, 100, 100, null, null,
                Map.of("jump", 5.0), List.of());
        when(statManager.get("stamina")).thenReturn(Optional.of(stamina));

        engine.consume(player, "stamina", "swim");

        assertEquals(100.0, engine.get(player, "stamina"));
    }

    @Test
    void consumeAllAppliesActionToEveryStatThatDefinesIt() {
        StatDefinition stamina = new StatDefinition("stamina", true, 100, 100, null, null,
                Map.of("attack", 3.0), List.of());
        StatDefinition mana = new StatDefinition("mana", true, 100, 100, null, null,
                Map.of("attack", 7.0), List.of());
        when(statManager.getAll()).thenReturn(List.of(stamina, mana));
        when(statManager.get("stamina")).thenReturn(Optional.of(stamina));
        when(statManager.get("mana")).thenReturn(Optional.of(mana));

        engine.consumeAll(player, "attack");

        assertEquals(97.0, engine.get(player, "stamina"));
        assertEquals(93.0, engine.get(player, "mana"));
    }

    @Test
    void clearResetsValueBackToConfiguredStart() {
        when(statManager.get("thirst")).thenReturn(Optional.of(stat("thirst", 100, 80)));
        engine.set(player, "thirst", 10);

        engine.clear(player);

        assertEquals(80.0, engine.get(player, "thirst"));
    }
}
