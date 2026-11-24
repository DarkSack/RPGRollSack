package com.sack.rpgroll.structures.config;

import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Ajustes de {@code workbench.yml}: receta y niveles. */
public record WorkbenchSettings(Map<WorkbenchTier, TierSettings> tiers, List<String> recipeShape,
                                Map<Character, String> recipeKeys) {

    public record Size(int width, int height, int length) {
        /** true si {@code other} cabe en este tamaño, eje por eje. */
        public boolean fits(Size other) {
            return other.width <= width && other.height <= height && other.length <= length;
        }
    }

    public record Cost(String itemId, int amount) {}

    public record TierSettings(Size maxSize, List<Cost> upgradeCost, double upgradeMoney, String model,
                               Material fallbackBlock) {
        /** Único sitio de la regla: el bloque que se coloca y que lleva el ítem; si el material no es un bloque, uno seguro. */
        public Material placeableBlock() {
            return resolve(fallbackBlock, fallbackBlock.isBlock());
        }

        /** La regla sin tocar el registro de Paper (isBlock lo necesita), para poder probarla. */
        public static Material resolve(Material fallback, boolean isBlock) {
            return isBlock ? fallback : Material.CRAFTING_TABLE;
        }
    }

    private static final Map<WorkbenchTier, Size> DEFAULT_SIZE = Map.of(
            WorkbenchTier.CONSTRUCTOR, new Size(16, 16, 16),
            WorkbenchTier.INGENIERO, new Size(32, 32, 32),
            WorkbenchTier.ARQUITECTO, new Size(64, 96, 64));
    private static final Map<WorkbenchTier, Material> DEFAULT_BLOCK = Map.of(
            WorkbenchTier.CONSTRUCTOR, Material.CRAFTING_TABLE,
            WorkbenchTier.INGENIERO, Material.SMITHING_TABLE,
            WorkbenchTier.ARQUITECTO, Material.BEACON);
    private static final List<String> DEFAULT_SHAPE = List.of("PPP", "CTC", "L L");
    private static final Map<Character, String> DEFAULT_KEYS = Map.of(
            'P', "minecraft:oak_planks", 'C', "minecraft:copper_ingot",
            'T', "minecraft:crafting_table", 'L', "minecraft:oak_log");

    public static WorkbenchSettings from(ConfigurationSection root, Consumer<String> warn) {
        Map<WorkbenchTier, TierSettings> tiers = new EnumMap<>(WorkbenchTier.class);
        for (WorkbenchTier tier : WorkbenchTier.values()) {
            String path = "tiers." + tier.name().toLowerCase(Locale.ROOT);
            ConfigurationSection s = root.getConfigurationSection(path);
            tiers.put(tier, new TierSettings(
                    size(s == null ? null : s.getConfigurationSection("max-size"), DEFAULT_SIZE.get(tier),
                            path + ".max-size", warn),
                    costs(s == null ? null : s.getMapList("upgrade-cost"), path + ".upgrade-cost", warn),
                    s == null ? 0 : Math.max(0, s.getDouble("upgrade-money", 0)),
                    s == null ? "" : s.getString("model", ""),
                    material(s == null ? null : s.getString("fallback-block"), DEFAULT_BLOCK.get(tier),
                            path + ".fallback-block", warn)));
        }
        List<String> shape = root.getStringList("recipe.shape");
        if (shape.isEmpty()) {
            shape = DEFAULT_SHAPE;
        }
        Map<Character, String> keys = new LinkedHashMap<>();
        ConfigurationSection k = root.getConfigurationSection("recipe.keys");
        if (k != null) {
            for (String key : k.getKeys(false)) {
                if (key.length() == 1) {
                    keys.put(key.charAt(0), k.getString(key, ""));
                } else {
                    warn.accept("recipe.keys." + key + ": la clave debe ser un solo carácter; se ignora.");
                }
            }
        }
        if (keys.isEmpty()) {
            keys.putAll(DEFAULT_KEYS);
        }
        return new WorkbenchSettings(tiers, List.copyOf(shape), Map.copyOf(keys));
    }

    private static Size size(ConfigurationSection s, Size def, String path, Consumer<String> warn) {
        if (s == null) {
            return def;
        }
        return new Size(axis(s, "width", def.width, path, warn), axis(s, "height", def.height, path, warn),
                axis(s, "length", def.length, path, warn));
    }

    private static int axis(ConfigurationSection s, String key, int def, String path, Consumer<String> warn) {
        if (!s.isSet(key)) {
            return def;
        }
        int v = s.getInt(key, -1);
        if (v < 1) {
            warn.accept(path + ": " + key + " inválido (" + s.get(key) + "); uso " + def + ".");
            return def;
        }
        return v;
    }

    private static Material material(String name, Material def, String path, Consumer<String> warn) {
        if (name == null || name.isBlank()) {
            return def;
        }
        Material m = Material.matchMaterial(name.trim());
        if (m == null) {
            warn.accept(path + ": '" + name + "' no es un material; uso " + def + ".");
            return def;
        }
        return m;
    }

    private static List<Cost> costs(List<Map<?, ?>> raw, String path, Consumer<String> warn) {
        List<Cost> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (Map<?, ?> m : raw) {
            Object item = m.get("item");
            Object amount = m.get("amount");
            if (item == null || !(amount instanceof Number n) || n.intValue() < 1) {
                warn.accept(path + ": entrada inválida " + m + "; se ignora.");
                continue;
            }
            out.add(new Cost(item.toString(), n.intValue()));
        }
        return List.copyOf(out);
    }
}
