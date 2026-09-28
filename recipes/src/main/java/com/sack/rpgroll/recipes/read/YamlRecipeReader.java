package com.sack.rpgroll.recipes.read;

import com.sack.rpgroll.common.recipe.RecipeEntry;
import com.sack.rpgroll.common.recipe.RecipeSlot;
import com.sack.rpgroll.common.recipe.RecipeStation;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Recetas escritas a mano en {@code plugins/RPGRoll-Recipes/extra/*.yml}: para los plugins que
 * craftean en su propio menú, con un NPC o con un comando y no registran nada que se pueda leer.
 * Los archivos que empiezan por {@code _} son de referencia y no se cargan.
 */
public final class YamlRecipeReader {

    private final File folder;
    private final Logger logger;

    public YamlRecipeReader(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public List<RecipeEntry> read() {

        List<RecipeEntry> entries = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml") && !name.startsWith("_"));
        if (files == null) {
            return entries;
        }
        Arrays.sort(files);

        for (File file : files) {
            String fileName = file.getName().substring(0, file.getName().length() - 4);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String source = yaml.getString("source", fileName);

            Map<String, RecipeStation> stations = new HashMap<>();
            ConfigurationSection stationSection = yaml.getConfigurationSection("stations");
            if (stationSection != null) {
                for (String id : stationSection.getKeys(false)) {
                    ConfigurationSection s = stationSection.getConfigurationSection(id);
                    if (s == null) {
                        continue;
                    }
                    Material icon = material(s.getString("icon"), Material.CRAFTING_TABLE);
                    stations.put(id.toLowerCase(Locale.ROOT), new RecipeStation("extra:" + id.toLowerCase(Locale.ROOT),
                            s.getString("name", id), ItemStack.of(icon)));
                }
            }

            ConfigurationSection recipes = yaml.getConfigurationSection("recipes");
            if (recipes == null) {
                continue;
            }
            for (String id : recipes.getKeys(false)) {
                ConfigurationSection r = recipes.getConfigurationSection(id);
                try {
                    RecipeEntry entry = r == null ? null : recipe(fileName + "/" + id, source, r, stations);
                    if (entry != null) {
                        entries.add(entry);
                    }
                } catch (RuntimeException e) {
                    logger.warning("✘ extra/" + file.getName() + " → receta '" + id + "': " + e.getMessage());
                }
            }
        }

        return entries;
    }

    private RecipeEntry recipe(String id, String source, ConfigurationSection r, Map<String, RecipeStation> stations) {

        String stationId = r.getString("station", RecipeStation.CRAFTING).toLowerCase(Locale.ROOT);
        RecipeStation station = stations.getOrDefault(stationId, Stations.vanilla(stationId));

        List<ItemStack> results = new ArrayList<>();
        if (r.isList("result")) {
            for (Object raw : r.getList("result", List.of())) {
                results.add(item(raw));
            }
        } else {
            results.add(item(r.isConfigurationSection("result") ? r.getConfigurationSection("result") : r.get("result")));
        }
        if (results.stream().allMatch(stack -> stack == null)) {
            throw new IllegalArgumentException("falta 'result' o no es un material válido");
        }

        RecipeEntry.Builder builder = RecipeEntry.builder(id, station).source(source);
        results.forEach(builder::output);

        List<String> shape = r.getStringList("shape");
        if (!shape.isEmpty()) {
            ConfigurationSection key = r.getConfigurationSection("ingredients");
            int width = shape.stream().mapToInt(String::length).max().orElse(1);
            width = Math.max(1, Math.min(3, width));
            List<RecipeSlot> slots = new ArrayList<>();
            for (int row = 0; row < Math.min(3, shape.size()); row++) {
                for (int col = 0; col < width; col++) {
                    String line = shape.get(row);
                    char c = col < line.length() ? line.charAt(col) : ' ';
                    Object spec = c == ' ' || key == null ? null : key.get(String.valueOf(c));
                    slots.add(spec == null ? RecipeSlot.EMPTY : slot(spec));
                }
            }
            builder.shaped(width, slots);
        } else {
            for (Object spec : r.getList("ingredients", List.of())) {
                builder.input(slot(spec));
            }
        }

        for (String note : r.getStringList("notes")) {
            builder.note(note);
        }

        return builder.build();
    }

    /** {@code "IRON_INGOT x3"}, {@code "#minecraft:planks"} o varias opciones con {@code |}. */
    private static RecipeSlot slot(Object spec) {

        if (spec instanceof ConfigurationSection || spec instanceof Map<?, ?>) {
            ItemStack stack = item(spec);
            return stack == null ? RecipeSlot.EMPTY : RecipeSlot.of(stack);
        }

        List<ItemStack> options = new ArrayList<>();
        for (String part : String.valueOf(spec).split("\\|")) {
            String text = part.trim();
            int amount = 1;
            int x = text.toLowerCase(Locale.ROOT).lastIndexOf(" x");
            if (x > 0) {
                amount = parseAmount(text.substring(x + 2));
                text = text.substring(0, x).trim();
            }
            if (text.startsWith("#")) {
                NamespacedKey tagKey = NamespacedKey.fromString(text.substring(1).toLowerCase(Locale.ROOT));
                Tag<Material> tag = tagKey == null ? null : Bukkit.getTag(Tag.REGISTRY_ITEMS, tagKey, Material.class);
                if (tag != null) {
                    for (Material material : tag.getValues()) {
                        options.add(ItemStack.of(material, amount));
                    }
                }
            } else {
                Material material = material(text, null);
                if (material != null) {
                    options.add(ItemStack.of(material, amount));
                }
            }
        }
        return RecipeSlot.of(options);
    }

    /** Un material suelto o {@code {material, amount, name, lore, model}}. */
    private static ItemStack item(Object raw) {

        if (raw == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();
        if (raw instanceof ConfigurationSection section) {
            map.putAll(section.getValues(false));
        } else if (raw instanceof Map<?, ?> m) {
            m.forEach((k, v) -> map.put(String.valueOf(k), v));
        } else {
            RecipeSlot slot = slot(raw);
            return slot.isEmpty() ? null : slot.options().getFirst();
        }

        Material material = material(String.valueOf(map.get("material")), null);
        if (material == null) {
            return null;
        }
        ItemStack stack = ItemStack.of(material, parseAmount(String.valueOf(map.getOrDefault("amount", 1))));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (map.get("name") != null) {
                meta.itemName(ComponentUtils.parse(String.valueOf(map.get("name"))));
            }
            if (map.get("lore") instanceof List<?> lore) {
                meta.lore(lore.stream().map(line -> ComponentUtils.parse(String.valueOf(line))).toList());
            }
            if (map.get("model") != null) {
                meta.setItemModel(NamespacedKey.fromString(String.valueOf(map.get("model")).toLowerCase(Locale.ROOT)));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static Material material(String raw, Material fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(raw.trim());
        return material != null && material.isItem() && !material.isAir() ? material : fallback;
    }

    private static int parseAmount(String raw) {
        try {
            return Math.max(1, Math.min(99, Integer.parseInt(raw.trim())));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
