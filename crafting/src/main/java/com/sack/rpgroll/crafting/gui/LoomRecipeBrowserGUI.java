package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.crafting.ingredient.IngredientType;
import com.sack.rpgroll.crafting.loom.LoomRecipeDefinition;
import com.sack.rpgroll.crafting.loom.LoomRecipeManager;
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

public class LoomRecipeBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final LoomRecipeManager recipeManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<LoomRecipeDefinition> recipes;

    public LoomRecipeBrowserGUI(Player player, LoomRecipeManager recipeManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.loom_recipe.browser_title"), NamedTextColor.GOLD), SIZE);
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

            LoomRecipeDefinition recipe = recipes.get(i);

            setItem(i, new ItemBuilder(Material.LOOM)
                    .setName(Component.text(recipe.id(), NamedTextColor.YELLOW))
                    .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.loom_recipe.banner_lore", "value", recipe.bannerIngredient().value()), NamedTextColor.GRAY),
                            ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.loom_recipe.dye_lore", "value", recipe.dyeIngredient().value()), NamedTextColor.GRAY),
                            ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.loom_recipe.result_lore", "value", recipe.result().value()), NamedTextColor.AQUA),
                            ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.click_edit"), NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.loom_recipe.create_new"), NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < recipes.size() && slot < 36) {
            new LoomRecipeEditorGUI(player, recipes.get(slot), recipeManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.loom_recipe.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (recipeManager.exists(id)) {
                player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.loom_recipe.already_exists"), NamedTextColor.RED));
                reopen();
                return;
            }

            IngredientSpec placeholder = new IngredientSpec(IngredientType.MATERIAL, "WHITE_BANNER", 1, null);
            IngredientSpec dyePlaceholder = new IngredientSpec(IngredientType.MATERIAL, "WHITE_DYE", 1, null);
            recipeManager.save(new LoomRecipeDefinition(id, placeholder, dyePlaceholder, null,
                    new RecipeResult(RecipeResultType.MATERIAL, "WHITE_BANNER", 1), List.of()));
            reopen();
        });
    }

    private void reopen() {
        this.recipes = List.copyOf(recipeManager.getAll());
        open();
    }

}
