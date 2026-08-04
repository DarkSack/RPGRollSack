package com.sack.rpgroll.fishing.gui;

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
    private final Runnable onBack;
    private FishSpecies current;

    public SpeciesEditorGUI(Player player, FishSpecies species, FishSpeciesManager speciesManager,
            BaitManager baitManager, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Especie: " + species.id(), NamedTextColor.GOLD), SIZE);
        this.current = species;
        this.speciesManager = speciesManager;
        this.baitManager = baitManager;
        this.chatPromptManager = chatPromptManager;
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
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW)).build());

        setItem(ICON_SLOT, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(current.icon()))
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW)).build());

        setItem(MODEL_DATA_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text("Custom Model Data: " + current.customModelData(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Click derecho: -1 · 0 = sin modelo custom", NamedTextColor.GRAY))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? "(sin descripción)" : current.description()))
                .build());

        setItem(CATEGORY_SLOT, new ItemBuilder(Material.TROPICAL_FISH)
                .setName(Component.text("Categoría: " + current.category(), NamedTextColor.AQUA))
                .setLore(Component.text("Click para pasar a la siguiente", NamedTextColor.GRAY)).build());

        setItem(RARITY_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text("Rareza: " + current.rarity(), NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text("Click para pasar a la siguiente", NamedTextColor.GRAY)).build());

        setItem(WATER_TYPES_SLOT, new ItemBuilder(Material.WATER_BUCKET)
                .setName(Component.text("Tipos de agua: " + joinEnum(current.waterTypes()), NamedTextColor.BLUE))
                .setLore(Component.text("Vacío = cualquiera. Escribí la lista separada por comas.", NamedTextColor.GRAY))
                .build());

        setItem(BIOMES_SLOT, new ItemBuilder(Material.GRASS_BLOCK)
                .setName(Component.text("Biomas: " + String.join(", ", current.biomes()), NamedTextColor.GREEN))
                .setLore(Component.text("Vacío = cualquiera. Nombres de Biome de Bukkit.", NamedTextColor.GRAY))
                .build());

        setItem(DEPTHS_SLOT, new ItemBuilder(Material.PRISMARINE_SHARD)
                .setName(Component.text("Profundidades: " + joinEnum(current.depths()), NamedTextColor.AQUA))
                .setLore(Component.text("Vacío = cualquiera.", NamedTextColor.GRAY)).build());

        setItem(WEIGHT_SLOT, new ItemBuilder(Material.IRON_INGOT)
                .setName(Component.text(
                        String.format(Locale.ROOT, "Peso: %.2f - %.2f kg", current.minWeight(), current.maxWeight()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Escribí: MIN MAX", NamedTextColor.GRAY)).build());

        setItem(LENGTH_SLOT, new ItemBuilder(Material.STICK)
                .setName(Component.text(
                        String.format(Locale.ROOT, "Longitud: %.1f - %.1f cm", current.minLength(), current.maxLength()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Escribí: MIN MAX", NamedTextColor.GRAY)).build());

        setItem(PRICE_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(Component.text(String.format(Locale.ROOT, "Precio base: %.1f", current.basePrice()),
                        NamedTextColor.GOLD))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY)).build());

        setItem(XP_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text("Experiencia base: " + current.baseExperience(), NamedTextColor.GREEN))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY)).build());

        setItem(BEHAVIOR_SLOT, new ItemBuilder(Material.SPIDER_EYE)
                .setName(Component.text("Comportamiento: " + current.behavior(), NamedTextColor.RED))
                .setLore(Component.text("Click para pasar al siguiente — afecta el minijuego RPG", NamedTextColor.GRAY))
                .build());

        setItem(SEASONS_SLOT, new ItemBuilder(Material.SUNFLOWER)
                .setName(Component.text("Estaciones: " + String.join(", ", current.allowedSeasons()), NamedTextColor.GREEN))
                .setLore(Component.text("Vacío = cualquiera. Ids de RPGRoll-Seasons.", NamedTextColor.GRAY)).build());

        setItem(WEATHERS_SLOT, new ItemBuilder(Material.LIGHTNING_ROD)
                .setName(Component.text("Climas: " + joinEnum(current.allowedWeathers()), NamedTextColor.AQUA))
                .setLore(Component.text("Vacío = cualquiera.", NamedTextColor.GRAY)).build());

        setItem(TIMES_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Horas: " + joinEnum(current.allowedTimes()), NamedTextColor.YELLOW))
                .setLore(Component.text("Vacío = cualquiera.", NamedTextColor.GRAY)).build());

        setItem(BAIT_TAGS_SLOT, new ItemBuilder(Material.STRING)
                .setName(Component.text("Tags de carnada: " + String.join(", ", current.attractedByBaitTags()),
                        NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text("Vacío = cualquier carnada sirve igual.", NamedTextColor.GRAY)).build());

        setItem(LEGENDARY_SLOT, new ItemBuilder(current.legendary() ? Material.DRAGON_EGG : Material.GRAY_DYE)
                .setName(Component.text("Legendario: " + current.legendary(), NamedTextColor.GOLD))
                .setLore(Component.text("Click para alternar", NamedTextColor.GRAY)).build());

        setItem(LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text("Nivel requerido: " + current.requiredLevel(), NamedTextColor.YELLOW))
                .setLore(Component.text("Solo aplica si es legendario. Click: +1 · Click derecho: -1", NamedTextColor.GRAY))
                .build());

        setItem(FULL_MOON_SLOT, new ItemBuilder(current.requiresFullMoon() ? Material.GLOWSTONE : Material.GRAY_DYE)
                .setName(Component.text("Requiere luna llena: " + current.requiresFullMoon(), NamedTextColor.AQUA))
                .setLore(Component.text("Solo aplica si es legendario. Click para alternar", NamedTextColor.GRAY))
                .build());

        setItem(REQUIRED_BAIT_SLOT, new ItemBuilder(Material.STRING)
                .setName(Component.text(
                        "Carnada requerida: " + (current.requiredBaitId() == null ? "(ninguna)" : current.requiredBaitId()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Solo aplica si es legendario.", NamedTextColor.GRAY)).build());

        setItem(CATCH_EFFECT_SLOT, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(Component.text(
                        "Efecto SackEffects: " + (current.catchEffectId() == null ? "(ninguno)" : current.catchEffectId()),
                        NamedTextColor.YELLOW))
                .build());

        setItem(CATCH_STATUS_EFFECT_SLOT, new ItemBuilder(Material.POTION)
                .setName(Component.text(
                        "Efecto de estado: " + (current.catchStatusEffectId() == null ? "(ninguno)" : current.catchStatusEffectId()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Id de un efecto de RPGRoll-Effects — ej. anguila eléctrica aturde al pescador.",
                        NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
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
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(f -> f.displayName = value));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material del ícono:", value -> replace(f -> f.icon = value));
            return;
        }

        if (slot == MODEL_DATA_SLOT) {
            replace(f -> f.customModelData = Math.max(0, current.customModelData() + sign));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(f -> f.description = value));
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
            chatPromptManager.prompt(player, "Escribí: RIVER,LAKE,SWAMP,OCEAN,DEEP_OCEAN,LAVA,MAGIC_WATER,CORRUPTED_WATER",
                    value -> replace(f -> f.waterTypes = parseEnumSet(WaterType.class, value)));
            return;
        }

        if (slot == BIOMES_SLOT) {
            chatPromptManager.prompt(player, "Escribí los biomas separados por comas (nombres de Biome de Bukkit):",
                    value -> replace(f -> f.biomes = parseStringSet(value)));
            return;
        }

        if (slot == DEPTHS_SLOT) {
            chatPromptManager.prompt(player, "Escribí: SURFACE,MID_WATER,BOTTOM,UNDERWATER_CAVE",
                    value -> replace(f -> f.depths = parseEnumSet(DepthRequirement.class, value)));
            return;
        }

        if (slot == WEIGHT_SLOT) {
            chatPromptManager.prompt(player, "Escribí: MIN MAX (kg):", value -> {
                double[] range = parseRange(value, current.minWeight(), current.maxWeight());
                replace(f -> {
                    f.minWeight = range[0];
                    f.maxWeight = range[1];
                });
            });
            return;
        }

        if (slot == LENGTH_SLOT) {
            chatPromptManager.prompt(player, "Escribí: MIN MAX (cm):", value -> {
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
            chatPromptManager.prompt(player, "Escribí los ids de estación separados por comas (RPGRoll-Seasons):",
                    value -> replace(f -> f.allowedSeasons = parseStringSet(value)));
            return;
        }

        if (slot == WEATHERS_SLOT) {
            chatPromptManager.prompt(player, "Escribí: SUNNY,RAIN,STORM,SNOW",
                    value -> replace(f -> f.allowedWeathers = parseEnumSet(WeatherType.class, value)));
            return;
        }

        if (slot == TIMES_SLOT) {
            chatPromptManager.prompt(player, "Escribí: DAY,NIGHT,DAWN,DUSK,NOON,MIDNIGHT",
                    value -> replace(f -> f.allowedTimes = parseEnumSet(TimeRequirement.class, value)));
            return;
        }

        if (slot == BAIT_TAGS_SLOT) {
            chatPromptManager.prompt(player, "Escribí los tags de carnada separados por comas:",
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
            chatPromptManager.prompt(player, "Escribí el id de la carnada requerida (o 'ninguna'):", value -> {

                if (value.equalsIgnoreCase("ninguna")) {
                    replace(f -> f.requiredBaitId = null);
                    return;
                }

                if (baitManager.get(value.trim().toLowerCase(Locale.ROOT)).isEmpty()) {
                    player.sendMessage(Component.text("No existe esa carnada.", NamedTextColor.RED));
                    return;
                }

                replace(f -> f.requiredBaitId = value.trim().toLowerCase(Locale.ROOT));
            });
            return;
        }

        if (slot == CATCH_EFFECT_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id del efecto de SackEffects (o 'ninguno'):",
                    value -> replace(f -> f.catchEffectId = value.equalsIgnoreCase("ninguno") ? null : value.trim()));
            return;
        }

        if (slot == CATCH_STATUS_EFFECT_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id del efecto de RPGRoll-Effects (o 'ninguno'):",
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
