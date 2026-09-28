package com.sack.rpgroll.recipes;

import com.sack.rpgroll.common.recipe.RecipeEntry;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** config.yml ya leído. */
public record Settings(long refreshMillis, boolean brewing, Set<String> hiddenSources, Set<String> hiddenStations,
        List<Pattern> hiddenRecipes, Book book) {

    public record Book(Material material, NamespacedKey model, boolean glint, boolean giveOnFirstJoin, boolean recipe) {
    }

    public static Settings from(FileConfiguration config) {

        Material material = Material.matchMaterial(config.getString("book.material", "BOOK"));
        String model = config.getString("book.model", "");

        return new Settings(
                Math.max(0, config.getLong("index.refresh-minutes", 5)) * 60_000L,
                config.getBoolean("brewing", true),
                lower(config.getStringList("hide.sources")),
                lower(config.getStringList("hide.stations")),
                config.getStringList("hide.recipes").stream().map(Settings::glob).toList(),
                new Book(material == null || !material.isItem() ? Material.BOOK : material,
                        model == null || model.isBlank() ? null : NamespacedKey.fromString(model.toLowerCase(Locale.ROOT)),
                        config.getBoolean("book.glint", true),
                        config.getBoolean("book.give-on-first-join", false),
                        config.getBoolean("book.recipe", true)));
    }

    public boolean hidden(RecipeEntry entry) {
        if (hiddenSources.contains(entry.source().toLowerCase(Locale.ROOT))
                || hiddenStations.contains(entry.station().id().toLowerCase(Locale.ROOT))) {
            return true;
        }
        String id = entry.id().toLowerCase(Locale.ROOT);
        return hiddenRecipes.stream().anyMatch(pattern -> pattern.matcher(id).matches());
    }

    /** {@code minecraft:*_bed} → regex; solo {@code *} es comodín. */
    static Pattern glob(String text) {
        String[] parts = text.toLowerCase(Locale.ROOT).split("\\*", -1);
        return Pattern.compile(java.util.Arrays.stream(parts).map(Pattern::quote).collect(Collectors.joining(".*")));
    }

    private static Set<String> lower(List<String> values) {
        return values.stream().map(value -> value.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }
}
