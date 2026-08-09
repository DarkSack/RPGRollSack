package com.sack.rpgroll.crafting.api;

import com.sack.rpgroll.crafting.anvil.AnvilRecipeManager;
import com.sack.rpgroll.crafting.brewing.BrewRecipeManager;
import com.sack.rpgroll.crafting.discovery.DiscoveryService;
import com.sack.rpgroll.crafting.fuel.FuelManager;
import com.sack.rpgroll.crafting.recipe.CustomRecipeManager;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.crafting.station.runtime.StationRuntime;
import com.sack.rpgroll.crafting.station.runtime.StationRuntimeRegistry;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeManager;

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
    private final DiscoveryService discovery;
    private final StationRuntimeRegistry stationRuntimes;

    private CraftingAPI(CustomRecipeManager customRecipes, CustomStationManager customStations, FuelManager fuels,
            VanillaRecipeManager vanillaRecipes, AnvilRecipeManager anvilRecipes, BrewRecipeManager brewRecipes,
            DiscoveryService discovery, StationRuntimeRegistry stationRuntimes) {
        this.customRecipes = customRecipes;
        this.customStations = customStations;
        this.fuels = fuels;
        this.vanillaRecipes = vanillaRecipes;
        this.anvilRecipes = anvilRecipes;
        this.brewRecipes = brewRecipes;
        this.discovery = discovery;
        this.stationRuntimes = stationRuntimes;
    }

    public static void init(CustomRecipeManager customRecipes, CustomStationManager customStations, FuelManager fuels,
            VanillaRecipeManager vanillaRecipes, AnvilRecipeManager anvilRecipes, BrewRecipeManager brewRecipes,
            DiscoveryService discovery, StationRuntimeRegistry stationRuntimes) {
        instance = new CraftingAPI(customRecipes, customStations, fuels, vanillaRecipes, anvilRecipes, brewRecipes,
                discovery, stationRuntimes);
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

    public DiscoveryService discovery() {
        return discovery;
    }

    public Optional<StationRuntime> stationAt(String world, int x, int y, int z) {
        return stationRuntimes.get(world, x, y, z);
    }

}
