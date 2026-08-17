package com.sack.rpgroll.crafting.api;

import com.sack.rpgroll.crafting.anvil.AnvilRecipeManager;
import com.sack.rpgroll.crafting.brewing.BrewRecipeManager;
import com.sack.rpgroll.crafting.cartography.CartographyRecipeManager;
import com.sack.rpgroll.crafting.discovery.DiscoveryService;
import com.sack.rpgroll.crafting.fuel.FuelManager;
import com.sack.rpgroll.crafting.grindstone.GrindstoneRecipeManager;
import com.sack.rpgroll.crafting.loom.LoomRecipeManager;
import com.sack.rpgroll.crafting.proficiency.ProficiencyService;
import com.sack.rpgroll.crafting.recipe.CustomRecipeManager;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.crafting.station.runtime.StationRuntime;
import com.sack.rpgroll.crafting.station.runtime.StationRuntimeRegistry;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeManager;
import com.sack.rpgroll.crafting.villager.VillagerTradeManager;

import java.util.Optional;

/**
 * Punto de entrada público de RPGRoll-Crafting para otros addons — mismo
 * patrón que {@code EconomyAPI}/{@code GuildsAPI}: inicializado una vez en
 * {@code onEnable} y consultado vía {@link #isReady()}/{@link #get()}.
 */
public final class CraftingAPI {

    private static CraftingAPI instance;

    private final CustomRecipeManager customRecipes;
    private final CustomStationManager customStations;
    private final FuelManager fuels;
    private final VanillaRecipeManager vanillaRecipes;
    private final AnvilRecipeManager anvilRecipes;
    private final BrewRecipeManager brewRecipes;
    private final GrindstoneRecipeManager grindstoneRecipes;
    private final CartographyRecipeManager cartographyRecipes;
    private final LoomRecipeManager loomRecipes;
    private final VillagerTradeManager villagerTrades;
    private final DiscoveryService discovery;
    private final ProficiencyService proficiency;
    private final StationRuntimeRegistry stationRuntimes;

    private CraftingAPI(CustomRecipeManager customRecipes, CustomStationManager customStations, FuelManager fuels,
            VanillaRecipeManager vanillaRecipes, AnvilRecipeManager anvilRecipes, BrewRecipeManager brewRecipes,
            GrindstoneRecipeManager grindstoneRecipes, CartographyRecipeManager cartographyRecipes,
            LoomRecipeManager loomRecipes, VillagerTradeManager villagerTrades, DiscoveryService discovery,
            ProficiencyService proficiency, StationRuntimeRegistry stationRuntimes) {
        this.customRecipes = customRecipes;
        this.customStations = customStations;
        this.fuels = fuels;
        this.vanillaRecipes = vanillaRecipes;
        this.anvilRecipes = anvilRecipes;
        this.brewRecipes = brewRecipes;
        this.grindstoneRecipes = grindstoneRecipes;
        this.cartographyRecipes = cartographyRecipes;
        this.loomRecipes = loomRecipes;
        this.villagerTrades = villagerTrades;
        this.discovery = discovery;
        this.proficiency = proficiency;
        this.stationRuntimes = stationRuntimes;
    }

    public static void init(CustomRecipeManager customRecipes, CustomStationManager customStations, FuelManager fuels,
            VanillaRecipeManager vanillaRecipes, AnvilRecipeManager anvilRecipes, BrewRecipeManager brewRecipes,
            GrindstoneRecipeManager grindstoneRecipes, CartographyRecipeManager cartographyRecipes,
            LoomRecipeManager loomRecipes, VillagerTradeManager villagerTrades, DiscoveryService discovery,
            ProficiencyService proficiency, StationRuntimeRegistry stationRuntimes) {
        instance = new CraftingAPI(customRecipes, customStations, fuels, vanillaRecipes, anvilRecipes, brewRecipes,
                grindstoneRecipes, cartographyRecipes, loomRecipes, villagerTrades, discovery, proficiency, stationRuntimes);
    }

    public static boolean isReady() {
        return instance != null;
    }

    /** @throws IllegalStateException si RPGRoll-Crafting todavía no está listo. */
    public static CraftingAPI get() {

        if (instance == null) {
            throw new IllegalStateException("RPGRoll-Crafting todavía no está listo.");
        }

        return instance;
    }

    public CustomRecipeManager customRecipes() {
        return customRecipes;
    }

    public CustomStationManager customStations() {
        return customStations;
    }

    public FuelManager fuels() {
        return fuels;
    }

    public VanillaRecipeManager vanillaRecipes() {
        return vanillaRecipes;
    }

    public AnvilRecipeManager anvilRecipes() {
        return anvilRecipes;
    }

    public BrewRecipeManager brewRecipes() {
        return brewRecipes;
    }

    public GrindstoneRecipeManager grindstoneRecipes() {
        return grindstoneRecipes;
    }

    public CartographyRecipeManager cartographyRecipes() {
        return cartographyRecipes;
    }

    public LoomRecipeManager loomRecipes() {
        return loomRecipes;
    }

    public VillagerTradeManager villagerTrades() {
        return villagerTrades;
    }

    public DiscoveryService discovery() {
        return discovery;
    }

    public ProficiencyService proficiency() {
        return proficiency;
    }

    public Optional<StationRuntime> stationAt(String world, int x, int y, int z) {
        return stationRuntimes.get(world, x, y, z);
    }

}
