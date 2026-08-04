package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SpellParser implements ContentParser<Spell> {

    @Override
    public Spell parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String school = config.getString("school");
        if (school == null || school.isBlank()) {
            throw new IllegalArgumentException("hechizo '" + id + "' sin campo obligatorio 'school'");
        }

        SpellRarity rarity = parseEnum(SpellRarity.class, config.getString("rarity"), SpellRarity.COMMON);
        SpellCastTrigger trigger = parseEnum(SpellCastTrigger.class, config.getString("trigger"),
                SpellCastTrigger.RIGHT_CLICK);

        SpellCost cost = parseCost(config);
        List<SpellComponent> components = parseComponents(config.getMapList("components"), id);

        return new Spell(
                id,
                config.getString("display-name", id),
                config.getString("icon", "BLAZE_POWDER"),
                config.getString("color", "WHITE"),
                school,
                rarity,
                config.getInt("level", 0),
                cost,
                config.getInt("cast-time", 0),
                config.getInt("cooldown", 0),
                trigger,
                config.getString("tree-parent"),
                config.getInt("tree-tier", 0),
                Set.copyOf(config.getStringList("tags")),
                config.getString("description", ""),
                components);
    }

    private SpellCost parseCost(YamlConfiguration config) {

        var section = config.getConfigurationSection("cost");

        if (section == null) {
            return SpellCost.none();
        }

        return new SpellCost(
                section.getInt("mana", 0),
                section.getInt("health", 0),
                section.getInt("experience", 0),
                section.getString("reagent-material"),
                section.getInt("reagent-amount", 1));
    }

    private List<SpellComponent> parseComponents(List<?> raw, String spellId) {

        List<SpellComponent> components = new ArrayList<>();

        for (Object rawComponent : raw) {

            if (!(rawComponent instanceof Map<?, ?> map)) {
                continue;
            }

            Object rawType = map.get("type");
            if (rawType == null) {
                continue;
            }

            SpellComponentType type;

            try {
                type = SpellComponentType.valueOf(rawType.toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "hechizo '" + spellId + "' tiene un componente con type inválido: " + rawType);
            }

            Map<String, String> params = new LinkedHashMap<>();

            for (var entry : map.entrySet()) {

                String key = String.valueOf(entry.getKey());

                if (key.equals("type") || entry.getValue() == null) {
                    continue;
                }

                params.put(key, String.valueOf(entry.getValue()));
            }

            components.add(new SpellComponent(type, params));
        }

        return components;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String raw, E fallback) {

        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

}
