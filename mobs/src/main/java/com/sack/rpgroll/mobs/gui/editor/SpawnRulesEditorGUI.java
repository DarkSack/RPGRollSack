package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.SpawnRules;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Editor de reglas de aparición natural: biomas, mundos, región, altura, hora, clima, distancia y peso. */
public class SpawnRulesEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NATURAL_SPAWN_SLOT = 9;
    private static final int MIN_HEIGHT_SLOT = 10;
    private static final int MAX_HEIGHT_SLOT = 11;
    private static final int HOUR_RANGE_SLOT = 12;
    private static final int WEATHER_SLOT = 13;
    private static final int MIN_DISTANCE_SLOT = 14;
    private static final int SPAWN_WEIGHT_SLOT = 15;

    private static final int BIOMES_START_SLOT = 18;
    private static final int ADD_BIOME_SLOT = 22;
    private static final int WORLDS_START_SLOT = 23;
    private static final int ADD_WORLD_SLOT = 26;

    private static final int REGIONS_START_SLOT = 27;
    private static final int ADD_REGION_SLOT = 35;

    private static final int BACK_SLOT = 44;

    private final MobEditorSession session;
    private final Runnable onBack;
    private final LangManager lang;

    public SpawnRulesEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("gui.spawn_rules.title", "id",
                session.original.id()), SIZE);
        this.session = session;
        this.onBack = onBack;
        this.lang = session.chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        SpawnRules rules = session.spawnRules;

        setItem(NATURAL_SPAWN_SLOT, new ItemBuilder(rules.naturalSpawn() ? Material.GRASS_BLOCK : Material.BARRIER)
                .setName(lang.component("gui.spawn_rules.natural_label", "value", rules.naturalSpawn()))
                .setLore(lang.component("gui.common.click_toggle"))
                .build());

        setItem(MIN_HEIGHT_SLOT, new ItemBuilder(Material.LADDER)
                .setName(lang.component("gui.spawn_rules.min_height_label", "value", rules.minHeight()))
                .setLore(lang.component("gui.spawn_rules.height_hint1"))
                .build());

        setItem(MAX_HEIGHT_SLOT, new ItemBuilder(Material.SCAFFOLDING)
                .setName(lang.component("gui.spawn_rules.max_height_label", "value", rules.maxHeight()))
                .setLore(lang.component("gui.spawn_rules.max_height_hint"))
                .build());

        setItem(HOUR_RANGE_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(lang.component("gui.spawn_rules.hour_range_label", "value", rules.hasTimeRange()
                        ? rules.hourMin() + "-" + rules.hourMax() : lang.raw("gui.spawn_rules.any_label")))
                .setLore(lang.component("gui.spawn_rules.hour_hint1"),
                        lang.component("gui.common.shift_remove_hint"))
                .build());

        setItem(WEATHER_SLOT, new ItemBuilder(Material.WATER_BUCKET)
                .setName(lang.component("gui.spawn_rules.weather_label", "value",
                        rules.weather() != null ? rules.weather() : "any"))
                .setLore(lang.component("gui.spawn_rules.weather_hint"))
                .build());

        setItem(MIN_DISTANCE_SLOT, new ItemBuilder(Material.SPYGLASS)
                .setName(lang.component("gui.spawn_rules.min_distance_label", "value",
                        rules.minDistanceFromPlayers()))
                .setLore(lang.component("gui.spawn_rules.min_distance_hint"))
                .build());

        setItem(SPAWN_WEIGHT_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(lang.component("gui.spawn_rules.weight_label", "value", rules.spawnWeight()))
                .setLore(lang.component("gui.spawn_rules.weight_note"),
                        lang.component("gui.spawn_rules.weight_hint"))
                .build());

        renderList(rules.biomes(), BIOMES_START_SLOT, 4, Material.OAK_SAPLING);
        setItem(ADD_BIOME_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.spawn_rules.add_biome")).build());

        renderList(rules.worlds(), WORLDS_START_SLOT, 3, Material.GRASS_BLOCK);
        setItem(ADD_WORLD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.spawn_rules.add_world")).build());

        renderList(rules.regions(), REGIONS_START_SLOT, 8, Material.OAK_FENCE_GATE);
        setItem(ADD_REGION_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.spawn_rules.add_region")).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    private void renderList(List<String> values, int startSlot, int max, Material material) {
        for (int i = 0; i < values.size() && i < max; i++) {
            setItem(startSlot + i, new ItemBuilder(material)
                    .setName(Component.text(values.get(i), NamedTextColor.AQUA))
                    .setLore(lang.component("gui.common.shift_remove_dark"))
                    .build());
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        SpawnRules rules = session.spawnRules;

        if (slot == NATURAL_SPAWN_SLOT) {
            session.spawnRules = with(rules.biomes(), rules.regions(), rules.worlds(), rules.hourMin(),
                    rules.hourMax(), rules.weather(), rules.minHeight(), rules.maxHeight(),
                    rules.minDistanceFromPlayers(), !rules.naturalSpawn(), rules.spawnWeight());
            build();
            return;
        }

        if (slot == MIN_HEIGHT_SLOT) {
            int updated = rules.minHeight() + (int) delta(event.getClick(), 8, 32);
            session.spawnRules = with(rules.biomes(), rules.regions(), rules.worlds(), rules.hourMin(),
                    rules.hourMax(), rules.weather(), updated, rules.maxHeight(), rules.minDistanceFromPlayers(),
                    rules.naturalSpawn(), rules.spawnWeight());
            build();
            return;
        }

        if (slot == MAX_HEIGHT_SLOT) {
            int updated = rules.maxHeight() + (int) delta(event.getClick(), 8, 32);
            session.spawnRules = with(rules.biomes(), rules.regions(), rules.worlds(), rules.hourMin(),
                    rules.hourMax(), rules.weather(), rules.minHeight(), updated, rules.minDistanceFromPlayers(),
                    rules.naturalSpawn(), rules.spawnWeight());
            build();
            return;
        }

        if (slot == HOUR_RANGE_SLOT) {
            if (event.isShiftClick()) {
                session.spawnRules = with(rules.biomes(), rules.regions(), rules.worlds(), -1, -1,
                        rules.weather(), rules.minHeight(), rules.maxHeight(), rules.minDistanceFromPlayers(),
                        rules.naturalSpawn(), rules.spawnWeight());
                build();
                return;
            }
            session.chatPromptManager.prompt(player, "gui.spawn_rules.prompt_hour_range", value -> {

                String[] parts = value.trim().split("-");
                if (parts.length != 2) {
                    lang.send(player, "gui.common.invalid_format");
                    return;
                }

                try {
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    session.spawnRules = with(rules.biomes(), rules.regions(), rules.worlds(), min, max,
                            rules.weather(), rules.minHeight(), rules.maxHeight(), rules.minDistanceFromPlayers(),
                            rules.naturalSpawn(), rules.spawnWeight());
                } catch (NumberFormatException e) {
                    lang.send(player, "gui.spawn_rules.invalid_hours");
                    return;
                }

                build();
            });
            return;
        }

        if (slot == WEATHER_SLOT) {
            String[] cycle = {"any", "clear", "rain", "thunder"};
            String current = rules.weather() != null ? rules.weather().toLowerCase(Locale.ROOT) : "any";
            int next = (indexOf(cycle, current) + 1) % cycle.length;
            session.spawnRules = with(rules.biomes(), rules.regions(), rules.worlds(), rules.hourMin(),
                    rules.hourMax(), cycle[next], rules.minHeight(), rules.maxHeight(),
                    rules.minDistanceFromPlayers(), rules.naturalSpawn(), rules.spawnWeight());
            build();
            return;
        }

        if (slot == MIN_DISTANCE_SLOT) {
            double updated = Math.max(0, rules.minDistanceFromPlayers() + delta(event.getClick(), 4, 16));
            session.spawnRules = with(rules.biomes(), rules.regions(), rules.worlds(), rules.hourMin(),
                    rules.hourMax(), rules.weather(), rules.minHeight(), rules.maxHeight(), updated,
                    rules.naturalSpawn(), rules.spawnWeight());
            build();
            return;
        }

        if (slot == SPAWN_WEIGHT_SLOT) {
            double updated = Math.max(0.1, rules.spawnWeight() + delta(event.getClick(), 0.5, 2));
            session.spawnRules = with(rules.biomes(), rules.regions(), rules.worlds(), rules.hourMin(),
                    rules.hourMax(), rules.weather(), rules.minHeight(), rules.maxHeight(),
                    rules.minDistanceFromPlayers(), rules.naturalSpawn(), updated);
            build();
            return;
        }

        if (slot >= BIOMES_START_SLOT && slot < BIOMES_START_SLOT + Math.min(rules.biomes().size(), 4)) {
            if (event.isShiftClick()) {
                removeFromList(rules.biomes(), slot - BIOMES_START_SLOT, updatedList -> with(updatedList,
                        rules.regions(), rules.worlds(), rules.hourMin(), rules.hourMax(), rules.weather(),
                        rules.minHeight(), rules.maxHeight(), rules.minDistanceFromPlayers(), rules.naturalSpawn(),
                        rules.spawnWeight()));
            }
            return;
        }

        if (slot == ADD_BIOME_SLOT) {
            session.chatPromptManager.prompt(player, "gui.spawn_rules.prompt_biome", value -> {
                addToList(rules.biomes(), value, updatedList -> with(updatedList, rules.regions(),
                        rules.worlds(), rules.hourMin(), rules.hourMax(), rules.weather(), rules.minHeight(),
                        rules.maxHeight(), rules.minDistanceFromPlayers(), rules.naturalSpawn(), rules.spawnWeight()));
            });
            return;
        }

        if (slot >= WORLDS_START_SLOT && slot < WORLDS_START_SLOT + Math.min(rules.worlds().size(), 3)) {
            if (event.isShiftClick()) {
                removeFromList(rules.worlds(), slot - WORLDS_START_SLOT, updatedList -> with(rules.biomes(),
                        rules.regions(), updatedList, rules.hourMin(), rules.hourMax(), rules.weather(),
                        rules.minHeight(), rules.maxHeight(), rules.minDistanceFromPlayers(), rules.naturalSpawn(),
                        rules.spawnWeight()));
            }
            return;
        }

        if (slot == ADD_WORLD_SLOT) {
            session.chatPromptManager.prompt(player, "gui.spawn_rules.prompt_world", value -> {
                addToList(rules.worlds(), value, updatedList -> with(rules.biomes(), rules.regions(),
                        updatedList, rules.hourMin(), rules.hourMax(), rules.weather(), rules.minHeight(),
                        rules.maxHeight(), rules.minDistanceFromPlayers(), rules.naturalSpawn(), rules.spawnWeight()));
            });
            return;
        }

        if (slot >= REGIONS_START_SLOT && slot < REGIONS_START_SLOT + Math.min(rules.regions().size(), 8)) {
            if (event.isShiftClick()) {
                removeFromList(rules.regions(), slot - REGIONS_START_SLOT, updatedList -> with(
                        rules.biomes(), updatedList, rules.worlds(), rules.hourMin(), rules.hourMax(),
                        rules.weather(), rules.minHeight(), rules.maxHeight(), rules.minDistanceFromPlayers(),
                        rules.naturalSpawn(), rules.spawnWeight()));
            }
            return;
        }

        if (slot == ADD_REGION_SLOT) {
            session.chatPromptManager.prompt(player, "gui.common.prompt_region_id", value -> {
                addToList(rules.regions(), value, updatedList -> with(rules.biomes(), updatedList,
                        rules.worlds(), rules.hourMin(), rules.hourMax(), rules.weather(), rules.minHeight(),
                        rules.maxHeight(), rules.minDistanceFromPlayers(), rules.naturalSpawn(), rules.spawnWeight()));
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void addToList(List<String> current, String value, java.util.function.Function<List<String>, SpawnRules> rebuild) {
        List<String> updated = new ArrayList<>(current);
        updated.add(value.trim());
        session.spawnRules = rebuild.apply(updated);
        build();
    }

    private void removeFromList(List<String> current, int index, java.util.function.Function<List<String>, SpawnRules> rebuild) {
        List<String> updated = new ArrayList<>(current);
        updated.remove(index);
        session.spawnRules = rebuild.apply(updated);
        build();
    }

    private int indexOf(String[] values, String target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(target)) {
                return i;
            }
        }
        return 0;
    }

    private double delta(ClickType click, double small, double large) {
        return switch (click) {
            case LEFT -> small;
            case SHIFT_LEFT -> large;
            case RIGHT -> -small;
            case SHIFT_RIGHT -> -large;
            default -> 0;
        };
    }

    private SpawnRules with(List<String> biomes, List<String> regions, List<String> worlds,
            int hourMin, int hourMax, String weather, int minHeight, int maxHeight, double minDistance,
            boolean naturalSpawn, double spawnWeight) {
        return new SpawnRules(biomes, regions, worlds, hourMin, hourMax, weather, minHeight, maxHeight, minDistance,
                naturalSpawn, spawnWeight);
    }

}
