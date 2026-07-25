package com.sack.rpgroll.gameplay.enchant;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.content.ContentParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EnchantParser implements ContentParser<CustomEnchantment> {

    private final RPGRoll plugin;

    public EnchantParser(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public CustomEnchantment parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String displayName = config.getString("display-name", id);
        String description = config.getString("description", "");
        List<String> lore = config.getStringList("lore");
        int maxLevel = config.getInt("max-level", 1);

        Set<ItemCategory> applicableTo = parseApplicableTo(config, id);
        EnchantTrigger trigger = parseTrigger(config, id);
        String effectType = parseEffect(config, id);
        Map<String, Object> params = parseParams(config);

        double dropChance = config.getDouble("drop-chance", 0.0);

        Set<String> dropMobs = config.getStringList("drop-mobs").stream()
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

        double shopPriceBase = config.getDouble("shop-price-base", 0.0);
        double shopPricePerLevel = config.getDouble("shop-price-per-level", 0.0);

        return new CustomEnchantment(id, displayName, description, lore, maxLevel, applicableTo, trigger, effectType,
                params, dropChance, dropMobs, shopPriceBase, shopPricePerLevel);
    }

    private Set<ItemCategory> parseApplicableTo(YamlConfiguration config, String id) {

        List<String> raw = config.getStringList("applicable-to");
        Set<ItemCategory> categories = new LinkedHashSet<>();

        for (String value : raw) {
            try {
                categories.add(ItemCategory.valueOf(value.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger()
                        .warning("✘ Encantamiento '" + id + "': categoría inválida '" + value + "', ignorada.");
            }
        }

        if (categories.isEmpty()) {
            throw new IllegalArgumentException("'" + id + "' no define 'applicable-to' válido");
        }

        return categories;
    }

    private EnchantTrigger parseTrigger(YamlConfiguration config, String id) {

        String raw = config.getString("trigger");

        if (raw == null) {
            throw new IllegalArgumentException("'" + id + "' sin campo obligatorio 'trigger'");
        }

        try {
            return EnchantTrigger.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("'" + id + "' tiene un trigger inválido: '" + raw + "'");
        }
    }

    private String parseEffect(YamlConfiguration config, String id) {

        String effect = config.getString("effect");

        if (effect == null || effect.isBlank()) {
            throw new IllegalArgumentException("'" + id + "' sin campo obligatorio 'effect'");
        }

        return effect.toUpperCase();
    }

    private Map<String, Object> parseParams(YamlConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("params");
        return section != null ? section.getValues(false) : Map.of();
    }

}