package com.sack.rpgroll.crafting.loom;

import com.sack.rpgroll.crafting.condition.ConditionEvaluator;
import com.sack.rpgroll.crafting.ingredient.IngredientMatcher;
import com.sack.rpgroll.crafting.recipe.RecipeResultFactory;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Motor de recetas personalizadas de telar. A diferencia del yunque/piedra
 * de amolar, Bukkit no expone un {@code LoomInventory} con getters de slot
 * ni un evento "prepare" — el layout vanilla es fijo (0=banner, 1=tinte,
 * 2=patrón, 3=resultado), así que se intercepta directamente el click en el
 * slot de resultado: si banner+tinte(+patrón opcional) coinciden con una
 * {@link LoomRecipeDefinition}, se cancela el click vanilla, se consumen los
 * ingredientes y se entrega el resultado personalizado a mano.
 */
public class LoomEngine implements Listener {

    private static final int BANNER_SLOT = 0;
    private static final int DYE_SLOT = 1;
    private static final int PATTERN_SLOT = 2;
    private static final int RESULT_SLOT = 3;

    private final LoomRecipeManager recipeManager;
    private final IngredientMatcher ingredientMatcher;
    private final ConditionEvaluator conditionEvaluator;
    private final RecipeResultFactory resultFactory;

    public LoomEngine(LoomRecipeManager recipeManager, IngredientMatcher ingredientMatcher,
            ConditionEvaluator conditionEvaluator, RecipeResultFactory resultFactory) {
        this.recipeManager = recipeManager;
        this.ingredientMatcher = ingredientMatcher;
        this.conditionEvaluator = conditionEvaluator;
        this.resultFactory = resultFactory;
    }

    @EventHandler
    public void onLoomClick(InventoryClickEvent event) {

        if (event.getInventory().getType() != InventoryType.LOOM || event.getSlot() != RESULT_SLOT
                || event.getClickedInventory() != event.getInventory()) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory inventory = event.getInventory();
        ItemStack banner = inventory.getItem(BANNER_SLOT);
        ItemStack dye = inventory.getItem(DYE_SLOT);
        ItemStack pattern = inventory.getItem(PATTERN_SLOT);

        if (banner == null || dye == null) {
            return;
        }

        for (LoomRecipeDefinition recipe : recipeManager.getAll()) {

            if (!ingredientMatcher.matchesWithAmount(banner, recipe.bannerIngredient())
                    || !ingredientMatcher.matchesWithAmount(dye, recipe.dyeIngredient())) {
                continue;
            }

            if (recipe.hasPatternIngredient() && !ingredientMatcher.matchesWithAmount(pattern, recipe.patternIngredient())) {
                continue;
            }

            if (!recipe.conditions().isEmpty() && !conditionEvaluator.evaluateAll(recipe.conditions(), player)) {
                continue;
            }

            Optional<ItemStack> result = resultFactory.build(recipe.result(), null);
            if (result.isEmpty()) {
                continue;
            }

            event.setCancelled(true);
            consumeOne(inventory, BANNER_SLOT, recipe.bannerIngredient().amount());
            consumeOne(inventory, DYE_SLOT, recipe.dyeIngredient().amount());
            if (recipe.hasPatternIngredient()) {
                consumeOne(inventory, PATTERN_SLOT, recipe.patternIngredient().amount());
            }
            inventory.setItem(RESULT_SLOT, null);

            deliver(player, result.get(), event.getClick());
            return;
        }
    }

    private void consumeOne(Inventory inventory, int slot, int amount) {

        ItemStack stack = inventory.getItem(slot);
        if (stack == null) {
            return;
        }

        int remaining = stack.getAmount() - amount;
        inventory.setItem(slot, remaining > 0 ? withAmount(stack, remaining) : null);
    }

    private ItemStack withAmount(ItemStack stack, int amount) {
        ItemStack copy = stack.clone();
        copy.setAmount(amount);
        return copy;
    }

    private void deliver(Player player, ItemStack result, ClickType click) {

        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
            var leftover = player.getInventory().addItem(result);
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            return;
        }

        ItemStack cursor = player.getItemOnCursor();
        if (cursor == null || cursor.getType().isAir()) {
            player.setItemOnCursor(result);
        } else {
            var leftover = player.getInventory().addItem(result);
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
    }

}
