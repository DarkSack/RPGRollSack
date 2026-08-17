package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.recipe.RecipeResult;
import com.sack.rpgroll.crafting.recipe.RecipeResultType;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeBridge;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeDefinition;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeManager;
import com.sack.rpgroll.crafting.vanilla.VanillaStationType;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VanillaRecipeBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final VanillaRecipeManager recipeManager;
    private final VanillaRecipeBridge recipeBridge;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<VanillaRecipeDefinition> recipes;

    public VanillaRecipeBrowserGUI(Player player, VanillaRecipeManager recipeManager, VanillaRecipeBridge recipeBridge,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.browser_title"), NamedTextColor.GOLD), SIZE);
        this.recipeManager = recipeManager;
        this.recipeBridge = recipeBridge;
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

            VanillaRecipeDefinition recipe = recipes.get(i);

            setItem(i, new ItemBuilder(Material.CRAFTING_TABLE)
                    .setName(Component.text(recipe.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.type_lore", "value", recipe.type()), NamedTextColor.AQUA),
                            Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.result_lore", "value", recipe.result().value()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.common.click_edit"), NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.create_new"), NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < recipes.size() && slot < 36) {
            new VanillaRecipeEditorGUI(player, recipes.get(slot), recipeManager, recipeBridge, chatPromptManager,
                    this::reopen).open();
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
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.vanilla_recipe.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (recipeManager.exists(id)) {
                player.sendMessage(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.already_exists"), NamedTextColor.RED));
                reopen();
                return;
            }

            VanillaRecipeDefinition created = new VanillaRecipeDefinition(id, VanillaStationType.CRAFTING_TABLE_SHAPELESS,
                    List.of(), Map.of(), List.of("PAPER"), null, null, 200, 0,
                    new RecipeResult(RecipeResultType.MATERIAL, "PAPER", 1), List.of());

            recipeManager.save(created);
            recipeBridge.reregister(created);
            reopen();
        });
    }

    private void reopen() {
        this.recipes = List.copyOf(recipeManager.getAll());
        open();
    }

}
