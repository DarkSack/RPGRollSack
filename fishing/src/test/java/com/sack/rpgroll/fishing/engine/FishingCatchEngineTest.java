package com.sack.rpgroll.fishing.engine;

import com.sack.rpgroll.fishing.core.Bait;
import com.sack.rpgroll.fishing.core.CatchQuality;
import com.sack.rpgroll.fishing.core.DepthRequirement;
import com.sack.rpgroll.fishing.core.FishBehaviorType;
import com.sack.rpgroll.fishing.core.FishCategory;
import com.sack.rpgroll.fishing.core.FishRarity;
import com.sack.rpgroll.fishing.core.FishSpecies;
import com.sack.rpgroll.fishing.core.FishSpeciesManager;
import com.sack.rpgroll.fishing.core.FishingRod;
import com.sack.rpgroll.fishing.core.Junk;
import com.sack.rpgroll.fishing.core.Treasure;
import com.sack.rpgroll.fishing.core.TreasureManager;
import com.sack.rpgroll.fishing.core.JunkManager;
import com.sack.rpgroll.fishing.core.WaterType;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FishingCatchEngineTest {

    @Mock
    private FishSpeciesManager speciesManager;

    @Mock
    private TreasureManager treasureManager;

    @Mock
    private JunkManager junkManager;

    @Mock
    private FishingConditionsResolver conditionsResolver;

    @Mock
    private Player player;

    @Mock
    private Location hookLocation;

    private FishSpecies cod() {
        return new FishSpecies("cod", "Cod", null, 0, null, FishCategory.FRESHWATER, FishRarity.COMMON,
                Set.of(WaterType.LAKE), Set.of(), Set.of(), 1, 5, 10, 50, 10, 5, FishBehaviorType.SLOW, Set.of(),
                Set.of(), Set.of(), Set.of(), false, 0, false, null, null, null);
    }

    private FishingConditions lakeConditions() {
        return new FishingConditions("plains", WaterType.LAKE, DepthRequirement.MID_WATER,
                com.sack.rpgroll.fishing.core.WeatherType.SUNNY, Set.of(), null);
    }

    @BeforeEach
    void setUp() {
        lenient().when(conditionsResolver.resolve(hookLocation)).thenReturn(lakeConditions());
    }

    @Test
    void alwaysReturnsTreasureWhenTreasureChanceIsOne() {
        Treasure treasure = new Treasure("chest", null, null, null, FishRarity.RARE, "DIAMOND", 1, 1.0);
        when(treasureManager.getAll()).thenReturn(List.of(treasure));

        FishingCatchEngine engine = new FishingCatchEngine(speciesManager, treasureManager, junkManager,
                conditionsResolver, 1.0, 0.0);

        CatchResult result = engine.resolveCatch(player, hookLocation, null, null);

        assertEquals(CatchResult.CatchOutcome.TREASURE, result.outcome());
        assertEquals(treasure, result.treasure());
    }

    @Test
    void alwaysReturnsJunkWhenJunkChanceCoversFullRangeAndTreasureChanceIsZero() {
        Junk junk = new Junk("boot", null, null, null, 1.0);
        when(junkManager.getAll()).thenReturn(List.of(junk));

        FishingCatchEngine engine = new FishingCatchEngine(speciesManager, treasureManager, junkManager,
                conditionsResolver, 0.0, 1.0);

        CatchResult result = engine.resolveCatch(player, hookLocation, null, null);

        assertEquals(CatchResult.CatchOutcome.JUNK, result.outcome());
        assertEquals(junk, result.junk());
    }

    @Test
    void fallsBackToJunkWhenNoSpeciesIsEligibleForCurrentConditions() {
        FishSpecies oceanOnly = new FishSpecies("tuna", "Tuna", null, 0, null, FishCategory.SALTWATER,
                FishRarity.COMMON, Set.of(WaterType.OCEAN), Set.of(), Set.of(), 1, 5, 10, 50, 10, 5,
                FishBehaviorType.SLOW, Set.of(), Set.of(), Set.of(), Set.of(), false, 0, false, null, null, null);

        Junk junk = new Junk("boot", null, null, null, 1.0);
        when(speciesManager.getAll()).thenReturn(List.of(oceanOnly));
        when(junkManager.getAll()).thenReturn(List.of(junk));

        FishingCatchEngine engine = new FishingCatchEngine(speciesManager, treasureManager, junkManager,
                conditionsResolver, 0.0, 0.0);

        CatchResult result = engine.resolveCatch(player, hookLocation, null, null);

        assertEquals(CatchResult.CatchOutcome.JUNK, result.outcome());
    }

    @Test
    void returnsNothingWhenNoEligibleSpeciesAndNoJunkConfigured() {
        when(speciesManager.getAll()).thenReturn(List.of());
        when(junkManager.getAll()).thenReturn(List.of());

        FishingCatchEngine engine = new FishingCatchEngine(speciesManager, treasureManager, junkManager,
                conditionsResolver, 0.0, 0.0);

        CatchResult result = engine.resolveCatch(player, hookLocation, null, null);

        assertEquals(CatchResult.CatchOutcome.NOTHING, result.outcome());
    }

    @Test
    void catchesTheOnlyEligibleSpeciesWithWeightAndLengthWithinItsRange() {
        FishSpecies cod = cod();
        when(speciesManager.getAll()).thenReturn(List.of(cod));

        FishingCatchEngine engine = new FishingCatchEngine(speciesManager, treasureManager, junkManager,
                conditionsResolver, 0.0, 0.0);

        for (int i = 0; i < 50; i++) {
            CatchResult result = engine.resolveCatch(player, hookLocation, null, null);

            assertEquals(CatchResult.CatchOutcome.FISH, result.outcome());
            assertEquals(cod, result.species());
            assertTrue(result.weight() >= cod.minWeight() && result.weight() <= cod.maxWeight());
            assertTrue(result.length() >= cod.minLength() && result.length() <= cod.maxLength());
        }
    }

    @Test
    void highRodPrecisionGuaranteesMasterworkQualityAndItsPriceMultiplier() {
        FishSpecies cod = cod();
        when(speciesManager.getAll()).thenReturn(List.of(cod));

        // score = random(0,100) + precision*10 ; precision high enough to reach >=95 regardless of the roll.
        FishingRod preciseRod = new FishingRod("master", null, null, null, 64, 1, 1, 100, 1, 1, Set.of());

        FishingCatchEngine engine = new FishingCatchEngine(speciesManager, treasureManager, junkManager,
                conditionsResolver, 0.0, 0.0);

        CatchResult result = engine.resolveCatch(player, hookLocation, preciseRod, null);

        assertEquals(CatchQuality.MASTERWORK, result.quality());
        assertEquals((int) Math.round(cod.baseExperience() * CatchQuality.MASTERWORK.priceMultiplier()),
                result.experience());
    }

    @Test
    void legendarySpeciesWithNoLevelRequirementAndRPGRollAPIUnreadyIsStillEligible() {
        FishSpecies legendary = new FishSpecies("kraken", "Kraken", null, 0, null, FishCategory.LEGENDARY,
                FishRarity.LEGENDARY, Set.of(WaterType.LAKE), Set.of(), Set.of(), 50, 100, 100, 200, 1000, 500,
                FishBehaviorType.ELUSIVE, Set.of(), Set.of(), Set.of(), Set.of(), true, 0, false, null, null, null);

        when(speciesManager.getAll()).thenReturn(List.of(legendary));

        FishingCatchEngine engine = new FishingCatchEngine(speciesManager, treasureManager, junkManager,
                conditionsResolver, 0.0, 0.0);

        CatchResult result = engine.resolveCatch(player, hookLocation, null, null);

        assertEquals(CatchResult.CatchOutcome.FISH, result.outcome());
        assertEquals(legendary, result.species());
    }

    @Test
    void legendarySpeciesWithLevelRequirementIsIneligibleWhenRPGRollAPIIsNotReady() {
        FishSpecies legendary = new FishSpecies("kraken", "Kraken", null, 0, null, FishCategory.LEGENDARY,
                FishRarity.LEGENDARY, Set.of(WaterType.LAKE), Set.of(), Set.of(), 50, 100, 100, 200, 1000, 500,
                FishBehaviorType.ELUSIVE, Set.of(), Set.of(), Set.of(), Set.of(), true, 10, false, null, null, null);

        Junk junk = new Junk("boot", null, null, null, 1.0);
        when(speciesManager.getAll()).thenReturn(List.of(legendary));
        when(junkManager.getAll()).thenReturn(List.of(junk));

        FishingCatchEngine engine = new FishingCatchEngine(speciesManager, treasureManager, junkManager,
                conditionsResolver, 0.0, 0.0);

        CatchResult result = engine.resolveCatch(player, hookLocation, null, null);

        assertEquals(CatchResult.CatchOutcome.JUNK, result.outcome());
    }

}
