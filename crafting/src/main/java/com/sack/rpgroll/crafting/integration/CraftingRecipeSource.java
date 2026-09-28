package com.sack.rpgroll.crafting.integration;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.recipe.RecipeEntry;
import com.sack.rpgroll.common.recipe.RecipeSlot;
import com.sack.rpgroll.common.recipe.RecipeSource;
import com.sack.rpgroll.common.recipe.RecipeStation;
import com.sack.rpgroll.crafting.anvil.AnvilRecipeDefinition;
import com.sack.rpgroll.crafting.api.CraftingAPI;
import com.sack.rpgroll.crafting.brewing.BrewRecipeDefinition;
import com.sack.rpgroll.crafting.cartography.CartographyRecipeDefinition;
import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.grindstone.GrindstoneRecipeDefinition;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.crafting.loom.LoomRecipeDefinition;
import com.sack.rpgroll.crafting.recipe.CustomRecipe;
import com.sack.rpgroll.crafting.recipe.RecipeResult;
import com.sack.rpgroll.crafting.recipe.RecipeResultFactory;
import com.sack.rpgroll.crafting.station.CustomStation;
import com.sack.rpgroll.crafting.villager.VillagerTradeDefinition;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Las recetas de Crafting que no pasan por el registro de Bukkit (las "vanilla" de Crafting sí
 * pasan y el recetario ya las ve): estaciones propias, yunque, fermentación, afiladora,
 * cartografía, telar y aldeanos. Se publica como {@link RecipeSource} para RPGRoll-Recipes; lee
 * {@link CraftingAPI} en cada llamada, así que lo editado desde el menú sale al rehacer el índice.
 * <p>
 * Las recetas de una estación con experimentación se esconden hasta que el jugador las descubre,
 * igual que en el libro de recetas de Crafting.
 */
public final class CraftingRecipeSource implements RecipeSource {

    private static final String SOURCE = "RPGRoll-Crafting";

    private final LangManager lang;
    private final RecipeResultFactory results = new RecipeResultFactory();

    public CraftingRecipeSource(LangManager lang) {
        this.lang = lang;
    }

    @Override
    public String name() {
        return SOURCE;
    }

    @Override
    public Collection<RecipeEntry> recipes() {

        if (!CraftingAPI.isReady()) {
            return List.of();
        }
        CraftingAPI api = CraftingAPI.get();
        List<RecipeEntry> entries = new ArrayList<>();

        for (CustomRecipe recipe : api.customRecipes().getAll()) {
            CustomStation station = api.customStations().get(recipe.stationId()).orElse(null);
            RecipeEntry.Builder builder = RecipeEntry.builder("station/" + recipe.id(), station(station, recipe.stationId()));
            recipe.ingredients().forEach(spec -> builder.input(slot(spec)));
            builder.output(result(recipe.result()));
            qualityNotes(builder, recipe.ingredients());
            if (recipe.processingTimeTicks() > 0) {
                builder.note(lang.raw("recipe_viewer.time", "seconds", number(recipe.processingTimeTicks() / 20.0)));
            }
            if (recipe.fuelPerCraft() > 0) {
                builder.note(lang.raw("recipe_viewer.fuel", "amount", recipe.fuelPerCraft()));
            }
            if (recipe.economyCost() > 0) {
                builder.note(lang.raw("recipe_viewer.cost", "amount", number(recipe.economyCost()),
                        "currency", recipe.economyCurrencyId() == null ? "" : recipe.economyCurrencyId()));
            }
            if (recipe.failChance() > 0) {
                builder.note(lang.raw("recipe_viewer.fail", "percent", number(recipe.failChance() * 100)));
            }
            if (recipe.qualityEnabled()) {
                builder.note(lang.raw("recipe_viewer.quality"));
            }
            conditionNote(builder, recipe.conditions());
            if (station != null && station.allowExperimentation()) {
                String id = recipe.id();
                builder.note(lang.raw("recipe_viewer.discovered"));
                builder.visibleTo(player -> api.discovery().get(player.getUniqueId()).hasDiscovered(id));
            }
            entries.add(builder.build());
        }

        RecipeStation anvil = RecipeStation.vanilla(RecipeStation.ANVIL, Material.ANVIL);
        for (AnvilRecipeDefinition recipe : api.anvilRecipes().getAll()) {
            RecipeEntry.Builder builder = RecipeEntry.builder("anvil/" + recipe.id(), anvil)
                    .input(slot(recipe.baseIngredient()))
                    .input(slot(recipe.additionIngredient()))
                    .output(result(recipe.result()));
            if (recipe.repairCostLevels() > 0) {
                builder.note(lang.raw("recipe_viewer.levels", "levels", recipe.repairCostLevels()));
            }
            conditionNote(builder, recipe.conditions());
            entries.add(builder.build());
        }

        RecipeStation brewing = RecipeStation.vanilla(RecipeStation.BREWING, Material.BREWING_STAND);
        for (BrewRecipeDefinition recipe : api.brewRecipes().getAll()) {
            RecipeEntry.Builder builder = RecipeEntry.builder("brewing/" + recipe.id(), brewing)
                    .input(slot(recipe.ingredient()))
                    .input(RecipeSlot.of(placeholder(Material.POTION, lang.raw("recipe_viewer.any_potion"), 1)))
                    .output(result(recipe.result()));
            conditionNote(builder, recipe.conditions());
            entries.add(builder.build());
        }

        RecipeStation grindstone = RecipeStation.vanilla(RecipeStation.GRINDSTONE, Material.GRINDSTONE);
        for (GrindstoneRecipeDefinition recipe : api.grindstoneRecipes().getAll()) {
            RecipeEntry.Builder builder = RecipeEntry.builder("grindstone/" + recipe.id(), grindstone)
                    .input(slot(recipe.upperIngredient()))
                    .input(slot(recipe.lowerIngredient()))
                    .output(result(recipe.result()));
            conditionNote(builder, recipe.conditions());
            entries.add(builder.build());
        }

        RecipeStation cartography = RecipeStation.vanilla(RecipeStation.CARTOGRAPHY, Material.CARTOGRAPHY_TABLE);
        for (CartographyRecipeDefinition recipe : api.cartographyRecipes().getAll()) {
            RecipeEntry.Builder builder = RecipeEntry.builder("cartography/" + recipe.id(), cartography)
                    .input(slot(recipe.mapIngredient()))
                    .input(slot(recipe.itemIngredient()))
                    .output(result(recipe.result()));
            conditionNote(builder, recipe.conditions());
            entries.add(builder.build());
        }

        RecipeStation loom = RecipeStation.vanilla(RecipeStation.LOOM, Material.LOOM);
        for (LoomRecipeDefinition recipe : api.loomRecipes().getAll()) {
            RecipeEntry.Builder builder = RecipeEntry.builder("loom/" + recipe.id(), loom)
                    .input(slot(recipe.bannerIngredient()))
                    .input(slot(recipe.dyeIngredient()));
            if (recipe.hasPatternIngredient()) {
                builder.input(slot(recipe.patternIngredient()));
            }
            builder.output(result(recipe.result()));
            conditionNote(builder, recipe.conditions());
            entries.add(builder.build());
        }

        RecipeStation villager = RecipeStation.vanilla(RecipeStation.VILLAGER, Material.EMERALD);
        for (VillagerTradeDefinition trade : api.villagerTrades().getAll()) {
            RecipeEntry.Builder builder = RecipeEntry.builder("villager/" + trade.id(), villager);
            trade.costs().forEach(cost -> builder.input(RecipeSlot.of(result(cost))));
            builder.output(result(trade.result()));
            builder.note(lang.raw("recipe_viewer.trade", "name", trade.displayName(), "uses", trade.maxUses()));
            if (trade.economyCost() > 0) {
                builder.note(lang.raw("recipe_viewer.cost", "amount", number(trade.economyCost()),
                        "currency", trade.economyCurrencyId() == null ? "" : trade.economyCurrencyId()));
            }
            conditionNote(builder, trade.conditions());
            entries.add(builder.build());
        }

        return entries;
    }

