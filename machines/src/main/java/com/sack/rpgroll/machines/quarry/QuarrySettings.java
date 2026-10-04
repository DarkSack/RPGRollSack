package com.sack.rpgroll.machines.quarry;

import com.sack.rpgroll.machines.core.Cost;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** quarries.yml ya leído. */
public record QuarrySettings(boolean enabled, Set<String> disabledWorlds, Material block, String itemModel,
        boolean frame, String frameModel, float frameScale, int maxPerPlayer, boolean requireClaim,
        int blocksPerTick, int scansPerTick, Set<Material> skip, Set<Material> junk,
        Map<Numeric, Track> tracks, Map<Unlock, Cost> unlocks, Recipe recipe) {

    /** Mejoras con niveles. El valor del nivel 0 es el de una cantera recién puesta. */
    public enum Numeric {
        /** Bloques por segundo. */
        SPEED("speed", Material.SUGAR, 1),
        /** Lado del cuadrado que excava. */
        AREA("area", Material.MAP, 16),
        /** Nivel de fortuna del pico virtual. */
        FORTUNE("fortune", Material.LAPIS_LAZULI, 0),
        /** Nivel de picado para las menas de RPGRoll-Items (-1 = el del pico de netherita). */
        TIER("tier", Material.NETHERITE_PICKAXE, -1);

        private final String id;
        private final Material icon;
        private final double base;

        Numeric(String id, Material icon, double base) {
            this.id = id;
            this.icon = icon;
            this.base = base;
        }

        public String id() {
            return id;
        }

        public Material icon() {
            return icon;
        }
    }

    /** Mejoras de una sola vez, que luego se encienden y apagan desde el menú. */
    public enum Unlock {
        SILK_TOUCH("silk-touch", Material.FEATHER),
        SMELT("smelt", Material.BLAST_FURNACE),
        FILTER("filter", Material.HOPPER);

        private final String id;
        private final Material icon;

        Unlock(String id, Material icon) {
            this.id = id;
            this.icon = icon;
        }

        public String id() {
            return id;
        }

        public Material icon() {
            return icon;
        }
    }

    public record Step(double value, Cost cost) {
    }

    public record Track(Material icon, List<Step> steps) {

        public Step step(int index) {
            return steps.get(Math.clamp(index, 0, steps.size() - 1));
        }

        public double value(int index) {
            return step(index).value();
        }

        public int max() {
            return steps.size() - 1;
        }
    }

    public record Recipe(boolean enabled, List<String> shape, Map<Character, Material> ingredients) {
    }

    public static QuarrySettings from(FileConfiguration config, Consumer<String> warn) {

        Material block = Material.matchMaterial(config.getString("block", "LODESTONE"));
        if (block == null || !block.isBlock() || !block.isItem() || !block.isSolid()) {
            warn.accept("quarries.yml: 'block' tiene que ser un bloque sólido; se usa LODESTONE");
            block = Material.LODESTONE;
        }

        EnumMap<Numeric, Track> tracks = new EnumMap<>(Numeric.class);
        for (Numeric numeric : Numeric.values()) {
            ConfigurationSection section = config.getConfigurationSection("upgrades." + numeric.id());
            List<Step> steps = new ArrayList<>();
            if (section != null && section.getBoolean("enabled", true)) {
                for (Map<?, ?> raw : section.getMapList("levels")) {
                    Object value = raw.get("value");
                    if (!(value instanceof Number number)) {
                        warn.accept("quarries.yml (" + numeric.id() + "): un nivel sin 'value' numérico; se ignora");
                        continue;
                    }
                    steps.add(new Step(number.doubleValue(), cost(raw.get("cost"), warn, numeric.id())));
                }
            }
            if (steps.isEmpty()) {
                steps.add(new Step(numeric.base, Cost.FREE));
            }
            Material icon = section == null ? null : Material.matchMaterial(section.getString("icon", ""));
            tracks.put(numeric, new Track(icon == null || !icon.isItem() ? numeric.icon() : icon, List.copyOf(steps)));
        }

        EnumMap<Unlock, Cost> unlocks = new EnumMap<>(Unlock.class);
        for (Unlock unlock : Unlock.values()) {
            ConfigurationSection section = config.getConfigurationSection("upgrades." + unlock.id());
            if (section != null && section.getBoolean("enabled", true)) {
                unlocks.put(unlock, Cost.from(section.getConfigurationSection("cost"),
                        text -> warn.accept("quarries.yml (" + unlock.id() + "): " + text)));
            }
        }

        Map<Character, Material> ingredients = new java.util.HashMap<>();
        ConfigurationSection ingredientSection = config.getConfigurationSection("recipe.ingredients");
        if (ingredientSection != null) {
            for (String key : ingredientSection.getKeys(false)) {
                Material material = Material.matchMaterial(ingredientSection.getString(key, ""));
                if (key.length() != 1 || material == null || !material.isItem()) {
                    warn.accept("quarries.yml (recipe): ingrediente '" + key + "' no válido");
                    continue;
                }
                ingredients.put(key.charAt(0), material);
            }
        }

        return new QuarrySettings(config.getBoolean("enabled", true),
                lower(config.getStringList("disabled-worlds")),
                block,
                config.getString("item-model", ""),
                config.getBoolean("frame.enabled", false),
                config.getString("frame.model", ""),
                (float) config.getDouble("frame.scale", 1.002),
                config.getInt("max-per-player", 2),
                config.getBoolean("require-claim", true),
                Math.max(1, config.getInt("performance.blocks-per-tick", 20)),
                Math.max(16, config.getInt("performance.scans-per-tick", 512)),
                materials(config.getStringList("skip"), warn),
                materials(config.getStringList("junk"), warn),
                Collections.unmodifiableMap(tracks),
                Collections.unmodifiableMap(unlocks),
                new Recipe(config.getBoolean("recipe.enabled", false), config.getStringList("recipe.shape"),
                        Map.copyOf(ingredients)));
    }

    private static Cost cost(Object raw, Consumer<String> warn, String where) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Cost.FREE;
        }
        return Cost.from(new MemoryConfiguration().createSection("cost", map),
                text -> warn.accept("quarries.yml (" + where + "): " + text));
    }

    private static Set<Material> materials(List<String> names, Consumer<String> warn) {
        Set<Material> out = EnumSet.noneOf(Material.class);
        for (String name : names) {
            Material material = Material.matchMaterial(name);
            if (material == null) {
                warn.accept("quarries.yml: material desconocido '" + name + "'");
            } else {
                out.add(material);
            }
        }
        return Collections.unmodifiableSet(out);
    }

    private static Set<String> lower(List<String> values) {
        return values.stream().map(v -> v.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }

    public Track track(Numeric numeric) {
        return tracks.get(numeric);
    }

    public boolean allowed(String world) {
        return enabled && !disabledWorlds.contains(world.toLowerCase(Locale.ROOT));
    }
}
