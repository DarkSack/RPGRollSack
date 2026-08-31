package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.crafting.brewing.BrewRecipeDefinition;
import com.sack.rpgroll.crafting.brewing.BrewRecipeManager;
import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.crafting.recipe.RecipeResult;
import com.sack.rpgroll.crafting.recipe.RecipeResultType;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/** Edita una {@code BrewRecipeDefinition} completa. */
public class BrewRecipeEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int INGREDIENT_SLOT = 10;
    private static final int RESULT_TYPE_SLOT = 12;
    private static final int RESULT_VALUE_SLOT = 13;
    private static final int RESULT_AMOUNT_SLOT = 14;
    private static final int CONDITIONS_SLOT = 19;
    private static final int DELETE_SLOT = 31;
    private static final int BACK_SLOT = 40;

    private final BrewRecipeManager recipeManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private BrewRecipeDefinition current;

    public BrewRecipeEditorGUI(Player player, BrewRecipeDefinition recipe, BrewRecipeManager recipeManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.brew_recipe.editor_title", "id", recipe.id()), NamedTextColor.GOLD), SIZE);
        this.current = recipe;
        this.recipeManager = recipeManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(BrewRecipeDefinition updated) {
        current = updated;
        recipeManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(INGREDIENT_SLOT, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.brew_recipe.field_ingredient", "value", IngredientSpecFormat.format(current.ingredient())), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.click_retype"), NamedTextColor.GRAY)).build());

        setItem(RESULT_TYPE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.result_type", "value", current.result().type()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.toggle_material_item_hint"), NamedTextColor.GRAY)).build());

        setItem(RESULT_VALUE_SLOT, new ItemBuilder(Material.PAPER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.result_value", "value", current.result().value()), NamedTextColor.YELLOW)).build());

        setItem(RESULT_AMOUNT_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.result_amount", "value", current.result().amount()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.click_inc_dec", "inc", "+1", "dec", "-1"), NamedTextColor.GRAY)).build());

        setItem(CONDITIONS_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.conditions_count", "count", current.conditions().size()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.click_edit_list"), NamedTextColor.GRAY)).build());

        setItem(DELETE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.brew_recipe.delete"), NamedTextColor.RED)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == INGREDIENT_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.ingredient_list.prompt_add"), raw -> {

                IngredientSpec spec;
                try {
                    spec = IngredientSpecFormat.parse(raw, chatPromptManager.lang());
                } catch (IllegalArgumentException e) {
                    player.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
                    return;
                }

                replace(withIngredient(spec));
            });
        } else if (slot == RESULT_TYPE_SLOT) {
            RecipeResultType next = current.result().type() == RecipeResultType.MATERIAL
                    ? RecipeResultType.ITEM_ID : RecipeResultType.MATERIAL;
            replace(withResult(new RecipeResult(next, current.result().value(), current.result().amount())));
        } else if (slot == RESULT_VALUE_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.common.prompt_result_value"), value -> replace(
                    withResult(new RecipeResult(current.result().type(), value, current.result().amount()))));
        } else if (slot == RESULT_AMOUNT_SLOT) {
            replace(withResult(new RecipeResult(current.result().type(), current.result().value(),
                    Math.max(1, current.result().amount() + sign))));
        } else if (slot == CONDITIONS_SLOT) {
            new RecipeConditionsEditorGUI(player, chatPromptManager.lang().raw("gui.conditions.title", "id", current.id()), current.conditions(),
                    chatPromptManager, this::replaceConditions, this::reopen).open();
        } else if (slot == DELETE_SLOT) {
            recipeManager.delete(current.id());
            onBack.run();
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void replaceConditions(List<RecipeCondition> updated) {
        replace(withConditions(updated));
    }

    private void reopen() {
        open();
    }

    private BrewRecipeDefinition withIngredient(IngredientSpec v) {
        return new BrewRecipeDefinition(current.id(), v, current.result(), current.conditions());
    }

    private BrewRecipeDefinition withResult(RecipeResult v) {
        return new BrewRecipeDefinition(current.id(), current.ingredient(), v, current.conditions());
    }

    private BrewRecipeDefinition withConditions(List<RecipeCondition> v) {
        return new BrewRecipeDefinition(current.id(), current.ingredient(), current.result(), v);
    }

}
