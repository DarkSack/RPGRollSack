package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.species.Species;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SpeciesEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 9;
    private static final int ICON_SLOT = 10;
    private static final int ENTITY_TYPE_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int PRODUCT_TYPES_SLOT = 13;
    private static final int WEIGHT_MIN_SLOT = 14;
    private static final int WEIGHT_MAX_SLOT = 15;
    private static final int BABY_DURATION_SLOT = 16;
    private static final int JUVENILE_DURATION_SLOT = 17;
    private static final int ELDER_THRESHOLD_SLOT = 18;
    private static final int GESTATION_SLOT = 19;
    private static final int MIN_LITTER_SLOT = 20;
    private static final int MAX_LITTER_SLOT = 21;
    private static final int FERTILITY_SLOT = 22;
    private static final int DIET_TAGS_SLOT = 23;
    private static final int BASE_PRODUCTION_SLOT = 24;
    private static final int BACK_SLOT = 40;

    private static final long TICK_STEP = 1200L;

    private final SpeciesManager speciesManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Species current;

    public SpeciesEditorGUI(Player player, Species species, SpeciesManager speciesManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.species.editor.title", "id", species.id()), NamedTextColor.GOLD), SIZE);
        this.current = species;
        this.speciesManager = speciesManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Species updated) {
        current = updated;
        speciesManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        var lang = chatPromptManager.lang();

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text(lang.raw("gui.editor.name_line", "name", current.displayName()), NamedTextColor.YELLOW)).build());

        setItem(ICON_SLOT, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(current.icon(), Material.COW_SPAWN_EGG))
                .setName(Component.text(lang.raw("gui.editor.icon_line", "icon", current.icon()), NamedTextColor.YELLOW)).build());

        setItem(ENTITY_TYPE_SLOT, new ItemBuilder(Material.ARMOR_STAND)
                .setName(Component.text(lang.raw("gui.species.editor.entity_type_line", "entity", current.entityType()), NamedTextColor.AQUA))
                .setLore(Component.text(lang.raw("gui.species.editor.entity_type_hint"), NamedTextColor.GRAY))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text(lang.raw("gui.editor.description_title"), NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank() ? lang.raw("gui.editor.no_description") : current.description()))
                .build());

        setItem(PRODUCT_TYPES_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(Component.text(lang.raw("gui.species.editor.product_types_line", "types", String.join(", ", current.productTypes())), NamedTextColor.GOLD))
                .setLore(Component.text(lang.raw("gui.species.editor.product_types_hint"), NamedTextColor.GRAY))
                .build());

        setItem(WEIGHT_MIN_SLOT, new ItemBuilder(Material.IRON_INGOT)
                .setName(Component.text(lang.raw("gui.species.editor.weight_min_line", "weight",
                        String.format(Locale.ROOT, "%.1f", current.baseWeightMin())), NamedTextColor.AQUA))
                .setLore(Component.text(lang.raw("gui.editor.step10_hint"), NamedTextColor.GRAY)).build());

        setItem(WEIGHT_MAX_SLOT, new ItemBuilder(Material.GOLD_INGOT)
                .setName(Component.text(lang.raw("gui.species.editor.weight_max_line", "weight",
                        String.format(Locale.ROOT, "%.1f", current.baseWeightMax())), NamedTextColor.AQUA))
                .setLore(Component.text(lang.raw("gui.editor.step10_hint"), NamedTextColor.GRAY)).build());

        setItem(BABY_DURATION_SLOT, new ItemBuilder(Material.EGG)
                .setName(Component.text(lang.raw("gui.species.editor.baby_duration_line", "duration", ticksToMinutes(current.babyStageDurationTicks())),
                        NamedTextColor.GREEN))
                .setLore(Component.text(lang.raw("gui.editor.step1min_hint"), NamedTextColor.GRAY)).build());

        setItem(JUVENILE_DURATION_SLOT, new ItemBuilder(Material.LEATHER)
                .setName(Component.text(lang.raw("gui.species.editor.juvenile_duration_line", "duration", ticksToMinutes(current.juvenileStageDurationTicks())),
                        NamedTextColor.GREEN))
                .setLore(Component.text(lang.raw("gui.editor.step1min_hint"), NamedTextColor.GRAY)).build());

        setItem(ELDER_THRESHOLD_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text(lang.raw("gui.species.editor.elder_threshold_line", "duration", ticksToMinutes(current.elderThresholdTicks())),
                        NamedTextColor.GRAY))
                .setLore(Component.text(lang.raw("gui.species.editor.elder_threshold_hint"),
                        NamedTextColor.GRAY)).build());

        setItem(GESTATION_SLOT, new ItemBuilder(Material.PINK_DYE)
                .setName(Component.text(lang.raw("gui.species.editor.gestation_line", "duration", ticksToMinutes(current.gestationDurationTicks())),
                        NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text(lang.raw("gui.species.editor.gestation_hint"),
                        NamedTextColor.GRAY)).build());

        setItem(MIN_LITTER_SLOT, new ItemBuilder(Material.BREAD)
                .setName(Component.text(lang.raw("gui.species.editor.min_litter_line", "value", current.minLitterSize()), NamedTextColor.WHITE))
                .setLore(Component.text(lang.raw("gui.editor.step1_hint"), NamedTextColor.GRAY)).build());

        setItem(MAX_LITTER_SLOT, new ItemBuilder(Material.CAKE)
                .setName(Component.text(lang.raw("gui.species.editor.max_litter_line", "value", current.maxLitterSize()), NamedTextColor.WHITE))
                .setLore(Component.text(lang.raw("gui.editor.step1_hint"), NamedTextColor.GRAY)).build());

        setItem(FERTILITY_SLOT, new ItemBuilder(Material.RABBIT_FOOT)
                .setName(Component.text(lang.raw("gui.species.editor.fertility_line", "value",
                        String.format(Locale.ROOT, "%.0f", current.baseFertility() * 100)), NamedTextColor.YELLOW))
                .setLore(Component.text(lang.raw("gui.editor.step5pct_hint"), NamedTextColor.GRAY)).build());

        setItem(DIET_TAGS_SLOT, new ItemBuilder(Material.WHEAT)
                .setName(Component.text(lang.raw("gui.species.editor.diet_line", "tags", String.join(", ", current.dietTags())), NamedTextColor.GOLD))
                .setLore(Component.text(lang.raw("gui.species.editor.diet_hint"), NamedTextColor.GRAY))
                .build());

        setItem(BASE_PRODUCTION_SLOT, new ItemBuilder(Material.BUCKET)
                .setName(Component.text(lang.raw("gui.species.editor.base_production_title"), NamedTextColor.AQUA))
                .setLore(ItemBuilder.toLoreLines(formatProduction(current.baseProduction())
                        + "\n" + lang.raw("gui.species.editor.base_production_hint")))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    private String ticksToMinutes(long ticks) {
        return chatPromptManager.lang().raw("gui.editor.ticks_to_minutes", "minutes", String.format(Locale.ROOT, "%.1f", ticks / 1200.0), "ticks", ticks);
    }

    private String formatProduction(Map<String, Double> production) {

        if (production.isEmpty()) {
            return chatPromptManager.lang().raw("gui.species.editor.no_production");
        }

        StringBuilder builder = new StringBuilder();

        for (var entry : production.entrySet()) {
            builder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        return builder.toString().strip();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_name"), value -> replace(withDisplayName(value)));
        } else if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_icon"), value -> replace(withIcon(value)));
        } else if (slot == ENTITY_TYPE_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.species.editor.prompt_entity_type"),
                    value -> replace(withEntityType(value)));
        } else if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_description"), value -> replace(withDescription(value)));
        } else if (slot == PRODUCT_TYPES_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.species.editor.prompt_product_types"),
                    value -> replace(withProductTypes(parseSet(value))));
        } else if (slot == WEIGHT_MIN_SLOT) {
            replace(withWeightMin(current.baseWeightMin() + sign * 10));
        } else if (slot == WEIGHT_MAX_SLOT) {
            replace(withWeightMax(current.baseWeightMax() + sign * 10));
        } else if (slot == BABY_DURATION_SLOT) {
            replace(withBabyDuration(Math.max(0, current.babyStageDurationTicks() + sign * TICK_STEP)));
        } else if (slot == JUVENILE_DURATION_SLOT) {
            replace(withJuvenileDuration(Math.max(0, current.juvenileStageDurationTicks() + sign * TICK_STEP)));
        } else if (slot == ELDER_THRESHOLD_SLOT) {
            replace(withElderThreshold(Math.max(0, current.elderThresholdTicks() + sign * TICK_STEP)));
        } else if (slot == GESTATION_SLOT) {
            replace(withGestation(Math.max(0, current.gestationDurationTicks() + sign * TICK_STEP)));
        } else if (slot == MIN_LITTER_SLOT) {
            replace(withMinLitter(Math.max(1, current.minLitterSize() + sign)));
        } else if (slot == MAX_LITTER_SLOT) {
            replace(withMaxLitter(Math.max(current.minLitterSize(), current.maxLitterSize() + sign)));
        } else if (slot == FERTILITY_SLOT) {
            replace(withFertility(Math.max(0, Math.min(1, current.baseFertility() + sign * 0.05))));
        } else if (slot == DIET_TAGS_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.species.editor.prompt_diet_tags"),
                    value -> replace(withDietTags(parseSet(value))));
        } else if (slot == BASE_PRODUCTION_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.species.editor.prompt_base_production"),
                    value -> replace(withBaseProduction(parseMap(value))));
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private Set<String> parseSet(String raw) {

        Set<String> result = new HashSet<>();

        for (String entry : raw.split(",")) {
            if (!entry.isBlank()) {
                result.add(entry.trim().toLowerCase(Locale.ROOT));
            }
        }

        return result;
    }

    private Map<String, Double> parseMap(String raw) {

        Map<String, Double> result = new HashMap<>();

        for (String entry : raw.split(",")) {

            String[] parts = entry.split("=", 2);

            if (parts.length != 2) {
                continue;
            }

            try {
                result.put(parts[0].trim().toLowerCase(Locale.ROOT), Double.parseDouble(parts[1].trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        return result;
    }

    private Species withDisplayName(String value) {
        return new Species(current.id(), value, current.icon(), current.description(), current.entityType(),
                current.productTypes(), current.baseProduction(), current.baseWeightMin(), current.baseWeightMax(),
                current.babyStageDurationTicks(), current.juvenileStageDurationTicks(), current.elderThresholdTicks(),
                current.gestationDurationTicks(), current.minLitterSize(), current.maxLitterSize(),
                current.baseFertility(), current.dietTags());
    }

    private Species withIcon(String value) {
        return new Species(current.id(), current.displayName(), value, current.description(), current.entityType(),
                current.productTypes(), current.baseProduction(), current.baseWeightMin(), current.baseWeightMax(),
                current.babyStageDurationTicks(), current.juvenileStageDurationTicks(), current.elderThresholdTicks(),
                current.gestationDurationTicks(), current.minLitterSize(), current.maxLitterSize(),
                current.baseFertility(), current.dietTags());
    }

    private Species withEntityType(String value) {
        return new Species(current.id(), current.displayName(), current.icon(), current.description(), value,
                current.productTypes(), current.baseProduction(), current.baseWeightMin(), current.baseWeightMax(),
                current.babyStageDurationTicks(), current.juvenileStageDurationTicks(), current.elderThresholdTicks(),
                current.gestationDurationTicks(), current.minLitterSize(), current.maxLitterSize(),
                current.baseFertility(), current.dietTags());
    }

    private Species withDescription(String value) {
        return new Species(current.id(), current.displayName(), current.icon(), value, current.entityType(),
                current.productTypes(), current.baseProduction(), current.baseWeightMin(), current.baseWeightMax(),
                current.babyStageDurationTicks(), current.juvenileStageDurationTicks(), current.elderThresholdTicks(),
                current.gestationDurationTicks(), current.minLitterSize(), current.maxLitterSize(),
                current.baseFertility(), current.dietTags());
    }

    private Species withProductTypes(Set<String> value) {
        return new Species(current.id(), current.displayName(), current.icon(), current.description(),
                current.entityType(), value, current.baseProduction(), current.baseWeightMin(), current.baseWeightMax(),
                current.babyStageDurationTicks(), current.juvenileStageDurationTicks(), current.elderThresholdTicks(),
                current.gestationDurationTicks(), current.minLitterSize(), current.maxLitterSize(),
                current.baseFertility(), current.dietTags());
    }

    private Species withWeightMin(double value) {
        return new Species(current.id(), current.displayName(), current.icon(), current.description(),
                current.entityType(), current.productTypes(), current.baseProduction(), value, current.baseWeightMax(),
                current.babyStageDurationTicks(), current.juvenileStageDurationTicks(), current.elderThresholdTicks(),
                current.gestationDurationTicks(), current.minLitterSize(), current.maxLitterSize(),
                current.baseFertility(), current.dietTags());
    }

    private Species withWeightMax(double value) {
        return new Species(current.id(), current.displayName(), current.icon(), current.description(),
                current.entityType(), current.productTypes(), current.baseProduction(), current.baseWeightMin(), value,
                current.babyStageDurationTicks(), current.juvenileStageDurationTicks(), current.elderThresholdTicks(),
                current.gestationDurationTicks(), current.minLitterSize(), current.maxLitterSize(),
                current.baseFertility(), current.dietTags());
    }

    private Species withBabyDuration(long value) {
        return new Species(current.id(), current.displayName(), current.icon(), current.description(),
                current.entityType(), current.productTypes(), current.baseProduction(), current.baseWeightMin(),
                current.baseWeightMax(), value, current.juvenileStageDurationTicks(), current.elderThresholdTicks(),
                current.gestationDurationTicks(), current.minLitterSize(), current.maxLitterSize(),
                current.baseFertility(), current.dietTags());
    }

    private Species withJuvenileDuration(long value) {
        return new Species(current.id(), current.displayName(), current.icon(), current.description(),
                current.entityType(), current.productTypes(), current.baseProduction(), current.baseWeightMin(),
                current.baseWeightMax(), current.babyStageDurationTicks(), value, current.elderThresholdTicks(),
                current.gestationDurationTicks(), current.minLitterSize(), current.maxLitterSize(),
                current.baseFertility(), current.dietTags());
    }

    private Species withElderThreshold(long value) {
        return new Species(current.id(), current.displayName(), current.icon(), current.description(),
                current.entityType(), current.productTypes(), current.baseProduction(), current.baseWeightMin(),
                current.baseWeightMax(), current.babyStageDurationTicks(), current.juvenileStageDurationTicks(), value,
                current.gestationDurationTicks(), current.minLitterSize(), current.maxLitterSize(),
                current.baseFertility(), current.dietTags());
    }

    private Species withGestation(long value) {
        return new Species(current.id(), current.displayName(), current.icon(), current.description(),
                current.entityType(), current.productTypes(), current.baseProduction(), current.baseWeightMin(),
                current.baseWeightMax(), current.babyStageDurationTicks(), current.juvenileStageDurationTicks(),
                current.elderThresholdTicks(), value, current.minLitterSize(), current.maxLitterSize(),
                current.baseFertility(), current.dietTags());
    }

    private Species withMinLitter(int value) {
        return new Species(current.id(), current.displayName(), current.icon(), current.description(),
                current.entityType(), current.productTypes(), current.baseProduction(), current.baseWeightMin(),
                current.baseWeightMax(), current.babyStageDurationTicks(), current.juvenileStageDurationTicks(),
                current.elderThresholdTicks(), current.gestationDurationTicks(), value, current.maxLitterSize(),
                current.baseFertility(), current.dietTags());
    }

    private Species withMaxLitter(int value) {
        return new Species(current.id(), current.displayName(), current.icon(), current.description(),
                current.entityType(), current.productTypes(), current.baseProduction(), current.baseWeightMin(),
                current.baseWeightMax(), current.babyStageDurationTicks(), current.juvenileStageDurationTicks(),
                current.elderThresholdTicks(), current.gestationDurationTicks(), current.minLitterSize(), value,
                current.baseFertility(), current.dietTags());
    }

    private Species withFertility(double value) {
        return new Species(current.id(), current.displayName(), current.icon(), current.description(),
                current.entityType(), current.productTypes(), current.baseProduction(), current.baseWeightMin(),
                current.baseWeightMax(), current.babyStageDurationTicks(), current.juvenileStageDurationTicks(),
                current.elderThresholdTicks(), current.gestationDurationTicks(), current.minLitterSize(),
                current.maxLitterSize(), value, current.dietTags());
    }

    private Species withDietTags(Set<String> value) {
        return new Species(current.id(), current.displayName(), current.icon(), current.description(),
                current.entityType(), current.productTypes(), current.baseProduction(), current.baseWeightMin(),
                current.baseWeightMax(), current.babyStageDurationTicks(), current.juvenileStageDurationTicks(),
                current.elderThresholdTicks(), current.gestationDurationTicks(), current.minLitterSize(),
                current.maxLitterSize(), current.baseFertility(), value);
    }

    private Species withBaseProduction(Map<String, Double> value) {
        return new Species(current.id(), current.displayName(), current.icon(), current.description(),
                current.entityType(), current.productTypes(), value, current.baseWeightMin(), current.baseWeightMax(),
                current.babyStageDurationTicks(), current.juvenileStageDurationTicks(), current.elderThresholdTicks(),
                current.gestationDurationTicks(), current.minLitterSize(), current.maxLitterSize(),
                current.baseFertility(), current.dietTags());
    }

}
