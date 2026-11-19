package com.sack.rpgroll.machines.area;

import com.sack.rpgroll.machines.core.Cost;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Menu;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Recipe;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Step;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Track;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Lo que comparten los ficheros de las máquinas de área ({@code fishers.yml}, {@code farms.yml},
 * {@code lumberjacks.yml}): bloque, modelo, menú, tope por jugador, claim, rendimiento, mejoras con
 * niveles y receta. Lo propio de cada tipo lo lee el tipo de {@link #config()}.
 */
public record AreaSettings(boolean enabled, Set<String> disabledWorlds, Material block, String itemModel,
        boolean frame, String frameModel, float frameScale, Menu menu, int maxPerPlayer, boolean requireClaim,
        int actionsPerTick, int scansPerTick, Map<String, Track> upgrades, Recipe recipe,
        ConfigurationSection config) {

    private static final Track NONE = new Track(Material.EXPERIENCE_BOTTLE, List.of(new Step(0, Cost.FREE)));

    public static AreaSettings from(FileConfiguration config, String file, Material defaultBlock,
            List<String> upgradeIds, Consumer<String> warn) {
        String blockName = config.getString("block");
        Material block = blockName == null ? defaultBlock : Material.matchMaterial(blockName);
        if (blockName != null && (block == null || !block.isBlock() || !block.isItem() || !block.isSolid())) {
            warn.accept(file + ": 'block' tiene que ser un bloque sólido; se usa " + defaultBlock);
            block = defaultBlock;
        }

        Map<String, Track> upgrades = new LinkedHashMap<>();
        for (String id : upgradeIds) {
            ConfigurationSection section = config.getConfigurationSection("upgrades." + id);
            List<Step> steps = new ArrayList<>();
            if (section != null && section.getBoolean("enabled", true)) {
                for (Map<?, ?> raw : section.getMapList("levels")) {
                    if (!(raw.get("value") instanceof Number number)) {
                        warn.accept(file + " (" + id + "): un nivel sin 'value' numérico; se ignora");
                        continue;
                    }
                    steps.add(new Step(number.doubleValue(), cost(raw.get("cost"), warn, file + " (" + id + ")")));
                }
            }
            Material icon = section == null ? null : Material.matchMaterial(section.getString("icon", ""));
            upgrades.put(id, steps.isEmpty() ? NONE
                    : new Track(icon == null || !icon.isItem() ? NONE.icon() : icon, List.copyOf(steps)));
        }

        Map<Character, Material> ingredients = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("recipe.ingredients");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Material material = Material.matchMaterial(section.getString(key, ""));
                if (key.length() != 1 || material == null || !material.isItem()) {
                    warn.accept(file + " (recipe): ingrediente '" + key + "' no válido");
                    continue;
                }
                ingredients.put(key.charAt(0), material);
            }
        }

        return new AreaSettings(config.getBoolean("enabled", true),
                config.getStringList("disabled-worlds").stream().map(w -> w.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toUnmodifiableSet()),
                block,
                config.getString("item-model", ""),
                config.getBoolean("frame.enabled", false),
                config.getString("frame.model", ""),
                (float) config.getDouble("frame.scale", 1.002),
                Menu.from(config.getConfigurationSection("menu")),
                config.getInt("max-per-player", 2),
                config.getBoolean("require-claim", true),
                Math.max(1, config.getInt("performance.actions-per-tick", 8)),
                Math.max(1, config.getInt("performance.scans-per-tick", 64)),
                Collections.unmodifiableMap(upgrades),
                new Recipe(config.getBoolean("recipe.enabled", false), config.getStringList("recipe.shape"),
                        Map.copyOf(ingredients)),
                config);
    }

    private static Cost cost(Object raw, Consumer<String> warn, String where) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Cost.FREE;
        }
        return Cost.from(new MemoryConfiguration().createSection("cost", map), text -> warn.accept(where + ": " + text));
    }

    /** La mejora {@code id}; una que no está en el fichero es un solo nivel con valor 0. */
    public Track track(String id) {
        return upgrades.getOrDefault(id, NONE);
    }

    /** Valor de la mejora {@code id} en el nivel que tiene la máquina. */
    public double value(String id, AreaMachine machine) {
        return track(id).value(machine.level(id));
    }

    public boolean allowed(String world) {
        return enabled && !disabledWorlds.contains(world.toLowerCase(Locale.ROOT));
    }
}
