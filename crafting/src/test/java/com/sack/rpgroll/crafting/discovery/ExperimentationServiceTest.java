package com.sack.rpgroll.crafting.discovery;

import com.sack.rpgroll.crafting.ingredient.IngredientMatcher;
import com.sack.rpgroll.crafting.recipe.CustomRecipe;
import com.sack.rpgroll.crafting.recipe.CustomRecipeManager;
import com.sack.rpgroll.crafting.recipe.RecipeResult;
import com.sack.rpgroll.crafting.recipe.RecipeResultType;
import com.sack.rpgroll.crafting.station.CustomStation;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExperimentationServiceTest {

    private CustomStation stationAllowing(boolean allow) {
        return new CustomStation("forge", null, null, null, 27, null, -1, 0, false, null, null, null, 1, null, 0, 0,
                null, allow);
    }

    private CustomRecipe recipe(String id, List<com.sack.rpgroll.crafting.ingredient.IngredientSpec> ingredients) {
        return new CustomRecipe(id, null, null, "forge", ingredients,
                new RecipeResult(RecipeResultType.MATERIAL, "DIAMOND", 1), null, 0, 0, 0, null, 0, 0, false);
    }

    @Test
    void attemptFailsImmediatelyWhenStationDoesNotAllowExperimentation() {
        CustomRecipeManager recipeManager = mock(CustomRecipeManager.class);
        DiscoveryService discoveryService = mock(DiscoveryService.class);
        IngredientMatcher matcher = new IngredientMatcher(null);
        ExperimentationService service = new ExperimentationService(recipeManager, discoveryService, matcher, 1.0,
                0.1, 0);

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        ExperimentationOutcome outcome = service.attempt(player, stationAllowing(false), mock(Inventory.class));

        assertEquals(ExperimentationResult.NOT_ALLOWED, outcome.result());
    }

    @Test
    void attemptFailsWhenOnCooldown() {
        CustomRecipeManager recipeManager = mock(CustomRecipeManager.class);
        DiscoveryService discoveryService = mock(DiscoveryService.class);
        IngredientMatcher matcher = new IngredientMatcher(null);
        ExperimentationService service = new ExperimentationService(recipeManager, discoveryService, matcher, 1.0,
                0.1, 60);

        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);

        CustomStation station = stationAllowing(true);
        Inventory inventory = mock(Inventory.class);
        when(inventory.getContents()).thenReturn(new ItemStack[0]);
        when(discoveryService.get(playerId)).thenReturn(PlayerDiscovery.empty(playerId));
        when(recipeManager.byStation("forge")).thenReturn(List.of());

        service.attempt(player, station, inventory);
        ExperimentationOutcome second = service.attempt(player, station, inventory);

        assertEquals(ExperimentationResult.ON_COOLDOWN, second.result());
    }

    @Test
    void attemptFailsWhenAllRecipesAlreadyDiscovered() {
        CustomRecipeManager recipeManager = mock(CustomRecipeManager.class);
        DiscoveryService discoveryService = mock(DiscoveryService.class);
        IngredientMatcher matcher = new IngredientMatcher(null);
        ExperimentationService service = new ExperimentationService(recipeManager, discoveryService, matcher, 1.0,
                0.1, 0);

        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);

        CustomStation station = stationAllowing(true);
        CustomRecipe recipe = recipe("known-recipe", List.of());

        when(recipeManager.byStation("forge")).thenReturn(List.of(recipe));
        PlayerDiscovery discovery = new PlayerDiscovery(playerId, java.util.Set.of("known-recipe"));
        when(discoveryService.get(playerId)).thenReturn(discovery);

        ExperimentationOutcome outcome = service.attempt(player, station, mock(Inventory.class));

        assertEquals(ExperimentationResult.ALREADY_DISCOVERED_ALL, outcome.result());
    }

    @Test
    void attemptFailsWithNoMatchWhenOverlapBelowMinimum() {
        CustomRecipeManager recipeManager = mock(CustomRecipeManager.class);
        DiscoveryService discoveryService = mock(DiscoveryService.class);
        IngredientMatcher matcher = new IngredientMatcher(null);
        ExperimentationService service = new ExperimentationService(recipeManager, discoveryService, matcher, 1.0,
                0.99, 0);

        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);

        CustomStation station = stationAllowing(true);
        CustomRecipe recipe = recipe("unknown-recipe",
                List.of(com.sack.rpgroll.crafting.ingredient.IngredientSpec.material("IRON_INGOT", 1),
                        com.sack.rpgroll.crafting.ingredient.IngredientSpec.material("GOLD_INGOT", 1)));

        when(recipeManager.byStation("forge")).thenReturn(List.of(recipe));
        when(discoveryService.get(playerId)).thenReturn(PlayerDiscovery.empty(playerId));

        Inventory inventory = mock(Inventory.class);
        when(inventory.getContents()).thenReturn(new ItemStack[0]);

        ExperimentationOutcome outcome = service.attempt(player, station, inventory);

        assertEquals(ExperimentationResult.NO_MATCH, outcome.result());
    }
}
