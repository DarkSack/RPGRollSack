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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Edita los campos escalares de una {@code CustomRecipe} y su
 * {@code result}. {@code ingredients} y {@code conditions} son listas — se
 * editan directamente en el YAML de la receta.
 */
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
    private static final int DELETE_SLOT = 31;
    private static final int BACK_SLOT = 40;

    private final CustomRecipeManager recipeManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private CustomRecipe current;

    public CustomRecipeEditorGUI(Player player, CustomRecipe recipe, CustomRecipeManager recipeManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Receta: " + recipe.id(), NamedTextColor.GOLD), SIZE);
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
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW)).build());

        setItem(ICON_SLOT, new ItemBuilder(CustomRecipeBrowserGUI.parseMaterial(current.icon()))
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW)).build());

        setItem(STATION_SLOT, new ItemBuilder(Material.SMITHING_TABLE)
                .setName(Component.text("Estación: " + current.stationId(), NamedTextColor.AQUA)).build());

        setItem(PROCESSING_TIME_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Tiempo (ticks): " + current.processingTimeTicks(), NamedTextColor.AQUA))
                .setLore(Component.text("Click: +20 · Click derecho: -20", NamedTextColor.GRAY)).build());

        setItem(FUEL_PER_CRAFT_SLOT, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(Component.text("Combustible por crafteo: " + current.fuelPerCraft(), NamedTextColor.GOLD))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY)).build());

        setItem(XP_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text("XP otorgada: " + current.xpAmount(), NamedTextColor.GREEN))
                .setLore(Component.text("Click: +5 · Click derecho: -5", NamedTextColor.GRAY)).build());

        setItem(ECONOMY_COST_SLOT, new ItemBuilder(Material.GOLD_INGOT)
                .setName(Component.text("Costo económico: " + current.economyCost(), NamedTextColor.GOLD))
                .setLore(Component.text("Click: +5 · Click derecho: -5", NamedTextColor.GRAY)).build());

        setItem(ECONOMY_CURRENCY_SLOT, new ItemBuilder(Material.SUNFLOWER)
                .setName(Component.text("Moneda: " + (current.economyCurrencyId() == null ? "(base)" : current.economyCurrencyId()),
                        NamedTextColor.YELLOW)).build());

        setItem(FAIL_CHANCE_SLOT, new ItemBuilder(Material.REDSTONE)
                .setName(Component.text("Prob. de fallo: " + (current.failChance() < 0 ? "(config)" : current.failChance()),
                        NamedTextColor.RED))
                .setLore(Component.text("Click: +0.05 · Click derecho: -0.05 (-1 = usar config)", NamedTextColor.GRAY))
                .build());

        setItem(QUALITY_ENABLED_SLOT, new ItemBuilder(current.qualityEnabled() ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE)
                .setName(Component.text("Calidad habilitada: " + current.qualityEnabled(), NamedTextColor.GOLD))
                .setLore(Component.text("Click para alternar", NamedTextColor.GRAY)).build());

        setItem(RESULT_TYPE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(Component.text("Result. tipo: " + current.result().type(), NamedTextColor.AQUA))
                .setLore(Component.text("Click para alternar MATERIAL/ITEM_ID", NamedTextColor.GRAY)).build());

        setItem(RESULT_VALUE_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text("Result. valor: " + current.result().value(), NamedTextColor.YELLOW)).build());

        setItem(RESULT_AMOUNT_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(Component.text("Result. cantidad: " + current.result().amount(), NamedTextColor.AQUA))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY)).build());

        setItem(DELETE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(Component.text("Eliminar receta", NamedTextColor.RED)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(withDisplayName(value)));
        } else if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material del ícono:", value -> replace(withIcon(value)));
        } else if (slot == STATION_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id de la estación:", value -> replace(withStation(value)));
        } else if (slot == PROCESSING_TIME_SLOT) {
            replace(withProcessingTime(Math.max(0, current.processingTimeTicks() + sign * 20)));
        } else if (slot == FUEL_PER_CRAFT_SLOT) {
            replace(withFuelPerCraft(Math.max(0, current.fuelPerCraft() + sign)));
        } else if (slot == XP_SLOT) {
            replace(withXp(Math.max(0, current.xpAmount() + sign * 5)));
        } else if (slot == ECONOMY_COST_SLOT) {
            replace(withEconomyCost(Math.max(0, current.economyCost() + sign * 5)));
        } else if (slot == ECONOMY_CURRENCY_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id de moneda (o 'base'):", value -> replace(
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
            chatPromptManager.prompt(player, "Escribí el material o id de ítem del resultado:", value -> replace(
                    withResult(new RecipeResult(current.result().type(), value, current.result().amount()))));
        } else if (slot == RESULT_AMOUNT_SLOT) {
            replace(withResult(new RecipeResult(current.result().type(), current.result().value(),
                    Math.max(1, current.result().amount() + sign))));
        } else if (slot == DELETE_SLOT) {
            recipeManager.delete(current.id());
            onBack.run();
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
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
