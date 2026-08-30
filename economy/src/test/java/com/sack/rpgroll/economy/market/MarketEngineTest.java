package com.sack.rpgroll.economy.market;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketEngineTest {

    @Mock
    private MarketProductManager productManager;

    @Mock
    private MarketStateStore stateStore;

    @Mock
    private MarketRegionManager regionManager;

    private MarketEngine engine;

    private MarketProduct ironIngot;

    @BeforeEach
    void setUp() {
        lenient().when(stateStore.loadAll()).thenReturn(Map.of());
        engine = new MarketEngine(productManager, stateStore, regionManager);

        ironIngot = new MarketProduct("IRON_INGOT", "Iron Ingot", null, null, 100, 10, 1000, 0, 0, 0.25, 0, "mineral",
                null);

        lenient().when(productManager.get(anyString())).thenReturn(Optional.of(ironIngot));
        lenient().when(productManager.getAll()).thenReturn(List.of(ironIngot));
    }

    @Test
    void priceWithNoActivityEqualsBasePrice() {
        assertEquals(100.0, engine.price(ironIngot));
    }

    @Test
    void priceByUnknownProductIdReturnsZero() {
        when(productManager.get("UNKNOWN")).thenReturn(Optional.empty());
        assertEquals(0, engine.price("UNKNOWN"));
    }

    @Test
    void priceByKnownProductIdDelegatesToProductOverload() {
        assertEquals(100.0, engine.price("IRON_INGOT"));
    }

    @Test
    void recordSellLowersPriceViaSupplyPressure() {
        engine.recordSell("IRON_INGOT", 250);
        // pressure = -250*1 / 100 = -2.5 ; price = 100 * (1 - 2.5*0.25) = 37.5
        assertEquals(37.5, engine.price(ironIngot), 0.0001);
    }

    @Test
    void recordBuyRaisesPriceViaDemandPressure() {
        engine.recordBuy("IRON_INGOT", 250);
        // pressure = 250*1 / 100 = 2.5 ; price = 100 * (1 + 2.5*0.25) = 162.5
        assertEquals(162.5, engine.price(ironIngot), 0.0001);
    }

    @Test
    void priceClampsToMinPriceOnExtremeSupply() {
        engine.recordSell("IRON_INGOT", 100_000);
        assertEquals(10.0, engine.price(ironIngot));
    }

    @Test
    void priceClampsToMaxPriceOnExtremeDemand() {
        engine.recordBuy("IRON_INGOT", 100_000);
        assertEquals(1000.0, engine.price(ironIngot));
    }

    @Test
    void runRecoveryDecaysAccumulatorsTowardsZero() {
        MarketProduct decaying = new MarketProduct("GOLD_INGOT", null, null, null, 100, 1, 1000, 0, 0, 0.25, 0.5,
                null, null);
        when(productManager.getAll()).thenReturn(List.of(decaying));

        engine.recordSell("GOLD_INGOT", 100);
        double priceBeforeRecovery = engine.price(decaying);

        engine.runRecovery();
        double priceAfterRecovery = engine.price(decaying);

        assertTrue(priceAfterRecovery > priceBeforeRecovery);
    }

    @Test
    void runRecoveryDoesNothingWhenAccumulatorsAreAlreadyNearZero() {
        engine.runRecovery();
        assertEquals(100.0, engine.price(ironIngot));
    }

    @Test
    void priceWithLocationButNoRegionManagerBehavesLikeNoLocation() {
        MarketEngine engineWithoutRegions = new MarketEngine(productManager, stateStore, null);
        World world = org.mockito.Mockito.mock(World.class);
        lenient().when(world.getName()).thenReturn("world");
        Location location = new Location(world, 0, 64, 0);

        assertEquals(100.0, engineWithoutRegions.price(ironIngot, location));
    }

    @Test
    void priceWithLocationAppliesMatchingRegionModifier() {
        World world = org.mockito.Mockito.mock(World.class);
        when(world.getName()).thenReturn("world");
        Location location = new Location(world, 5, 64, 5);

        MarketRegion miningTown = new MarketRegion("mining-town", null, "world", 0, 0, 0, 10, 128, 10,
                Map.of("mineral", 0.5), Map.of());

        when(regionManager.resolve("world", 5.0, 64.0, 5.0)).thenReturn(Optional.of(miningTown));

        assertEquals(50.0, engine.price(ironIngot, location));
    }

    @Test
    void priceWithLocationOutsideAnyRegionIsUnaffected() {
        World world = org.mockito.Mockito.mock(World.class);
        when(world.getName()).thenReturn("world");
        Location location = new Location(world, 5, 64, 5);

        when(regionManager.resolve(anyString(), anyDouble(), anyDouble(), anyDouble())).thenReturn(Optional.empty());

        assertEquals(100.0, engine.price(ironIngot, location));
    }

    @Test
    void priceWithLocationWhereWorldIsNullSkipsRegionLookup() {
        Location location = new Location(null, 5, 64, 5);
        assertEquals(100.0, engine.price(ironIngot, location));
    }

    @Test
    void stateOfCreatesNewStateForUnseenProduct() {
        MarketState state = engine.stateOf("NEW_PRODUCT");
        assertEquals("NEW_PRODUCT", state.productId());
        assertEquals(0.0, state.supplyAccumulator());
        assertEquals(0.0, state.demandAccumulator());
    }

}