    private static RecipeStation station(CustomStation station, String fallbackId) {
        if (station == null) {
            return new RecipeStation("rpgroll-crafting:" + fallbackId, fallbackId, ItemStack.of(Material.SMITHING_TABLE));
        }
        return new RecipeStation("rpgroll-crafting:" + station.id(), station.displayName(),
                ItemStack.of(material(station.icon(), Material.SMITHING_TABLE)));
    }

    private RecipeSlot slot(IngredientSpec spec) {

        if (spec == null) {
            return RecipeSlot.EMPTY;
        }

        return switch (spec.type()) {
            case MATERIAL -> {
                Material material = material(spec.value(), null);
                yield material == null
                        ? RecipeSlot.of(placeholder(Material.BARRIER, spec.value(), spec.amount()))
                        : RecipeSlot.of(material, spec.amount());
            }
            case TAG -> {
                NamespacedKey key = NamespacedKey.fromString(spec.value().toLowerCase(Locale.ROOT));
                Tag<Material> tag = key == null ? null : Bukkit.getTag(Tag.REGISTRY_ITEMS, key, Material.class);
                List<ItemStack> options = new ArrayList<>();
                if (tag != null) {
                    tag.getValues().stream().filter(Material::isItem)
                            .forEach(material -> options.add(ItemStack.of(material, spec.amount())));
                }
                yield RecipeSlot.of(options);
            }
            case ITEM_ID -> RecipeSlot.of(ItemsBridge.createItem(spec.value()).map(stack -> {
                stack.setAmount(spec.amount());
                return stack;
            }).orElseGet(() -> placeholder(Material.PAPER, spec.value(), spec.amount())));
            case ANY -> RecipeSlot.of(placeholder(Material.STRUCTURE_VOID, lang.raw("recipe_viewer.any_item"),
                    spec.amount()));
        };
    }

    private ItemStack result(RecipeResult result) {
        return results.build(result, null)
                .orElseGet(() -> placeholder(Material.BARRIER, result.value(), result.amount()));
    }

    private void qualityNotes(RecipeEntry.Builder builder, List<IngredientSpec> ingredients) {
        for (IngredientSpec spec : ingredients) {
            if (spec.hasMinQuality()) {
                builder.note(lang.raw("recipe_viewer.min_quality", "item", spec.value(), "quality", spec.minQuality()));
            }
        }
    }

    private void conditionNote(RecipeEntry.Builder builder, List<RecipeCondition> conditions) {
        if (!conditions.isEmpty()) {
            builder.note(lang.raw("recipe_viewer.conditions", "count", conditions.size()));
        }
    }

    private static ItemStack placeholder(Material material, String name, int amount) {
        ItemStack stack = ItemStack.of(material, Math.max(1, Math.min(99, amount)));
        ItemMeta meta = stack.getItemMeta();
        meta.itemName(ComponentUtils.parse(name));
        stack.setItemMeta(meta);
        return stack;
    }

    private static Material material(String raw, Material fallback) {
        Material material = raw == null ? null : Material.matchMaterial(raw.trim());
        return material != null && material.isItem() && !material.isAir() ? material : fallback;
    }

    private static String number(double value) {
        return new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value);
    }
}
