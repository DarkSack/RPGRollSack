package com.sack.rpgroll.seasons.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.seasons.core.ClimateProfile;
import com.sack.rpgroll.seasons.core.DurationUnit;
import com.sack.rpgroll.seasons.core.Season;
import com.sack.rpgroll.seasons.core.SeasonManager;
import com.sack.rpgroll.seasons.core.VegetationEffectType;
import com.sack.rpgroll.seasons.core.WorldEventManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Identidad, duración, clima, tags, temperaturas por bioma, efectos de
 * vegetación e ids de eventos mundiales elegibles de una estación — en
 * una sola pantalla vía chat, salvo subestaciones y mobs de temporada
 * (demasiado estructurados, tienen su propia pantalla).
 */
public class SeasonEditorHubGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private static final int NAME_SLOT = 0;
    private static final int ICON_SLOT = 1;
    private static final int COLOR_SLOT = 2;
    private static final int DESCRIPTION_SLOT = 3;
    private static final int DURATION_SLOT = 4;

    private static final int CLIMATE_SLOT = 9;
    private static final int EXCLUSIVE_BOSS_SLOT = 10;
    private static final int WORLD_EVENT_CHANCE_SLOT = 11;
    private static final int TAGS_SLOT = 12;
    private static final int BIOME_MODIFIERS_SLOT = 13;
    private static final int VEGETATION_SLOT = 14;

    private static final int SUB_SEASONS_SLOT = 18;
    private static final int MOB_MODIFIERS_SLOT = 19;
    private static final int WORLD_EVENTS_SLOT = 20;

    private static final int BACK_SLOT = 49;

    private final SeasonManager seasonManager;
    private final WorldEventManager worldEventManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Season current;

    public SeasonEditorHubGUI(Player player, Season season, SeasonManager seasonManager,
            WorldEventManager worldEventManager, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Estación: " + season.id(), NamedTextColor.GOLD), SIZE);
        this.current = season;
        this.seasonManager = seasonManager;
        this.worldEventManager = worldEventManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Season updated) {
        current = updated;
        seasonManager.save(current);
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

        setItem(ICON_SLOT, new ItemBuilder(SeasonBrowserGUI.parseMaterial(current.icon()))
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW)).build());

        setItem(COLOR_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text("Color: " + current.color(), SeasonBrowserGUI.parseColor(current.color())))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? "(sin descripción)" : current.description()))
                .build());

        setItem(DURATION_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Duración: " + current.durationAmount() + " " + current.durationUnit(),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Escribí: CANTIDAD UNIDAD (ej. 7 MINECRAFT_DAYS)", NamedTextColor.GRAY))
                .build());

        ClimateProfile climate = current.climate();
        setItem(CLIMATE_SLOT, new ItemBuilder(Material.WATER_BUCKET)
                .setName(Component.text("Clima", NamedTextColor.AQUA))
                .setLore(
                        Component.text("rain=" + climate.rainChance() + " storm=" + climate.stormChance(),
                                NamedTextColor.GRAY),
                        Component.text("temp=" + climate.baseTemperature() + " snow=" + climate.snowChance(),
                                NamedTextColor.GRAY),
                        Component.text("Click para reescribir todos los valores", NamedTextColor.DARK_GRAY))
                .build());

        setItem(EXCLUSIVE_BOSS_SLOT, new ItemBuilder(Material.DRAGON_HEAD)
                .setName(Component.text(
                        "Jefe exclusivo: " + (current.exclusiveBossId() == null ? "(ninguno)" : current.exclusiveBossId()),
                        NamedTextColor.RED))
                .setLore(Component.text("Escribí un id de mob de RPGRoll-Mobs (o 'ninguno')", NamedTextColor.GRAY))
                .build());

        setItem(WORLD_EVENT_CHANCE_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text("Chance diaria de evento mundial: "
                        + Math.round(current.worldEventDailyChance() * 100) + "%", NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text("Click: +5% · Click derecho: -5%", NamedTextColor.GRAY)).build());

        setItem(TAGS_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Tags: " + String.join(", ", current.tags()), NamedTextColor.YELLOW))
                .setLore(Component.text("Escribí la lista separada por comas", NamedTextColor.GRAY)).build());

        setItem(BIOME_MODIFIERS_SLOT, new ItemBuilder(Material.GRASS_BLOCK)
                .setName(Component.text("Modificadores de bioma: " + current.biomeTemperatureModifiers().size(),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Escribí: bioma=delta,bioma2=delta2 (ej. taiga=-10,desert=5)",
                        NamedTextColor.GRAY))
                .build());

        setItem(VEGETATION_SLOT, new ItemBuilder(Material.OAK_SAPLING)
                .setName(Component.text("Vegetación: " + current.vegetationEffects().size(), NamedTextColor.GREEN))
                .setLore(Component.text(
                        current.vegetationEffects().stream().map(Enum::name).collect(Collectors.joining(", ")),
                        NamedTextColor.GRAY),
                        Component.text("Escribí la lista separada por comas (ver tipos en la doc)",
                                NamedTextColor.DARK_GRAY))
                .build());

        setItem(SUB_SEASONS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(Component.text("▶ Subestaciones (" + current.subSeasons().size() + ")", NamedTextColor.GREEN))
                .build());

        setItem(MOB_MODIFIERS_SLOT, new ItemBuilder(Material.ZOMBIE_HEAD)
                .setName(Component.text("▶ Mobs de temporada (" + current.mobModifiers().size() + ")",
                        NamedTextColor.GREEN))
                .build());

        setItem(WORLD_EVENTS_SLOT, new ItemBuilder(Material.END_CRYSTAL)
                .setName(Component.text("Eventos elegibles: " + current.worldEventIds().size(), NamedTextColor.YELLOW))
                .setLore(Component.text("Escribí los ids separados por comas", NamedTextColor.GRAY)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(rebuild(b -> b.displayName = value)));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material del ícono:", value -> replace(rebuild(b -> b.icon = value)));
            return;
        }

        if (slot == COLOR_SLOT) {
            chatPromptManager.prompt(player, "Escribí el color (ej. GREEN, GOLD):", value -> replace(rebuild(b -> b.color = value)));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(rebuild(b -> b.description = value)));
            return;
        }

        if (slot == DURATION_SLOT) {
            chatPromptManager.prompt(player, "Escribí: CANTIDAD UNIDAD (REAL_HOURS, REAL_DAYS, REAL_WEEKS, MINECRAFT_DAYS):",
                    value -> {

                        String[] parts = value.trim().split("\\s+", 2);

                        if (parts.length < 2) {
                            player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                            return;
                        }

                        int amount = parseIntOr(parts[0], current.durationAmount());
                        DurationUnit unit;

                        try {
                            unit = DurationUnit.valueOf(parts[1].trim().toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException e) {
                            player.sendMessage(Component.text("Unidad inválida: " + parts[1], NamedTextColor.RED));
                            return;
                        }

                        replace(rebuild(b -> {
                            b.durationAmount = amount;
                            b.durationUnit = unit;
                        }));
                    });
            return;
        }

        if (slot == CLIMATE_SLOT) {
            chatPromptManager.prompt(player,
                    "Escribí: rain-chance=x,storm-chance=x,snow-chance=x,fog-chance=x,base-temperature=x,"
                            + "temperature-variance=x,wind-strength=x,humidity=x,heatwave-chance=x,thunderstorm-chance=x",
                    this::parseAndReplaceClimate);
            return;
        }

        if (slot == EXCLUSIVE_BOSS_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id del mob jefe (o 'ninguno'):",
                    value -> replace(rebuild(b -> b.exclusiveBossId = value.equalsIgnoreCase("ninguno") ? null : value.trim())));
            return;
        }

        if (slot == WORLD_EVENT_CHANCE_SLOT) {
            double sign = click == ClickType.RIGHT ? -0.05 : 0.05;
            replace(rebuild(b -> b.worldEventDailyChance = Math.max(0, Math.min(1, current.worldEventDailyChance() + sign))));
            return;
        }

        if (slot == TAGS_SLOT) {
            chatPromptManager.prompt(player, "Escribí los tags separados por comas:", value -> replace(rebuild(
                    b -> b.tags = splitToSet(value))));
            return;
        }

        if (slot == BIOME_MODIFIERS_SLOT) {
            chatPromptManager.prompt(player, "Escribí: bioma=delta,bioma2=delta2:", value -> replace(rebuild(
                    b -> b.biomeModifiers = parseNumericMap(value))));
            return;
        }

        if (slot == VEGETATION_SLOT) {
            chatPromptManager.prompt(player,
                    "Escribí: SNOW_LAYERS,ICE_LAKES,DRY_GRASS,FALLING_LEAVES,FLOWER_BOOM (los que apliquen):",
                    value -> replace(rebuild(b -> b.vegetationEffects = parseVegetationEffects(value))));
            return;
        }

        if (slot == SUB_SEASONS_SLOT) {
            new SubSeasonsEditorGUI(player, current, seasonManager, chatPromptManager, this::onSubBack).open();
            return;
        }

        if (slot == MOB_MODIFIERS_SLOT) {
            new MobModifiersEditorGUI(player, current, seasonManager, chatPromptManager, this::onSubBack).open();
            return;
        }

        if (slot == WORLD_EVENTS_SLOT) {
            chatPromptManager.prompt(player, "Escribí los ids de eventos elegibles, separados por comas:", value -> {

                List<String> ids = List.of(value.split("\\s*,\\s*")).stream()
                        .filter(id -> !id.isBlank())
                        .filter(worldEventManager::exists)
                        .toList();

                replace(rebuild(b -> b.worldEventIds = ids));
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void onSubBack() {
        current = seasonManager.get(current.id()).orElse(current);
        build();
    }

    private void parseAndReplaceClimate(String value) {

        Map<String, Double> values = parseNumericMap(value);
        ClimateProfile old = current.climate();

        ClimateProfile updated = new ClimateProfile(
                values.getOrDefault("rain-chance", old.rainChance()),
                values.getOrDefault("storm-chance", old.stormChance()),
                values.getOrDefault("snow-chance", old.snowChance()),
                values.getOrDefault("fog-chance", old.fogChance()),
                values.getOrDefault("base-temperature", old.baseTemperature()),
                values.getOrDefault("temperature-variance", old.temperatureVariance()),
                values.getOrDefault("wind-strength", old.windStrength()),
                values.getOrDefault("humidity", old.humidity()),
                values.getOrDefault("heatwave-chance", old.heatwaveChance()),
                values.getOrDefault("thunderstorm-chance", old.thunderstormChance()));

        replace(rebuild(b -> b.climate = updated));
    }

    private Map<String, Double> parseNumericMap(String raw) {

        Map<String, Double> result = new LinkedHashMap<>();

        for (String pair : raw.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try {
                    result.put(kv[0].trim().toLowerCase(Locale.ROOT), Double.parseDouble(kv[1].trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return result;
    }

    private Set<String> splitToSet(String raw) {
        Set<String> result = new LinkedHashSet<>();
        for (String entry : raw.split(",")) {
            if (!entry.isBlank()) {
                result.add(entry.trim().toLowerCase(Locale.ROOT));
            }
        }
        return result;
    }

    private Set<VegetationEffectType> parseVegetationEffects(String raw) {

        Set<VegetationEffectType> result = new LinkedHashSet<>();

        for (String entry : raw.split(",")) {
            try {
                result.add(VegetationEffectType.valueOf(entry.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return result;
    }

    private int parseIntOr(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Mutable staging para reconstruir el record inmutable sin repetir sus 16 campos en cada handler. */
    private static final class Fields {
        String displayName;
        String icon;
        String color;
        String description;
        int durationAmount;
        DurationUnit durationUnit;
        ClimateProfile climate;
        String exclusiveBossId;
        double worldEventDailyChance;
        Set<String> tags;
        Map<String, Double> biomeModifiers;
        Set<VegetationEffectType> vegetationEffects;
        List<String> worldEventIds;
    }

    private Season rebuild(java.util.function.Consumer<Fields> mutator) {

        Fields fields = new Fields();
        fields.displayName = current.displayName();
        fields.icon = current.icon();
        fields.color = current.color();
        fields.description = current.description();
        fields.durationAmount = current.durationAmount();
        fields.durationUnit = current.durationUnit();
        fields.climate = current.climate();
        fields.exclusiveBossId = current.exclusiveBossId();
        fields.worldEventDailyChance = current.worldEventDailyChance();
        fields.tags = current.tags();
        fields.biomeModifiers = current.biomeTemperatureModifiers();
        fields.vegetationEffects = current.vegetationEffects();
        fields.worldEventIds = current.worldEventIds();

        mutator.accept(fields);

        return new Season(current.id(), fields.displayName, fields.icon, fields.color, fields.description,
                fields.durationAmount, fields.durationUnit, fields.climate, current.subSeasons(),
                fields.biomeModifiers, fields.vegetationEffects, current.mobModifiers(), fields.exclusiveBossId,
                fields.worldEventIds, fields.worldEventDailyChance, fields.tags);
    }

}
