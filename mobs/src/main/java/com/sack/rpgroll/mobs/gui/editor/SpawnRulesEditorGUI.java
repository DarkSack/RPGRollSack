package com.sack.rpgroll.mobs.gui.editor;

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

    public SpawnRulesEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, Component.text("Spawn: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        SpawnRules rules = session.spawnRules;

        setItem(NATURAL_SPAWN_SLOT, new ItemBuilder(rules.naturalSpawn() ? Material.GRASS_BLOCK : Material.BARRIER)
                .setName(Component.text("Spawn natural: " + rules.naturalSpawn(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para alternar", NamedTextColor.GRAY))
                .build());

        setItem(MIN_HEIGHT_SLOT, new ItemBuilder(Material.LADDER)
                .setName(Component.text("Altura mínima: " + rules.minHeight(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +8 · Shift-click: +32", NamedTextColor.GRAY))
                .build());

        setItem(MAX_HEIGHT_SLOT, new ItemBuilder(Material.SCAFFOLDING)
                .setName(Component.text("Altura máxima: " + rules.maxHeight(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +8 · Shift-click: +32 · Derecho: -8/-32", NamedTextColor.GRAY))
                .build());

        setItem(HOUR_RANGE_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Rango horario: " + (rules.hasTimeRange()
                        ? rules.hourMin() + "-" + rules.hourMax() : "(cualquiera)"), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: escribir min-max (0-23)", NamedTextColor.GRAY),
                        Component.text("Shift-click: quitar", NamedTextColor.GRAY))
                .build());

        setItem(WEATHER_SLOT, new ItemBuilder(Material.WATER_BUCKET)
                .setName(Component.text("Clima: " + (rules.weather() != null ? rules.weather() : "any"),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click para pasar al siguiente (any/clear/rain/thunder)", NamedTextColor.GRAY))
                .build());

        setItem(MIN_DISTANCE_SLOT, new ItemBuilder(Material.SPYGLASS)
                .setName(Component.text("Distancia mínima a jugadores: " + rules.minDistanceFromPlayers(),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +4 · Shift-click: +16 · Derecho: -4/-16", NamedTextColor.GRAY))
                .build());

        setItem(SPAWN_WEIGHT_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(Component.text("Peso de spawn: " + rules.spawnWeight(), NamedTextColor.YELLOW))
                .setLore(Component.text("Usado para elegir entre varios mobs candidatos", NamedTextColor.DARK_GRAY),
                        Component.text("Click: +0.5 · Shift-click: +2 · Derecho: -0.5/-2", NamedTextColor.GRAY))
                .build());

        renderList(rules.biomes(), BIOMES_START_SLOT, 4, Material.OAK_SAPLING);
        setItem(ADD_BIOME_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar bioma", NamedTextColor.GREEN)).build());

        renderList(rules.worlds(), WORLDS_START_SLOT, 3, Material.GRASS_BLOCK);
        setItem(ADD_WORLD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar mundo", NamedTextColor.GREEN)).build());

        renderList(rules.regions(), REGIONS_START_SLOT, 8, Material.OAK_FENCE_GATE);
        setItem(ADD_REGION_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar región", NamedTextColor.GREEN)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private void renderList(List<String> values, int startSlot, int max, Material material) {
        for (int i = 0; i < values.size() && i < max; i++) {
            setItem(startSlot + i, new ItemBuilder(material)
                    .setName(Component.text(values.get(i), NamedTextColor.AQUA))
                    .setLore(Component.text("Shift-click para quitar", NamedTextColor.DARK_GRAY))
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
            session.chatPromptManager.prompt(player, "Escribí el rango horario (ej. 19-6):", value -> {

                String[] parts = value.trim().split("-");
                if (parts.length != 2) {
                    player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                    return;
                }

                try {
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    session.spawnRules = with(rules.biomes(), rules.regions(), rules.worlds(), min, max,
                            rules.weather(), rules.minHeight(), rules.maxHeight(), rules.minDistanceFromPlayers(),
                            rules.naturalSpawn(), rules.spawnWeight());
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Horas numéricas inválidas.", NamedTextColor.RED));
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
            session.chatPromptManager.prompt(player, "Escribí la key del bioma (ej. plains, dark_forest):", value -> {
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
            session.chatPromptManager.prompt(player, "Escribí el nombre del mundo:", value -> {
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
            session.chatPromptManager.prompt(player, "Escribí el id de región:", value -> {
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
