package com.sack.rpgroll.enchantments.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Serializa un {@link CustomEnchantment} completo de vuelta a YAML — inverso de {@link EnchantmentParser}. */
public class EnchantmentDefinitionWriter {

    private final File folder;
    private final Logger logger;

    public EnchantmentDefinitionWriter(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public void save(CustomEnchantment enchantment) {

        YamlConfiguration config = new YamlConfiguration();

        config.set("id", enchantment.id());
        config.set("display-name", enchantment.displayName());
        config.set("rarity", enchantment.rarity().name());
        config.set("max-level", enchantment.maxLevel());
        config.set("chance", enchantment.chance());

        List<String> categories = new ArrayList<>();
        enchantment.categories().forEach(c -> categories.add(c.name()));
        config.set("categories", categories);

        List<String> allowedItems = new ArrayList<>();
        enchantment.allowedItems().forEach(m -> allowedItems.add(m.name()));
        config.set("allowed-items", allowedItems);

        config.set("conflicts", enchantment.conflicts());

        List<String> triggers = new ArrayList<>();
        enchantment.triggers().forEach(t -> triggers.add(t.name()));
        config.set("trigger", triggers);

        config.set("conditions", enchantment.conditions());

        for (var levelEntry : enchantment.levelData().entrySet()) {
            for (var dataEntry : levelEntry.getValue().entrySet()) {
                config.set("levels." + levelEntry.getKey() + "." + dataEntry.getKey(), dataEntry.getValue());
            }
        }

        List<Map<String, Object>> effects = new ArrayList<>();
        for (EnchantEffect effect : enchantment.effects()) {
            Map<String, Object> map = new LinkedHashMap<>(effect.params());
            map.put("type", effect.type().name());
            effects.add(map);
        }
        config.set("effects", effects);

        try {
            if (!folder.exists()) {
                folder.mkdirs();
            }
            config.save(new File(folder, enchantment.id() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ Error guardando encantamiento '" + enchantment.id() + "': " + e.getMessage());
        }
    }

}
