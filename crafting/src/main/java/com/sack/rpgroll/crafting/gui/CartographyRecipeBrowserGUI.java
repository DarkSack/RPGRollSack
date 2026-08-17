package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.cartography.CartographyRecipeDefinition;
import com.sack.rpgroll.crafting.cartography.CartographyRecipeManager;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.crafting.ingredient.IngredientType;
import com.sack.rpgroll.crafting.recipe.RecipeResult;
import com.sack.rpgroll.crafting.recipe.RecipeResultType;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class CartographyRecipeBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final CartographyRecipeManager recipeManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<CartographyRecipeDefinition> recipes;

    public CartographyRecipeBrowserGUI(Player player, CartographyRecipeManager recipeManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.cartography_recipe.browser_title"), NamedTextColor.GOLD), SIZE);
        this.recipeManager = recipeManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.recipes = List.copyOf(recipeManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < recipes.size() && i < 36; i++) {

            CartographyRecipeDefinition recipe = recipes.get(i);

            setItem(i, new ItemBuilder(Material.CARTOGRAPHY_TABLE)
                    .setName(Component.text(recipe.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text(chatPromptManager.lang().raw("gui.cartography_recipe.map_lore", "value", recipe.mapIngredient().value()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.cartography_recipe.item_lore", "value", recipe.itemIngredient().value()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.cartography_recipe.result_lore", "value", recipe.result().value()), NamedTextColor.AQUA),
                            Component.text(chatPromptManager.lang().raw("gui.common.click_edit"), NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text(chatPromptManager.lang().raw("gui.cartography_recipe.create_new"), NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < recipes.size() && slot < 36) {
            new CartographyRecipeEditorGUI(player, recipes.get(slot), recipeManager, chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.cartography_recipe.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (recipeManager.exists(id)) {
                player.sendMessage(Component.text(chatPromptManager.lang().raw("gui.cartography_recipe.already_exists"), NamedTextColor.RED));
                reopen();
                return;
            }

            IngredientSpec placeholder = new IngredientSpec(IngredientType.MATERIAL, "PAPER", 1, null);
            recipeManager.save(new CartographyRecipeDefinition(id, placeholder, placeholder,
                    new RecipeResult(RecipeResultType.MATERIAL, "PAPER", 1), List.of()));
            reopen();
        });
    }

    private void reopen() {
        this.recipes = List.copyOf(recipeManager.getAll());
        open();
    }

}
