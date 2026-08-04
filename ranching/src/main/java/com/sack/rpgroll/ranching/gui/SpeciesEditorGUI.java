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
        super(player, Component.text("Especie: " + species.id(), NamedTextColor.GOLD), SIZE);
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

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW)).build());

        setItem(ICON_SLOT, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(current.icon(), Material.COW_SPAWN_EGG))
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW)).build());

        setItem(ENTITY_TYPE_SLOT, new ItemBuilder(Material.ARMOR_STAND)
                .setName(Component.text("Entidad vanilla: " + current.entityType(), NamedTextColor.AQUA))
                .setLore(Component.text("Nombre de un EntityType (COW, CHICKEN, SHEEP...)", NamedTextColor.GRAY))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank() ? "(sin descripción)" : current.description()))
                .build());

        setItem(PRODUCT_TYPES_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(Component.text("Productos: " + String.join(", ", current.productTypes()), NamedTextColor.GOLD))
                .setLore(Component.text("Escribí tipos separados por comas (milk, wool, eggs...)", NamedTextColor.GRAY))
                .build());

        setItem(WEIGHT_MIN_SLOT, new ItemBuilder(Material.IRON_INGOT)
                .setName(Component.text(String.format(Locale.ROOT, "Peso mín.: %.1f kg", current.baseWeightMin()),
                        NamedTextColor.AQUA))
                .setLore(Component.text("Click: +10 · Click derecho: -10", NamedTextColor.GRAY)).build());

        setItem(WEIGHT_MAX_SLOT, new ItemBuilder(Material.GOLD_INGOT)
                .setName(Component.text(String.format(Locale.ROOT, "Peso máx.: %.1f kg", current.baseWeightMax()),
                        NamedTextColor.AQUA))
                .setLore(Component.text("Click: +10 · Click derecho: -10", NamedTextColor.GRAY)).build());

        setItem(BABY_DURATION_SLOT, new ItemBuilder(Material.EGG)
                .setName(Component.text("Duración cría: " + ticksToMinutes(current.babyStageDurationTicks()),
                        NamedTextColor.GREEN))
                .setLore(Component.text("Click: +1 min · Click derecho: -1 min", NamedTextColor.GRAY)).build());

        setItem(JUVENILE_DURATION_SLOT, new ItemBuilder(Material.LEATHER)
                .setName(Component.text("Duración juvenil: " + ticksToMinutes(current.juvenileStageDurationTicks()),
                        NamedTextColor.GREEN))
                .setLore(Component.text("Click: +1 min · Click derecho: -1 min", NamedTextColor.GRAY)).build());

        setItem(ELDER_THRESHOLD_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Umbral anciano: " + ticksToMinutes(current.elderThresholdTicks()),
                        NamedTextColor.GRAY))
                .setLore(Component.text("Tiempo como adulto antes de ser anciano. Click: +1 min · Click derecho: -1 min",
                        NamedTextColor.GRAY)).build());

        setItem(GESTATION_SLOT, new ItemBuilder(Material.PINK_DYE)
                .setName(Component.text("Gestación: " + ticksToMinutes(current.gestationDurationTicks()),
                        NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text("0 = nace directo, sin gestación. Click: +1 min · Click derecho: -1 min",
                        NamedTextColor.GRAY)).build());

        setItem(MIN_LITTER_SLOT, new ItemBuilder(Material.BREAD)
                .setName(Component.text("Camada mín.: " + current.minLitterSize(), NamedTextColor.WHITE))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY)).build());

        setItem(MAX_LITTER_SLOT, new ItemBuilder(Material.CAKE)
                .setName(Component.text("Camada máx.: " + current.maxLitterSize(), NamedTextColor.WHITE))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY)).build());

        setItem(FERTILITY_SLOT, new ItemBuilder(Material.RABBIT_FOOT)
                .setName(Component.text(String.format(Locale.ROOT, "Fertilidad base: %.0f%%", current.baseFertility() * 100),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +5% · Click derecho: -5%", NamedTextColor.GRAY)).build());

        setItem(DIET_TAGS_SLOT, new ItemBuilder(Material.WHEAT)
                .setName(Component.text("Dieta: " + String.join(", ", current.dietTags()), NamedTextColor.GOLD))
                .setLore(Component.text("Escribí tags separados por comas — vacío = cualquier alimento", NamedTextColor.GRAY))
                .build());

        setItem(BASE_PRODUCTION_SLOT, new ItemBuilder(Material.BUCKET)
                .setName(Component.text("Producción base", NamedTextColor.AQUA))
                .setLore(ItemBuilder.toLoreLines(formatProduction(current.baseProduction())
                        + "\nEscribí 'clave=valor,clave=valor' (ej. milk=3,leather=1)"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private String ticksToMinutes(long ticks) {
        return String.format(Locale.ROOT, "%.1f min (%d ticks)", ticks / 1200.0, ticks);
    }

    private String formatProduction(Map<String, Double> production) {

        if (production.isEmpty()) {
            return "(sin productos configurados)";
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
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(withDisplayName(value)));
        } else if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material del ícono:", value -> replace(withIcon(value)));
        } else if (slot == ENTITY_TYPE_SLOT) {
            chatPromptManager.prompt(player, "Escribí el EntityType (COW, CHICKEN, SHEEP...):",
                    value -> replace(withEntityType(value)));
        } else if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(withDescription(value)));
        } else if (slot == PRODUCT_TYPES_SLOT) {
            chatPromptManager.prompt(player, "Escribí los tipos de producto separados por comas:",
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
            chatPromptManager.prompt(player, "Escribí los tags de dieta separados por comas:",
                    value -> replace(withDietTags(parseSet(value))));
        } else if (slot == BASE_PRODUCTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí 'clave=valor,clave=valor':",
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
