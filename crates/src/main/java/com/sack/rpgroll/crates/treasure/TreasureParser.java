package com.sack.rpgroll.crates.treasure;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.crates.lucky.LuckyAction;
import com.sack.rpgroll.crates.lucky.LuckyParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Lee treasures/*.yml. Ver resources/treasures/_referencia.yml. */
public class TreasureParser implements ContentParser<Treasure> {

    @Override
    public Treasure parse(YamlConfiguration config) {

        String rawId = config.getString("id");
        if (rawId == null || rawId.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }
        String id = rawId.trim().toLowerCase(Locale.ROOT);
        String where = "tesoro '" + id + "'";

        try {
            return new Treasure(id, config.getString("display-name", id), map(config), placement(config),
                    chest(config, where), loot(config), actions(config.getMapList("actions"), where));
        } catch (IllegalArgumentException e) {
            throw e.getMessage() != null && e.getMessage().startsWith(where)
                    ? e : new IllegalArgumentException(where + ": " + e.getMessage(), e);
        }
    }

    private static Treasure.MapItem map(YamlConfiguration config) {
        return new Treasure.MapItem(config.getString("map.material", "PAPER"), blankToNull(config.getString("map.model")),
                config.getString("map.name"), config.getStringList("map.lore"));
    }

    private static Treasure.Placement placement(YamlConfiguration config) {

        Set<String> biomes = new LinkedHashSet<>();
        for (String biome : config.getStringList("placement.biomes")) {
            String key = biome.trim().toLowerCase(Locale.ROOT);
            biomes.add(key.contains(":") ? key : "minecraft:" + key);
        }

        return new Treasure.Placement(config.getStringList("placement.worlds"), biomes,
                config.getInt("placement.min-distance", 120), config.getInt("placement.max-distance", 500),
                config.getInt("placement.depth", 3));
    }

    private static Treasure.Chest chest(YamlConfiguration config, String where) {
        return new Treasure.Chest(blankToNull(config.getString("chest.model")), config.getDouble("chest.scale", 1.0),
                config.getInt("chest.rows", 3), config.getString("chest.title"),
                actions(config.getMapList("chest.guardians"), where + ", guardianes"));
    }

    private static Treasure.Loot loot(YamlConfiguration config) {

        int[] rolls = range(config.getString("loot.rolls", "3-6"), "loot.rolls");

        List<Treasure.Entry> entries = new ArrayList<>();
        for (Map<?, ?> entry : config.getMapList("loot.entries")) {
            Object item = entry.get("item");
            if (item == null || item.toString().isBlank()) {
                throw new IllegalArgumentException("una entrada de loot no tiene item");
            }
            double weight = Double.parseDouble(String.valueOf(entry.containsKey("weight") ? entry.get("weight") : 1));
            if (weight <= 0) {
                continue;
            }
            int[] amount = range(String.valueOf(entry.containsKey("amount") ? entry.get("amount") : 1),
                    "amount de '" + item + "'");
            entries.add(new Treasure.Entry(item.toString().trim(), amount[0], amount[1], weight));
        }

        return new Treasure.Loot(rolls[0], rolls[1], entries);
    }

    private static List<LuckyAction> actions(List<Map<?, ?>> raw, String where) {
        List<LuckyAction> actions = new ArrayList<>();
        for (Map<?, ?> map : raw) {
            actions.add(LuckyParser.parseAction(map, where));
        }
        return actions;
    }

    /** "3" o "2-5". */
    static int[] range(String raw, String field) {
        String text = raw.trim();
        try {
            int dash = text.indexOf('-', 1);
            if (dash < 0) {
                int value = Integer.parseInt(text);
                return new int[]{value, value};
            }
            return new int[]{Integer.parseInt(text.substring(0, dash).trim()),
                    Integer.parseInt(text.substring(dash + 1).trim())};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " no es un número ni un rango: '" + raw + "'");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
