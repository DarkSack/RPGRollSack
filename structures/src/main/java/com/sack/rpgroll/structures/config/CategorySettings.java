package com.sack.rpgroll.structures.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/** Ajustes de {@code categories.yml}. El nombre visible sale del lang: {@code category.<id>}. */
public record CategorySettings(List<Category> categories) {

    /** {@code icon} es un id de material vanilla; {@code order} ordena el menú (menor primero). */
    public record Category(String id, String icon, int order) {}

    private static final String FALLBACK_ICON = "minecraft:stone";
    private static final List<Category> DEFAULTS = List.of(
            new Category("casas", "minecraft:oak_door", 1),
            new Category("granjas_mobs", "minecraft:spawner", 2),
            new Category("granjas_auto", "minecraft:wheat", 3),
            new Category("parcelas", "minecraft:oak_fence", 4),
            new Category("decoracion", "minecraft:flower_pot", 5),
            new Category("infraestructura", "minecraft:rail", 6),
            new Category("redstone", "minecraft:redstone", 7),
            new Category("especiales", "minecraft:nether_star", 8));

    public static CategorySettings from(ConfigurationSection root, Consumer<String> warn) {
        ConfigurationSection s = root.getConfigurationSection("categories");
        if (s == null || s.getKeys(false).isEmpty()) {
            return new CategorySettings(DEFAULTS);
        }
        Map<String, Category> out = new LinkedHashMap<>();
        int next = 1;
        for (String key : s.getKeys(false)) {
            String id = key.toLowerCase(Locale.ROOT);
            ConfigurationSection c = s.getConfigurationSection(key);
            Category cat = new Category(id, c == null ? FALLBACK_ICON : c.getString("icon", FALLBACK_ICON),
                    c == null ? next : c.getInt("order", next));
            next++;
            if (out.putIfAbsent(id, cat) != null) {
                warn.accept("categories." + key + ": id repetido '" + id + "'; me quedo con la primera.");
            }
        }
        List<Category> list = new ArrayList<>(out.values());
        list.sort(Comparator.comparingInt(Category::order)); // estable: a igual orden, el del archivo
        return new CategorySettings(List.copyOf(list));
    }

    public Optional<Category> byId(String id) {
        return categories.stream().filter(c -> c.id().equalsIgnoreCase(id)).findFirst();
    }
}
