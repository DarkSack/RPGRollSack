package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.condition.RecipeCondition;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Edita una {@code VanillaRecipeDefinition} completa, incluyendo
 * {@code shape}/{@code key}/{@code ingredients} (vía chat, retipeando la
 * lista/mapa entero de una — más simple que un editor slot-por-slot para
 * datos de forma tan variable). Guardar reregistra la receta en Bukkit al
 * instante, sin esperar a un reinicio del servidor.
 */
public class VanillaRecipeEditorGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private static final int TYPE_SLOT = 10;
    private static final int SHAPE_SLOT = 11;
    private static final int KEY_SLOT = 12;
    private static final int INGREDIENTS_SLOT = 13;
    private static final int TEMPLATE_MATERIAL_SLOT = 14;
    private static final int BASE_MATERIAL_SLOT = 15;
    private static final int COOKING_TIME_SLOT = 19;
    private static final int EXPERIENCE_SLOT = 20;
    private static final int RESULT_TYPE_SLOT = 21;
    private static final int RESULT_VALUE_SLOT = 22;
    private static final int RESULT_AMOUNT_SLOT = 23;
    private static final int CONDITIONS_SLOT = 24;
    private static final int DELETE_SLOT = 31;
    private static final int BACK_SLOT = 49;

    private final VanillaRecipeManager recipeManager;
    private final VanillaRecipeBridge recipeBridge;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private VanillaRecipeDefinition current;

    public VanillaRecipeEditorGUI(Player player, VanillaRecipeDefinition recipe, VanillaRecipeManager recipeManager,
            VanillaRecipeBridge recipeBridge, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.editor_title", "id", recipe.id()), NamedTextColor.GOLD), SIZE);
        this.current = recipe;
        this.recipeManager = recipeManager;
        this.recipeBridge = recipeBridge;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(VanillaRecipeDefinition updated) {
        current = updated;
        recipeManager.save(current);
        recipeBridge.reregister(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(TYPE_SLOT, new ItemBuilder(Material.CRAFTING_TABLE)
                .setName(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.field_type", "value", current.type()), NamedTextColor.AQUA))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.click_cycle_hint"), NamedTextColor.GRAY)).build());

        setItem(SHAPE_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.field_shape", "value", current.shape()), NamedTextColor.YELLOW))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.shape_only_hint"), NamedTextColor.DARK_GRAY),
                        Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.shape_retype_hint"), NamedTextColor.GRAY))
                .build());

        setItem(KEY_SLOT, new ItemBuilder(Material.TRIPWIRE_HOOK)
                .setName(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.field_key", "value", current.key()), NamedTextColor.YELLOW))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.shape_only_hint"), NamedTextColor.DARK_GRAY),
                        Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.key_retype_hint"), NamedTextColor.GRAY))
                .build());

        setItem(INGREDIENTS_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.field_ingredients", "value", current.ingredients()), NamedTextColor.YELLOW))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.ingredients_type_hint"),
                                NamedTextColor.DARK_GRAY),
                        Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.ingredients_retype_hint"), NamedTextColor.GRAY))
                .build());

        setItem(TEMPLATE_MATERIAL_SLOT, new ItemBuilder(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
                .setName(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.field_template", "value", current.templateMaterial()), NamedTextColor.YELLOW))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.smithing_only_hint"), NamedTextColor.DARK_GRAY)).build());

        setItem(BASE_MATERIAL_SLOT, new ItemBuilder(Material.IRON_INGOT)
                .setName(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.field_base", "value", current.baseMaterial()), NamedTextColor.YELLOW))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.smithing_only_hint"), NamedTextColor.DARK_GRAY)).build());

        setItem(COOKING_TIME_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.field_cooking_time", "value", current.cookingTimeTicks()), NamedTextColor.AQUA))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.common.click_inc_dec", "inc", "+20", "dec", "-20"), NamedTextColor.GRAY)).build());

        setItem(EXPERIENCE_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.field_experience", "value", current.experience()), NamedTextColor.GREEN))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.experience_hint"), NamedTextColor.GRAY)).build());

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
                .setLore(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.conditions_crafting_only_hint"), NamedTextColor.DARK_GRAY),
                        Component.text(chatPromptManager.lang().raw("gui.common.click_edit_list"), NamedTextColor.GRAY)).build());

        setItem(DELETE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(Component.text(chatPromptManager.lang().raw("gui.vanilla_recipe.delete"), NamedTextColor.RED)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == TYPE_SLOT) {
            VanillaStationType[] values = VanillaStationType.values();
            int next = (current.type().ordinal() + 1) % values.length;
            replace(withType(values[next]));
        } else if (slot == SHAPE_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.vanilla_recipe.prompt_shape"),
                    value -> replace(withShape(Arrays.asList(value.split(",")))));
        } else if (slot == KEY_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.vanilla_recipe.prompt_key"),
                    value -> replace(withKey(parseKey(value))));
        } else if (slot == INGREDIENTS_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.vanilla_recipe.prompt_ingredients"),
                    value -> replace(withIngredients(Arrays.asList(value.split(",")))));
        } else if (slot == TEMPLATE_MATERIAL_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.vanilla_recipe.prompt_template"),
                    value -> replace(withTemplateMaterial(value.trim())));
        } else if (slot == BASE_MATERIAL_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.vanilla_recipe.prompt_base"),
                    value -> replace(withBaseMaterial(value.trim())));
        } else if (slot == COOKING_TIME_SLOT) {
            replace(withCookingTime(Math.max(20, current.cookingTimeTicks() + sign * 20)));
        } else if (slot == EXPERIENCE_SLOT) {
            replace(withExperience((float) Math.max(0, current.experience() + sign * 0.5)));
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
            recipeBridge.unregister(current.id());
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

    private Map<String, String> parseKey(String raw) {

        Map<String, String> key = new LinkedHashMap<>();

        for (String pair : raw.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && !kv[0].isBlank()) {
                key.put(kv[0].trim(), kv[1].trim().toUpperCase(Locale.ROOT));
            }
        }

        return key;
    }

    private VanillaRecipeDefinition withType(VanillaStationType v) {
        return rebuild(v, current.shape(), current.key(), current.ingredients(), current.templateMaterial(),
                current.baseMaterial(), current.cookingTimeTicks(), current.experience(), current.result(),
                current.conditions());
    }

    private VanillaRecipeDefinition withShape(List<String> v) {
        return rebuild(current.type(), v, current.key(), current.ingredients(), current.templateMaterial(),
                current.baseMaterial(), current.cookingTimeTicks(), current.experience(), current.result(),
                current.conditions());
    }

    private VanillaRecipeDefinition withKey(Map<String, String> v) {
        return rebuild(current.type(), current.shape(), v, current.ingredients(), current.templateMaterial(),
                current.baseMaterial(), current.cookingTimeTicks(), current.experience(), current.result(),
                current.conditions());
    }

    private VanillaRecipeDefinition withIngredients(List<String> v) {
        return rebuild(current.type(), current.shape(), current.key(), v, current.templateMaterial(),
                current.baseMaterial(), current.cookingTimeTicks(), current.experience(), current.result(),
                current.conditions());
    }

    private VanillaRecipeDefinition withTemplateMaterial(String v) {
        return rebuild(current.type(), current.shape(), current.key(), current.ingredients(), v,
                current.baseMaterial(), current.cookingTimeTicks(), current.experience(), current.result(),
                current.conditions());
    }

    private VanillaRecipeDefinition withBaseMaterial(String v) {
        return rebuild(current.type(), current.shape(), current.key(), current.ingredients(),
                current.templateMaterial(), v, current.cookingTimeTicks(), current.experience(), current.result(),
                current.conditions());
    }

    private VanillaRecipeDefinition withCookingTime(int v) {
        return rebuild(current.type(), current.shape(), current.key(), current.ingredients(),
                current.templateMaterial(), current.baseMaterial(), v, current.experience(), current.result(),
                current.conditions());
    }

    private VanillaRecipeDefinition withExperience(float v) {
        return rebuild(current.type(), current.shape(), current.key(), current.ingredients(),
                current.templateMaterial(), current.baseMaterial(), current.cookingTimeTicks(), v, current.result(),
                current.conditions());
    }

    private VanillaRecipeDefinition withResult(RecipeResult v) {
        return rebuild(current.type(), current.shape(), current.key(), current.ingredients(),
                current.templateMaterial(), current.baseMaterial(), current.cookingTimeTicks(), current.experience(),
                v, current.conditions());
    }

    private VanillaRecipeDefinition withConditions(List<RecipeCondition> v) {
        return rebuild(current.type(), current.shape(), current.key(), current.ingredients(),
                current.templateMaterial(), current.baseMaterial(), current.cookingTimeTicks(), current.experience(),
                current.result(), v);
    }

    private VanillaRecipeDefinition rebuild(VanillaStationType type, List<String> shape, Map<String, String> key,
            List<String> ingredients, String templateMaterial, String baseMaterial, int cookingTimeTicks,
            float experience, RecipeResult result, List<RecipeCondition> conditions) {
        return new VanillaRecipeDefinition(current.id(), type, shape, key, ingredients, templateMaterial,
                baseMaterial, cookingTimeTicks, experience, result, conditions);
    }

}
