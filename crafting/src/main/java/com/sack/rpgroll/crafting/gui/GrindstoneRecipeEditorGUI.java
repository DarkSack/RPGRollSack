package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.grindstone.GrindstoneRecipeDefinition;
import com.sack.rpgroll.crafting.grindstone.GrindstoneRecipeManager;
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
import java.util.function.Function;

/** Edita una {@code GrindstoneRecipeDefinition} completa. */
public class GrindstoneRecipeEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int UPPER_SLOT = 10;
    private static final int LOWER_SLOT = 11;
    private static final int RESULT_TYPE_SLOT = 12;
    private static final int RESULT_VALUE_SLOT = 13;
    private static final int RESULT_AMOUNT_SLOT = 14;
    private static final int CONDITIONS_SLOT = 20;
    private static final int DELETE_SLOT = 31;
    private static final int BACK_SLOT = 40;

    private final GrindstoneRecipeManager recipeManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private GrindstoneRecipeDefinition current;

    public GrindstoneRecipeEditorGUI(Player player, GrindstoneRecipeDefinition recipe, GrindstoneRecipeManager recipeManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.grindstone_recipe.editor_title", "id", recipe.id()), NamedTextColor.GOLD), SIZE);
        this.current = recipe;
        this.recipeManager = recipeManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(GrindstoneRecipeDefinition updated) {
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

        setItem(UPPER_SLOT, new ItemBuilder(Material.GRINDSTONE)
                .setName(Component.text(chatPromptManager.lang().raw("gui.grindstone_recipe.field_upper", "value", IngredientSpecFormat.format(current.upperIngredient())), NamedTextColor.AQUA))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.common.click_retype"), NamedTextColor.GRAY)).build());

        setItem(LOWER_SLOT, new ItemBuilder(Material.IRON_INGOT)
                .setName(Component.text(chatPromptManager.lang().raw("gui.grindstone_recipe.field_lower", "value", IngredientSpecFormat.format(current.lowerIngredient())), NamedTextColor.AQUA))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.common.click_retype"), NamedTextColor.GRAY)).build());

        setItem(RESULT_TYPE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(Component.text(chatPromptManager.lang().raw("gui.common.result_type", "value", current.result().type()), NamedTextColor.AQUA))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.common.toggle_material_item_hint"), NamedTextColor.GRAY)).build());

        setItem(RESULT_VALUE_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text(chatPromptManager.lang().raw("gui.common.result_value", "value", current.result().value()), NamedTextColor.YELLOW)).build());

        setItem(RESULT_AMOUNT_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(Component.text(chatPromptManager.lang().raw("gui.common.result_amount", "value", current.result().amount()), NamedTextColor.AQUA))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.common.click_inc_dec", "inc", "+1", "dec", "-1"), NamedTextColor.GRAY)).build());

        setItem(CONDITIONS_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(Component.text(chatPromptManager.lang().raw("gui.common.conditions_count", "count", current.conditions().size()), NamedTextColor.AQUA))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.common.click_edit_list"), NamedTextColor.GRAY)).build());

        setItem(DELETE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(Component.text(chatPromptManager.lang().raw("gui.grindstone_recipe.delete"), NamedTextColor.RED)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == UPPER_SLOT) {
            promptIngredient(chatPromptManager.lang().raw("gui.grindstone_recipe.label_upper"), this::withUpper);
        } else if (slot == LOWER_SLOT) {
            promptIngredient(chatPromptManager.lang().raw("gui.grindstone_recipe.label_lower"), this::withLower);
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

    private void promptIngredient(String label, Function<IngredientSpec, GrindstoneRecipeDefinition> withFn) {
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.common.prompt_ingredient", "label", label), raw -> {

            IngredientSpec spec;
            try {
                spec = IngredientSpecFormat.parse(raw, chatPromptManager.lang());
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
                return;
            }

            replace(withFn.apply(spec));
        });
    }

    private void replaceConditions(List<RecipeCondition> updated) {
        replace(withConditions(updated));
    }

    private void reopen() {
        open();
    }

    private GrindstoneRecipeDefinition withUpper(IngredientSpec v) {
        return rebuild(v, current.lowerIngredient(), current.result(), current.conditions());
    }

    private GrindstoneRecipeDefinition withLower(IngredientSpec v) {
        return rebuild(current.upperIngredient(), v, current.result(), current.conditions());
    }

    private GrindstoneRecipeDefinition withResult(RecipeResult v) {
        return rebuild(current.upperIngredient(), current.lowerIngredient(), v, current.conditions());
    }

    private GrindstoneRecipeDefinition withConditions(List<RecipeCondition> v) {
        return rebuild(current.upperIngredient(), current.lowerIngredient(), current.result(), v);
    }

    private GrindstoneRecipeDefinition rebuild(IngredientSpec upper, IngredientSpec lower, RecipeResult result,
            List<RecipeCondition> conditions) {
        return new GrindstoneRecipeDefinition(current.id(), upper, lower, result, conditions);
    }

}
