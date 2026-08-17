package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.fishing.core.BaitManager;
import com.sack.rpgroll.fishing.core.DepthRequirement;
import com.sack.rpgroll.fishing.core.FishBehaviorType;
import com.sack.rpgroll.fishing.core.FishCategory;
import com.sack.rpgroll.fishing.core.FishRarity;
import com.sack.rpgroll.fishing.core.FishSpecies;
import com.sack.rpgroll.fishing.core.FishSpeciesManager;
import com.sack.rpgroll.fishing.core.TimeRequirement;
import com.sack.rpgroll.fishing.core.WaterType;
import com.sack.rpgroll.fishing.core.WeatherType;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Identidad/clasificación/requisitos de captura de una especie — todos
 * los campos {@code Set} se editan como listas separadas por comas vía
 * chat (mismo criterio que las afinidades de MagicSchool o los tags de
 * Season). Usa un objeto de staging mutable para reconstruir el record
 * sin repetir sus 27 campos en cada handler.
 */
public class SpeciesEditorGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private static final int NAME_SLOT = 0;
    private static final int ICON_SLOT = 1;
    private static final int MODEL_DATA_SLOT = 2;
    private static final int DESCRIPTION_SLOT = 3;
    private static final int CATEGORY_SLOT = 4;
    private static final int RARITY_SLOT = 5;

    private static final int WATER_TYPES_SLOT = 9;
    private static final int BIOMES_SLOT = 10;
    private static final int DEPTHS_SLOT = 11;
    private static final int WEIGHT_SLOT = 12;
    private static final int LENGTH_SLOT = 13;
    private static final int PRICE_SLOT = 14;
    private static final int XP_SLOT = 15;
    private static final int BEHAVIOR_SLOT = 16;

    private static final int SEASONS_SLOT = 18;
    private static final int WEATHERS_SLOT = 19;
    private static final int TIMES_SLOT = 20;
    private static final int BAIT_TAGS_SLOT = 21;
    private static final int LEGENDARY_SLOT = 22;
    private static final int LEVEL_SLOT = 23;
    private static final int FULL_MOON_SLOT = 24;
    private static final int REQUIRED_BAIT_SLOT = 25;

    private static final int CATCH_EFFECT_SLOT = 27;
    private static final int CATCH_STATUS_EFFECT_SLOT = 28;

    private static final int BACK_SLOT = 49;

    private final FishSpeciesManager speciesManager;
    private final BaitManager baitManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;
    private FishSpecies current;

    public SpeciesEditorGUI(Player player, FishSpecies species, FishSpeciesManager speciesManager,
            BaitManager baitManager, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.species.editor_title", "id", species.id()), SIZE);
        this.current = species;
        this.speciesManager = speciesManager;
        this.baitManager = baitManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.onBack = onBack;
    }

    /** Staging mutable — evita repetir los 27 campos del record en cada handler. */
    private static final class Fields {
        String displayName;
        String icon;
        int customModelData;
        String description;
        FishCategory category;
        FishRarity rarity;
        Set<WaterType> waterTypes;
        Set<String> biomes;
        Set<DepthRequirement> depths;
        double minWeight;
        double maxWeight;
        double minLength;
        double maxLength;
        double basePrice;
        int baseExperience;
        FishBehaviorType behavior;
        Set<String> allowedSeasons;
        Set<WeatherType> allowedWeathers;
        Set<TimeRequirement> allowedTimes;
        Set<String> attractedByBaitTags;
        boolean legendary;
        int requiredLevel;
        boolean requiresFullMoon;
        String requiredBaitId;
        String catchEffectId;
        String catchStatusEffectId;
    }

    private Fields snapshot() {

        Fields f = new Fields();
        f.displayName = current.displayName();
        f.icon = current.icon();
        f.customModelData = current.customModelData();
        f.description = current.description();
        f.category = current.category();
        f.rarity = current.rarity();
        f.waterTypes = current.waterTypes();
        f.biomes = current.biomes();
        f.depths = current.depths();
        f.minWeight = current.minWeight();
        f.maxWeight = current.maxWeight();
        f.minLength = current.minLength();
        f.maxLength = current.maxLength();
        f.basePrice = current.basePrice();
        f.baseExperience = current.baseExperience();
        f.behavior = current.behavior();
        f.allowedSeasons = current.allowedSeasons();
        f.allowedWeathers = current.allowedWeathers();
        f.allowedTimes = current.allowedTimes();
        f.attractedByBaitTags = current.attractedByBaitTags();
        f.legendary = current.legendary();
        f.requiredLevel = current.requiredLevel();
        f.requiresFullMoon = current.requiresFullMoon();
        f.requiredBaitId = current.requiredBaitId();
        f.catchEffectId = current.catchEffectId();
        f.catchStatusEffectId = current.catchStatusEffectId();
        return f;
    }

    private void replace(Consumer<Fields> mutator) {

        Fields f = snapshot();
        mutator.accept(f);

        current = new FishSpecies(current.id(), f.displayName, f.icon, f.customModelData, f.description, f.category,
                f.rarity, f.waterTypes, f.biomes, f.depths, f.minWeight, f.maxWeight, f.minLength, f.maxLength,
                f.basePrice, f.baseExperience, f.behavior, f.allowedSeasons, f.allowedWeathers, f.allowedTimes,
                f.attractedByBaitTags, f.legendary, f.requiredLevel, f.requiresFullMoon, f.requiredBaitId,
                f.catchEffectId, f.catchStatusEffectId);

        speciesManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.species.field_name", "name", current.displayName())).build());

        setItem(ICON_SLOT, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(current.icon()))
                .setName(lang.component("gui.species.field_icon", "icon", current.icon())).build());

        setItem(MODEL_DATA_SLOT, new ItemBuilder(Material.PAPER)
                .setName(lang.component("gui.species.field_model_data", "value", current.customModelData()))
                .setLore(lang.component("gui.species.model_data_hint"))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("gui.common.description_title"))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? lang.raw("gui.common.no_description") : current.description()))
                .build());

        setItem(CATEGORY_SLOT, new ItemBuilder(Material.TROPICAL_FISH)
                .setName(lang.component("gui.species.field_category", "value", current.category()))
                .setLore(lang.component("gui.common.click_next")).build());

        setItem(RARITY_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(lang.component("gui.species.field_rarity", "value", current.rarity()))
                .setLore(lang.component("gui.common.click_next")).build());

        setItem(WATER_TYPES_SLOT, new ItemBuilder(Material.WATER_BUCKET)
                .setName(lang.component("gui.species.field_water_types", "value", joinEnum(current.waterTypes())))
                .setLore(lang.component("gui.common.empty_comma_hint"))
                .build());

        setItem(BIOMES_SLOT, new ItemBuilder(Material.GRASS_BLOCK)
                .setName(lang.component("gui.species.field_biomes", "value", String.join(", ", current.biomes())))
                .setLore(lang.component("gui.species.biomes_hint"))
                .build());

        setItem(DEPTHS_SLOT, new ItemBuilder(Material.PRISMARINE_SHARD)
                .setName(lang.component("gui.species.field_depths", "value", joinEnum(current.depths())))
                .setLore(lang.component("gui.common.empty_any")).build());

        setItem(WEIGHT_SLOT, new ItemBuilder(Material.IRON_INGOT)
                .setName(lang.component("gui.species.field_weight", "min",
                        String.format(Locale.ROOT, "%.2f", current.minWeight()), "max",
                        String.format(Locale.ROOT, "%.2f", current.maxWeight())))
                .setLore(lang.component("gui.species.weight_hint")).build());

        setItem(LENGTH_SLOT, new ItemBuilder(Material.STICK)
                .setName(lang.component("gui.species.field_length", "min",
                        String.format(Locale.ROOT, "%.1f", current.minLength()), "max",
                        String.format(Locale.ROOT, "%.1f", current.maxLength())))
                .setLore(lang.component("gui.species.length_hint")).build());

        setItem(PRICE_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(lang.component("gui.species.field_price", "value",
                        String.format(Locale.ROOT, "%.1f", current.basePrice())))
                .setLore(lang.component("gui.common.plusminus_1")).build());

        setItem(XP_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(lang.component("gui.species.field_xp", "value", current.baseExperience()))
                .setLore(lang.component("gui.common.plusminus_1")).build());

        setItem(BEHAVIOR_SLOT, new ItemBuilder(Material.SPIDER_EYE)
                .setName(lang.component("gui.species.field_behavior", "value", current.behavior()))
                .setLore(lang.component("gui.species.behavior_hint"))
                .build());

        setItem(SEASONS_SLOT, new ItemBuilder(Material.SUNFLOWER)
                .setName(lang.component("gui.species.field_seasons", "value", String.join(", ", current.allowedSeasons())))
                .setLore(lang.component("gui.species.seasons_hint")).build());

        setItem(WEATHERS_SLOT, new ItemBuilder(Material.LIGHTNING_ROD)
                .setName(lang.component("gui.species.field_weathers", "value", joinEnum(current.allowedWeathers())))
                .setLore(lang.component("gui.common.empty_any")).build());

        setItem(TIMES_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(lang.component("gui.species.field_times", "value", joinEnum(current.allowedTimes())))
                .setLore(lang.component("gui.common.empty_any")).build());

        setItem(BAIT_TAGS_SLOT, new ItemBuilder(Material.STRING)
                .setName(lang.component("gui.species.field_bait_tags", "value",
                        String.join(", ", current.attractedByBaitTags())))
                .setLore(lang.component("gui.species.bait_tags_hint")).build());

        setItem(LEGENDARY_SLOT, new ItemBuilder(current.legendary() ? Material.DRAGON_EGG : Material.GRAY_DYE)
                .setName(lang.component("gui.species.field_legendary", "value", current.legendary()))
                .setLore(lang.component("gui.species.toggle_hint")).build());

        setItem(LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(lang.component("gui.species.field_level", "value", current.requiredLevel()))
                .setLore(lang.component("gui.species.level_hint"))
                .build());

        setItem(FULL_MOON_SLOT, new ItemBuilder(current.requiresFullMoon() ? Material.GLOWSTONE : Material.GRAY_DYE)
                .setName(lang.component("gui.species.field_full_moon", "value", current.requiresFullMoon()))
                .setLore(lang.component("gui.species.full_moon_hint"))
                .build());

        setItem(REQUIRED_BAIT_SLOT, new ItemBuilder(Material.STRING)
                .setName(lang.component("gui.species.field_required_bait", "value",
                        current.requiredBaitId() == null ? lang.raw("gui.common.none_fem") : current.requiredBaitId()))
                .setLore(lang.component("gui.species.required_bait_hint")).build());

        setItem(CATCH_EFFECT_SLOT, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(lang.component("gui.species.field_catch_effect", "value",
                        current.catchEffectId() == null ? lang.raw("gui.common.none_masc") : current.catchEffectId()))
                .build());

        setItem(CATCH_STATUS_EFFECT_SLOT, new ItemBuilder(Material.POTION)
                .setName(lang.component("gui.species.field_catch_status_effect", "value",
                        current.catchStatusEffectId() == null ? lang.raw("gui.common.none_masc")
                                : current.catchStatusEffectId()))
                .setLore(lang.component("gui.species.catch_status_effect_hint"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    private String joinEnum(Set<? extends Enum<?>> values) {
        return values.stream().map(Enum::name).collect(Collectors.joining(", "));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();
        int sign = click == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.species.prompt_name"), value -> replace(f -> f.displayName = value));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.species.prompt_icon"), value -> replace(f -> f.icon = value));
            return;
        }

        if (slot == MODEL_DATA_SLOT) {
            replace(f -> f.customModelData = Math.max(0, current.customModelData() + sign));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.species.prompt_description"), value -> replace(f -> f.description = value));
            return;
        }

        if (slot == CATEGORY_SLOT) {
            FishCategory[] values = FishCategory.values();
            FishCategory next = values[(current.category().ordinal() + 1) % values.length];
            replace(f -> f.category = next);
            return;
        }

        if (slot == RARITY_SLOT) {
            FishRarity[] values = FishRarity.values();
            FishRarity next = values[(current.rarity().ordinal() + 1) % values.length];
            replace(f -> f.rarity = next);
            return;
        }

        if (slot == WATER_TYPES_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.species.prompt_water_types"),
                    value -> replace(f -> f.waterTypes = parseEnumSet(WaterType.class, value)));
            return;
        }

        if (slot == BIOMES_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.species.prompt_biomes"),
                    value -> replace(f -> f.biomes = parseStringSet(value)));
            return;
        }

        if (slot == DEPTHS_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.species.prompt_depths"),
                    value -> replace(f -> f.depths = parseEnumSet(DepthRequirement.class, value)));
            return;
        }

        if (slot == WEIGHT_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.species.prompt_weight"), value -> {
                double[] range = parseRange(value, current.minWeight(), current.maxWeight());
                replace(f -> {
                    f.minWeight = range[0];
                    f.maxWeight = range[1];
                });
            });
            return;
        }

        if (slot == LENGTH_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.species.prompt_length"), value -> {
                double[] range = parseRange(value, current.minLength(), current.maxLength());
                replace(f -> {
                    f.minLength = range[0];
                    f.maxLength = range[1];
                });
            });
            return;
        }

        if (slot == PRICE_SLOT) {
            replace(f -> f.basePrice = Math.max(0, current.basePrice() + sign));
            return;
        }

        if (slot == XP_SLOT) {
            replace(f -> f.baseExperience = Math.max(0, current.baseExperience() + sign));
            return;
        }

        if (slot == BEHAVIOR_SLOT) {
            FishBehaviorType[] values = FishBehaviorType.values();
            FishBehaviorType next = values[(current.behavior().ordinal() + 1) % values.length];
            replace(f -> f.behavior = next);
            return;
        }

        if (slot == SEASONS_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.species.prompt_seasons"),
                    value -> replace(f -> f.allowedSeasons = parseStringSet(value)));
            return;
        }

        if (slot == WEATHERS_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.species.prompt_weathers"),
                    value -> replace(f -> f.allowedWeathers = parseEnumSet(WeatherType.class, value)));
            return;
        }

        if (slot == TIMES_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.species.prompt_times"),
                    value -> replace(f -> f.allowedTimes = parseEnumSet(TimeRequirement.class, value)));
            return;
        }

        if (slot == BAIT_TAGS_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.species.prompt_bait_tags"),
                    value -> replace(f -> f.attractedByBaitTags = parseStringSet(value)));
            return;
        }

        if (slot == LEGENDARY_SLOT) {
            replace(f -> f.legendary = !current.legendary());
            return;
        }

        if (slot == LEVEL_SLOT) {
            replace(f -> f.requiredLevel = Math.max(0, current.requiredLevel() + sign));
            return;
        }

        if (slot == FULL_MOON_SLOT) {
            replace(f -> f.requiresFullMoon = !current.requiresFullMoon());
            return;
        }

        if (slot == REQUIRED_BAIT_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.species.prompt_required_bait"), value -> {

                if (value.equalsIgnoreCase("ninguna")) {
                    replace(f -> f.requiredBaitId = null);
                    return;
                }

                if (baitManager.get(value.trim().toLowerCase(Locale.ROOT)).isEmpty()) {
                    lang.send(player, "gui.species.unknown_bait");
                    return;
                }

                replace(f -> f.requiredBaitId = value.trim().toLowerCase(Locale.ROOT));
            });
            return;
        }

        if (slot == CATCH_EFFECT_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.species.prompt_catch_effect"),
                    value -> replace(f -> f.catchEffectId = value.equalsIgnoreCase("ninguno") ? null : value.trim()));
            return;
        }

        if (slot == CATCH_STATUS_EFFECT_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.species.prompt_catch_status_effect"),
                    value -> replace(f -> f.catchStatusEffectId = value.equalsIgnoreCase("ninguno") ? null : value.trim()));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private Set<String> parseStringSet(String raw) {

        Set<String> result = new HashSet<>();

        for (String entry : raw.split(",")) {
            if (!entry.isBlank()) {
                result.add(entry.trim().toLowerCase(Locale.ROOT));
            }
        }

        return result;
    }

    private <E extends Enum<E>> Set<E> parseEnumSet(Class<E> type, String raw) {

        Set<E> result = new HashSet<>();

        for (String entry : raw.split(",")) {
            try {
                result.add(Enum.valueOf(type, entry.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return result;
    }

    private double[] parseRange(String raw, double fallbackMin, double fallbackMax) {

        String[] parts = raw.trim().split("\\s+");

        if (parts.length < 2) {
            return new double[] { fallbackMin, fallbackMax };
        }

        try {
            return new double[] { Double.parseDouble(parts[0]), Double.parseDouble(parts[1]) };
        } catch (NumberFormatException e) {
            return new double[] { fallbackMin, fallbackMax };
        }
    }

}
