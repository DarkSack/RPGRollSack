package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/** Edita todos los campos de una {@code CustomRecipe}, incluyendo sus listas de ingredientes y condiciones. */
public class CustomRecipeEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int STATION_SLOT = 12;
    private static final int PROCESSING_TIME_SLOT = 13;
    private static final int FUEL_PER_CRAFT_SLOT = 14;
    private static final int XP_SLOT = 19;
    private static final int ECONOMY_COST_SLOT = 20;
    private static final int ECONOMY_CURRENCY_SLOT = 21;
    private static final int FAIL_CHANCE_SLOT = 22;
    private static final int QUALITY_ENABLED_SLOT = 23;
    private static final int RESULT_TYPE_SLOT = 28;
    private static final int RESULT_VALUE_SLOT = 29;
    private static final int RESULT_AMOUNT_SLOT = 30;
    private static final int INGREDIENTS_SLOT = 32;
    private static final int CONDITIONS_SLOT = 33;
    private static final int DELETE_SLOT = 31;
    private static final int BACK_SLOT = 40;

    private final CustomRecipeManager recipeManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private CustomRecipe current;

    public CustomRecipeEditorGUI(Player player, CustomRecipe recipe, CustomRecipeManager recipeManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.recipe.editor_title", "id", recipe.id()), NamedTextColor.GOLD), SIZE);
        this.current = recipe;
        this.recipeManager = recipeManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(CustomRecipe updated) {
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

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(chatPromptManager.lang().component("gui.common.field_name", "value", current.displayName())).build());

        setItem(ICON_SLOT, new ItemBuilder(CustomRecipeBrowserGUI.parseMaterial(current.icon()))
                .setName(Component.text(chatPromptManager.lang().raw("gui.common.field_icon", "value", current.icon()), NamedTextColor.YELLOW)).build());

        setItem(STATION_SLOT, new ItemBuilder(Material.SMITHING_TABLE)
                .setName(Component.text(chatPromptManager.lang().raw("gui.recipe.field_station", "value", current.stationId()), NamedTextColor.AQUA)).build());

        setItem(PROCESSING_TIME_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text(chatPromptManager.lang().raw("gui.recipe.field_processing_time", "value", current.processingTimeTicks()), NamedTextColor.AQUA))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.recipe.time_ticks_hint"), NamedTextColor.GRAY)).build());

        setItem(FUEL_PER_CRAFT_SLOT, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(Component.text(chatPromptManager.lang().raw("gui.recipe.field_fuel_per_craft", "value", current.fuelPerCraft()), NamedTextColor.GOLD))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.recipe.plus1_minus1_hint"), NamedTextColor.GRAY)).build());

        setItem(XP_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text(chatPromptManager.lang().raw("gui.recipe.field_xp", "value", current.xpAmount()), NamedTextColor.GREEN))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.recipe.plus5_minus5_hint"), NamedTextColor.GRAY)).build());

        setItem(ECONOMY_COST_SLOT, new ItemBuilder(Material.GOLD_INGOT)
                .setName(Component.text(chatPromptManager.lang().raw("gui.recipe.field_economy_cost", "value", current.economyCost()), NamedTextColor.GOLD))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.recipe.plus5_minus5_hint"), NamedTextColor.GRAY)).build());

        setItem(ECONOMY_CURRENCY_SLOT, new ItemBuilder(Material.SUNFLOWER)
                .setName(Component.text(chatPromptManager.lang().raw("gui.recipe.field_economy_currency", "value",
                        current.economyCurrencyId() == null ? chatPromptManager.lang().raw("gui.recipe.currency_base") : current.economyCurrencyId()),
                        NamedTextColor.YELLOW)).build());

        setItem(FAIL_CHANCE_SLOT, new ItemBuilder(Material.REDSTONE)
                .setName(Component.text(chatPromptManager.lang().raw("gui.recipe.field_fail_chance", "value",
                        current.failChance() < 0 ? chatPromptManager.lang().raw("gui.recipe.fail_chance_config") : current.failChance()),
                        NamedTextColor.RED))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.recipe.fail_chance_hint"), NamedTextColor.GRAY))
                .build());

        setItem(QUALITY_ENABLED_SLOT, new ItemBuilder(current.qualityEnabled() ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE)
                .setName(Component.text(chatPromptManager.lang().raw("gui.recipe.field_quality_enabled", "value", current.qualityEnabled()), NamedTextColor.GOLD))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.common.click_toggle"), NamedTextColor.GRAY)).build());

        setItem(RESULT_TYPE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(Component.text(chatPromptManager.lang().raw("gui.common.result_type", "value", current.result().type()), NamedTextColor.AQUA))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.common.toggle_material_item_hint"), NamedTextColor.GRAY)).build());

        setItem(RESULT_VALUE_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text(chatPromptManager.lang().raw("gui.common.result_value", "value", current.result().value()), NamedTextColor.YELLOW)).build());

        setItem(RESULT_AMOUNT_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(Component.text(chatPromptManager.lang().raw("gui.common.result_amount", "value", current.result().amount()), NamedTextColor.AQUA))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.common.click_inc_dec", "inc", "+1", "dec", "-1"), NamedTextColor.GRAY)).build());

        setItem(INGREDIENTS_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(Component.text(chatPromptManager.lang().raw("gui.recipe.ingredients_count", "count", current.ingredients().size()), NamedTextColor.AQUA))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.common.click_edit_list"), NamedTextColor.GRAY)).build());

        setItem(CONDITIONS_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(Component.text(chatPromptManager.lang().raw("gui.common.conditions_count", "count", current.conditions().size()), NamedTextColor.AQUA))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.common.click_edit_list"), NamedTextColor.GRAY)).build());

        setItem(DELETE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(Component.text(chatPromptManager.lang().raw("gui.recipe.delete"), NamedTextColor.RED)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.common.prompt_name"), value -> replace(withDisplayName(value)));
        } else if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.common.prompt_icon"), value -> replace(withIcon(value)));
        } else if (slot == STATION_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.recipe.prompt_station"), value -> replace(withStation(value)));
        } else if (slot == PROCESSING_TIME_SLOT) {
            replace(withProcessingTime(Math.max(0, current.processingTimeTicks() + sign * 20)));
        } else if (slot == FUEL_PER_CRAFT_SLOT) {
            replace(withFuelPerCraft(Math.max(0, current.fuelPerCraft() + sign)));
        } else if (slot == XP_SLOT) {
            replace(withXp(Math.max(0, current.xpAmount() + sign * 5)));
        } else if (slot == ECONOMY_COST_SLOT) {
            replace(withEconomyCost(Math.max(0, current.economyCost() + sign * 5)));
        } else if (slot == ECONOMY_CURRENCY_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.recipe.prompt_economy_currency"), value -> replace(
                    withEconomyCurrency(value.equalsIgnoreCase("base") ? null : value)));
        } else if (slot == FAIL_CHANCE_SLOT) {
            double next = current.failChance() < 0 ? 0 : current.failChance() + sign * 0.05;
            replace(withFailChance(next < 0 ? -1 : Math.min(1, next)));
        } else if (slot == QUALITY_ENABLED_SLOT) {
            replace(withQualityEnabled(!current.qualityEnabled()));
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
        } else if (slot == INGREDIENTS_SLOT) {
            new IngredientListEditorGUI(player, chatPromptManager.lang().raw("gui.ingredient_list.title", "id", current.id()), current.ingredients(),
                    chatPromptManager, this::replaceIngredients, this::reopen).open();
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

    private void replaceIngredients(List<IngredientSpec> updated) {
        current = new CustomRecipe(current.id(), current.displayName(), current.icon(), current.stationId(), updated,
                current.result(), current.conditions(), current.processingTimeTicks(), current.fuelPerCraft(),
                current.xpAmount(), current.economyCurrencyId(), current.economyCost(), current.failChance(),
                current.qualityEnabled());
        recipeManager.save(current);
    }

    private void replaceConditions(List<RecipeCondition> updated) {
        current = new CustomRecipe(current.id(), current.displayName(), current.icon(), current.stationId(),
                current.ingredients(), current.result(), updated, current.processingTimeTicks(),
                current.fuelPerCraft(), current.xpAmount(), current.economyCurrencyId(), current.economyCost(),
                current.failChance(), current.qualityEnabled());
        recipeManager.save(current);
    }

    private void reopen() {
        open();
    }

    private CustomRecipe withDisplayName(String v) {
        return rebuild(v, current.icon(), current.stationId(), current.processingTimeTicks(), current.fuelPerCraft(),
                current.xpAmount(), current.economyCurrencyId(), current.economyCost(), current.failChance(),
                current.qualityEnabled(), current.result());
    }

    private CustomRecipe withIcon(String v) {
        return rebuild(current.displayName(), v, current.stationId(), current.processingTimeTicks(),
                current.fuelPerCraft(), current.xpAmount(), current.economyCurrencyId(), current.economyCost(),
                current.failChance(), current.qualityEnabled(), current.result());
    }

    private CustomRecipe withStation(String v) {
        return rebuild(current.displayName(), current.icon(), v.trim(), current.processingTimeTicks(),
                current.fuelPerCraft(), current.xpAmount(), current.economyCurrencyId(), current.economyCost(),
                current.failChance(), current.qualityEnabled(), current.result());
    }

    private CustomRecipe withProcessingTime(int v) {
        return rebuild(current.displayName(), current.icon(), current.stationId(), v, current.fuelPerCraft(),
                current.xpAmount(), current.economyCurrencyId(), current.economyCost(), current.failChance(),
                current.qualityEnabled(), current.result());
    }

    private CustomRecipe withFuelPerCraft(int v) {
        return rebuild(current.displayName(), current.icon(), current.stationId(), current.processingTimeTicks(), v,
                current.xpAmount(), current.economyCurrencyId(), current.economyCost(), current.failChance(),
                current.qualityEnabled(), current.result());
    }

    private CustomRecipe withXp(double v) {
        return rebuild(current.displayName(), current.icon(), current.stationId(), current.processingTimeTicks(),
                current.fuelPerCraft(), v, current.economyCurrencyId(), current.economyCost(), current.failChance(),
                current.qualityEnabled(), current.result());
    }

    private CustomRecipe withEconomyCost(double v) {
        return rebuild(current.displayName(), current.icon(), current.stationId(), current.processingTimeTicks(),
                current.fuelPerCraft(), current.xpAmount(), current.economyCurrencyId(), v, current.failChance(),
                current.qualityEnabled(), current.result());
    }

    private CustomRecipe withEconomyCurrency(String v) {
        return rebuild(current.displayName(), current.icon(), current.stationId(), current.processingTimeTicks(),
                current.fuelPerCraft(), current.xpAmount(), v, current.economyCost(), current.failChance(),
                current.qualityEnabled(), current.result());
    }

    private CustomRecipe withFailChance(double v) {
        return rebuild(current.displayName(), current.icon(), current.stationId(), current.processingTimeTicks(),
                current.fuelPerCraft(), current.xpAmount(), current.economyCurrencyId(), current.economyCost(), v,
                current.qualityEnabled(), current.result());
    }

    private CustomRecipe withQualityEnabled(boolean v) {
        return rebuild(current.displayName(), current.icon(), current.stationId(), current.processingTimeTicks(),
                current.fuelPerCraft(), current.xpAmount(), current.economyCurrencyId(), current.economyCost(),
                current.failChance(), v, current.result());
    }

    private CustomRecipe withResult(RecipeResult v) {
        return rebuild(current.displayName(), current.icon(), current.stationId(), current.processingTimeTicks(),
                current.fuelPerCraft(), current.xpAmount(), current.economyCurrencyId(), current.economyCost(),
                current.failChance(), current.qualityEnabled(), v);
    }

    private CustomRecipe rebuild(String displayName, String icon, String stationId, int processingTimeTicks,
            int fuelPerCraft, double xpAmount, String economyCurrencyId, double economyCost, double failChance,
            boolean qualityEnabled, RecipeResult result) {
        return new CustomRecipe(current.id(), displayName, icon, stationId, current.ingredients(), result,
                current.conditions(), processingTimeTicks, fuelPerCraft, xpAmount, economyCurrencyId, economyCost,
                failChance, qualityEnabled);
    }

}
