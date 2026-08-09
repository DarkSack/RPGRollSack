package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.recipe.CustomRecipe;
import com.sack.rpgroll.crafting.recipe.CustomRecipeManager;
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

public class CustomRecipeBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final CustomRecipeManager recipeManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<CustomRecipe> recipes;

    public CustomRecipeBrowserGUI(Player player, CustomRecipeManager recipeManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text("Recetas personalizadas", NamedTextColor.GOLD), SIZE);
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

            CustomRecipe recipe = recipes.get(i);

            setItem(i, new ItemBuilder(parseMaterial(recipe.icon()))
                    .setName(Component.text(recipe.displayName(), NamedTextColor.YELLOW))
                    .setLore(Component.text("id: " + recipe.id(), NamedTextColor.GRAY),
                            Component.text("estación: " + recipe.stationId(), NamedTextColor.AQUA),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear receta nueva", NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < recipes.size() && slot < 36) {
            new CustomRecipeEditorGUI(player, recipes.get(slot), recipeManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id de la nueva receta:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (recipeManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una receta con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            chatPromptManager.prompt(player, "Escribí el id de la estación donde se procesa:", stationId -> {
                recipeManager.save(new CustomRecipe(id, id, "CRAFTING_TABLE", stationId.trim(), List.of(),
                        new RecipeResult(RecipeResultType.MATERIAL, "PAPER", 1), List.of(), 100, 0, 0, null, 0, -1,
                        false));
                reopen();
            });
        });
    }

    static Material parseMaterial(String raw) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.CRAFTING_TABLE;
        }
    }

    private void reopen() {
        this.recipes = List.copyOf(recipeManager.getAll());
        open();
    }

}
