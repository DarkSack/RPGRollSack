package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.anvil.AnvilRecipeDefinition;
import com.sack.rpgroll.crafting.anvil.AnvilRecipeManager;
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

/** Edita una {@code AnvilRecipeDefinition} completa. */
public class AnvilRecipeEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int BASE_SLOT = 10;
    private static final int ADDITION_SLOT = 11;
    private static final int RESULT_TYPE_SLOT = 12;
    private static final int RESULT_VALUE_SLOT = 13;
    private static final int RESULT_AMOUNT_SLOT = 14;
    private static final int REPAIR_COST_SLOT = 19;
    private static final int CONDITIONS_SLOT = 20;
    private static final int DELETE_SLOT = 31;
    private static final int BACK_SLOT = 40;

    private final AnvilRecipeManager recipeManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private AnvilRecipeDefinition current;

    public AnvilRecipeEditorGUI(Player player, AnvilRecipeDefinition recipe, AnvilRecipeManager recipeManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Receta de yunque: " + recipe.id(), NamedTextColor.GOLD), SIZE);
        this.current = recipe;
        this.recipeManager = recipeManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(AnvilRecipeDefinition updated) {
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

        setItem(BASE_SLOT, new ItemBuilder(Material.ANVIL)
                .setName(Component.text("Base: " + IngredientSpecFormat.format(current.baseIngredient()), NamedTextColor.AQUA))
                .setLore(Component.text("Click para retipear", NamedTextColor.GRAY)).build());

        setItem(ADDITION_SLOT, new ItemBuilder(Material.NETHERITE_INGOT)
                .setName(Component.text("Adición: " + IngredientSpecFormat.format(current.additionIngredient()),
                        NamedTextColor.AQUA))
                .setLore(Component.text("Click para retipear", NamedTextColor.GRAY)).build());

        setItem(RESULT_TYPE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(Component.text("Result. tipo: " + current.result().type(), NamedTextColor.AQUA))
                .setLore(Component.text("Click para alternar MATERIAL/ITEM_ID", NamedTextColor.GRAY)).build());

        setItem(RESULT_VALUE_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text("Result. valor: " + current.result().value(), NamedTextColor.YELLOW)).build());

        setItem(RESULT_AMOUNT_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(Component.text("Result. cantidad: " + current.result().amount(), NamedTextColor.AQUA))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY)).build());

        setItem(REPAIR_COST_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text("Costo (niveles): " + current.repairCostLevels(), NamedTextColor.GREEN))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY)).build());

        setItem(CONDITIONS_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(Component.text("Condiciones (" + current.conditions().size() + ")", NamedTextColor.AQUA))
                .setLore(Component.text("Click para editar la lista", NamedTextColor.GRAY)).build());

        setItem(DELETE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(Component.text("Eliminar receta", NamedTextColor.RED)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == BASE_SLOT) {
            promptIngredient("base", this::withBase);
        } else if (slot == ADDITION_SLOT) {
            promptIngredient("adición", this::withAddition);
        } else if (slot == RESULT_TYPE_SLOT) {
            RecipeResultType next = current.result().type() == RecipeResultType.MATERIAL
                    ? RecipeResultType.ITEM_ID : RecipeResultType.MATERIAL;
            replace(withResult(new RecipeResult(next, current.result().value(), current.result().amount())));
        } else if (slot == RESULT_VALUE_SLOT) {
            chatPromptManager.prompt(player, "Escribí el material o id de ítem del resultado:", value -> replace(
                    withResult(new RecipeResult(current.result().type(), value, current.result().amount()))));
        } else if (slot == RESULT_AMOUNT_SLOT) {
            replace(withResult(new RecipeResult(current.result().type(), current.result().value(),
                    Math.max(1, current.result().amount() + sign))));
        } else if (slot == REPAIR_COST_SLOT) {
            replace(withRepairCost(Math.max(0, current.repairCostLevels() + sign)));
        } else if (slot == CONDITIONS_SLOT) {
            new RecipeConditionsEditorGUI(player, "Condiciones: " + current.id(), current.conditions(),
                    chatPromptManager, this::replaceConditions, this::reopen).open();
        } else if (slot == DELETE_SLOT) {
            recipeManager.delete(current.id());
            onBack.run();
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptIngredient(String label, java.util.function.Function<IngredientSpec, AnvilRecipeDefinition> withFn) {
        chatPromptManager.prompt(player, "Escribí " + label + ": TIPO VALOR CANTIDAD [CALIDAD-MINIMA]", raw -> {

            IngredientSpec spec;
            try {
                spec = IngredientSpecFormat.parse(raw);
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

    private AnvilRecipeDefinition withBase(IngredientSpec v) {
        return rebuild(v, current.additionIngredient(), current.result(), current.repairCostLevels(),
                current.conditions());
    }

    private AnvilRecipeDefinition withAddition(IngredientSpec v) {
        return rebuild(current.baseIngredient(), v, current.result(), current.repairCostLevels(),
                current.conditions());
    }

    private AnvilRecipeDefinition withResult(RecipeResult v) {
        return rebuild(current.baseIngredient(), current.additionIngredient(), v, current.repairCostLevels(),
                current.conditions());
    }

    private AnvilRecipeDefinition withRepairCost(int v) {
        return rebuild(current.baseIngredient(), current.additionIngredient(), current.result(), v,
                current.conditions());
    }

    private AnvilRecipeDefinition withConditions(List<RecipeCondition> v) {
        return rebuild(current.baseIngredient(), current.additionIngredient(), current.result(),
                current.repairCostLevels(), v);
    }

    private AnvilRecipeDefinition rebuild(IngredientSpec base, IngredientSpec addition, RecipeResult result,
            int repairCostLevels, List<RecipeCondition> conditions) {
        return new AnvilRecipeDefinition(current.id(), base, addition, result, repairCostLevels, conditions);
    }

}
