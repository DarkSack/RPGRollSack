package com.sack.rpgroll.items.core;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.items.util.DurationParser;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convierte un YAML de items/ (posiblemente en una subcarpeta de
 * categoría, ej. items/sword/flame_blade.yml) en un {@link ItemDefinition}
 * completo. Solo "id" y "material" son obligatorios — cada componente cae
 * a "deshabilitado" si no aparece en el archivo.
 */
public class ItemParser implements ContentParser<ItemDefinition> {

    @Override
    public ItemDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        Material material = parseMaterial(config.getString("material"));
        if (material == null) {
            throw new IllegalArgumentException("ítem '" + id + "' tiene un 'material' inválido o ausente");
        }

        String pack = config.getString("pack", config.getString("category", "misc"));
        String displayName = config.getString("display-name", id);
        List<String> lore = config.getStringList("lore");
        Integer customModelData = config.contains("custom-model-data") ? config.getInt("custom-model-data") : null;
        String rarityId = config.getString("rarity", "common");
        Boolean glow = config.contains("glow") ? config.getBoolean("glow") : null;
        boolean unbreakable = config.getBoolean("unbreakable", false);

        List<String> flags = parseFlags(config);
        String dyeColor = config.getString("color");
        String skullTexture = config.getString("skull-texture");
        ArmorTrimDef trim = parseTrim(config.getConfigurationSection("trim"));

        Map<String, Double> stats = parseNumberMap(config.getConfigurationSection("stats"));
        Map<String, Double> attributeModifiers = parseNumberMap(config.getConfigurationSection("attributes"));
        ItemRequirements requirements = parseRequirements(config.getConfigurationSection("requirements"));
        ItemDurabilityDef durability = parseDurability(config.getConfigurationSection("durability"));

        Map<String, Integer> vanillaEnchantments = parseIntMap(config.getConfigurationSection("enchantments"));
        Map<String, Integer> customEnchantments = parseIntMap(config.getConfigurationSection("custom-enchantments"));

        List<ItemEffectDef> effects = parseEffects(config.getList("effects"));
        Map<ItemTrigger, List<ItemAction>> triggers = parseTriggers(config.getConfigurationSection("triggers"));
        List<ItemAbility> abilities = parseAbilities(config.getList("abilities"));
        List<SocketDefinition> sockets = parseSockets(config.getList("sockets"));
        List<ItemSkin> skins = parseSkins(config.getList("skins"));
        List<UpgradeLevel> upgrades = parseUpgrades(config.getConfigurationSection("upgrades"));
        List<ItemRecipeDef> recipes = parseRecipes(config.getList("recipes"));

        double sellPrice = config.getDouble("economy.sell", 0);
        double buyPrice = config.getDouble("economy.buy", 0);
        Map<String, String> customData = parseStringMap(config.getConfigurationSection("custom-data"));

        return new ItemDefinition(id, pack, material, displayName, lore, customModelData, rarityId, glow,
                flags, unbreakable, dyeColor, skullTexture, trim, stats, attributeModifiers, requirements,
                durability, vanillaEnchantments, customEnchantments, effects, triggers, abilities, sockets, skins,
                upgrades, recipes, sellPrice, buyPrice, customData);
    }

    // ============ Componentes simples ============

    private List<String> parseFlags(YamlConfiguration config) {

        List<String> flags = new ArrayList<>();

        for (String flag : config.getStringList("flags")) {
            flags.add(flag.trim().toUpperCase(Locale.ROOT));
        }

        // Compatibilidad: "hide-attributes: true" sigue funcionando como
        // atajo para agregar ese flag sin tener que migrar YAML viejos.
        if (config.getBoolean("hide-attributes", false) && !flags.contains("HIDE_ATTRIBUTES")) {
            flags.add("HIDE_ATTRIBUTES");
        }

        return flags;
    }

    private ArmorTrimDef parseTrim(ConfigurationSection section) {

        if (section == null) {
            return null;
        }

        String material = section.getString("material");
        String pattern = section.getString("pattern");

        if (material == null || pattern == null) {
            return null;
        }

        return new ArmorTrimDef(material, pattern);
    }

    private Material parseMaterial(String raw) {

        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Map<String, Double> parseNumberMap(ConfigurationSection section) {

        Map<String, Double> result = new HashMap<>();

        if (section == null) {
            return result;
        }

        for (String key : section.getKeys(false)) {
            if (section.get(key) instanceof Number number) {
                result.put(key.toLowerCase(Locale.ROOT), number.doubleValue());
            }
        }

        return result;
    }

    private Map<String, Integer> parseIntMap(ConfigurationSection section) {

        Map<String, Integer> result = new HashMap<>();

        if (section == null) {
            return result;
        }

        for (String key : section.getKeys(false)) {
            if (section.get(key) instanceof Number number) {
                result.put(key.toUpperCase(Locale.ROOT), number.intValue());
            }
        }

        return result;
    }

    private Map<String, String> parseStringMap(ConfigurationSection section) {

        Map<String, String> result = new HashMap<>();

        if (section == null) {
            return result;
        }

        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value != null) {
                result.put(key, value.toString());
            }
        }

        return result;
    }

    // ============ Requirements / Durability ============

    private ItemRequirements parseRequirements(ConfigurationSection section) {

        if (section == null) {
            return ItemRequirements.none();
        }

        return new ItemRequirements(
                section.getInt("level", 0),
                section.getString("race"),
                section.getString("class"),
                section.getString("profession"),
                section.getString("skill"),
                section.getString("trait"),
                section.getString("permission"),
                section.getDouble("money", 0),
                section.getStringList("completed-quests"));
    }

    private ItemDurabilityDef parseDurability(ConfigurationSection section) {

        if (section == null) {
            return ItemDurabilityDef.disabled();
        }

        return new ItemDurabilityDef(
                section.getInt("max", 0),
                section.getBoolean("repairable", true),
                section.getInt("degrade-per-use", 1),
                section.getInt("auto-repair-per-minute", 0));
    }

    // ============ Effects ============

    private List<ItemEffectDef> parseEffects(List<?> raw) {

        List<ItemEffectDef> effects = new ArrayList<>();

        if (raw == null) {
            return effects;
        }

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map) || map.get("potion") == null) {
                continue;
            }

            String potion = map.get("potion").toString().trim().toUpperCase(Locale.ROOT);
            int amplifier = map.get("amplifier") != null ? parseInt(map.get("amplifier").toString(), 0) : 0;
            boolean requiresHeld = map.get("requires-held") == null
                    || Boolean.parseBoolean(map.get("requires-held").toString());

            effects.add(new ItemEffectDef(potion, amplifier, requiresHeld));
        }

        return effects;
    }

    // ============ Triggers / Actions ============

    private Map<ItemTrigger, List<ItemAction>> parseTriggers(ConfigurationSection section) {

        Map<ItemTrigger, List<ItemAction>> triggers = new EnumMap<>(ItemTrigger.class);

        if (section == null) {
            return triggers;
        }

        for (String key : section.getKeys(false)) {

            ItemTrigger trigger;
            try {
                trigger = ItemTrigger.valueOf(key.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                continue;
            }

            triggers.put(trigger, parseActions(section.getList(key)));
        }

        return triggers;
    }

    private List<ItemAction> parseActions(List<?> raw) {

        List<ItemAction> actions = new ArrayList<>();

        if (raw == null) {
            return actions;
        }

        for (Object entry : raw) {

            if (entry instanceof String simple) {
                actions.add(new ItemAction(simple.trim().toUpperCase(Locale.ROOT), Map.of()));
                continue;
            }

            if (!(entry instanceof Map<?, ?> map) || map.get("type") == null) {
                continue;
            }

            Map<String, String> params = new HashMap<>();

            for (var mapEntry : map.entrySet()) {

                String key = mapEntry.getKey().toString();

                if (key.equals("type") || mapEntry.getValue() == null) {
                    continue;
                }

                params.put(key, mapEntry.getValue().toString());
            }

            actions.add(new ItemAction(map.get("type").toString().trim().toUpperCase(Locale.ROOT), params));
        }

        return actions;
    }

    // ============ Abilities ============

    private List<ItemAbility> parseAbilities(List<?> raw) {

        List<ItemAbility> abilities = new ArrayList<>();

        if (raw == null) {
            return abilities;
        }

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map) || map.get("id") == null) {
                continue;
            }

            String id = map.get("id").toString();
            String displayName = map.get("display-name") != null ? map.get("display-name").toString() : id;
            boolean passive = map.get("passive") != null && Boolean.parseBoolean(map.get("passive").toString());

            ItemTrigger trigger = null;
            if (map.get("trigger") != null) {
                try {
                    trigger = ItemTrigger.valueOf(map.get("trigger").toString().trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                }
            }

            long cooldownMillis = DurationParser.parseMillis(
                    map.get("cooldown") != null ? map.get("cooldown").toString() : "");

            List<String> conditions = map.get("conditions") instanceof List<?> conditionList
                    ? conditionList.stream().map(Object::toString).toList()
                    : List.of();

            List<ItemAction> actions = map.get("actions") instanceof List<?> actionList
                    ? parseActions(actionList)
                    : List.of();

            abilities.add(new ItemAbility(id, displayName, passive, trigger, cooldownMillis, conditions, actions));
        }

        return abilities;
    }

    // ============ Sockets / Skins ============

    private List<SocketDefinition> parseSockets(List<?> raw) {

        List<SocketDefinition> sockets = new ArrayList<>();

        if (raw == null) {
            return sockets;
        }

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map) || map.get("id") == null) {
                continue;
            }

            List<String> acceptedTypes = map.get("accepted-types") instanceof List<?> typeList
                    ? typeList.stream().map(Object::toString).toList()
                    : List.of();

            sockets.add(new SocketDefinition(map.get("id").toString(), acceptedTypes));
        }

        return sockets;
    }

    private List<ItemSkin> parseSkins(List<?> raw) {

        List<ItemSkin> skins = new ArrayList<>();

        if (raw == null) {
            return skins;
        }

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map) || map.get("id") == null) {
                continue;
            }

            String id = map.get("id").toString();
            String displayName = map.get("display-name") != null ? map.get("display-name").toString() : id;
            String material = map.get("material") != null ? map.get("material").toString() : null;
            Integer customModelData = map.get("custom-model-data") != null
                    ? parseInt(map.get("custom-model-data").toString(), 0)
                    : null;

            skins.add(new ItemSkin(id, displayName, material, customModelData));
        }

        return skins;
    }

    // ============ Upgrades ============

    private List<UpgradeLevel> parseUpgrades(ConfigurationSection section) {

        List<UpgradeLevel> upgrades = new ArrayList<>();

        if (section == null) {
            return upgrades;
        }

        for (String levelKey : section.getKeys(false)) {

            int level;
            try {
                level = Integer.parseInt(levelKey.trim());
            } catch (NumberFormatException e) {
                continue;
            }

            ConfigurationSection levelSection = section.getConfigurationSection(levelKey);
            if (levelSection == null) {
                continue;
            }

            Map<String, Double> statBonus = parseNumberMap(levelSection.getConfigurationSection("stats"));
            String displayNameOverride = levelSection.getString("display-name");
            String rarityOverride = levelSection.getString("rarity");
            double cost = levelSection.getDouble("cost", 0);
            String costMaterial = levelSection.getString("cost-material");
            int costAmount = levelSection.getInt("cost-amount", 0);

            upgrades.add(new UpgradeLevel(level, statBonus, displayNameOverride, rarityOverride, cost,
                    costMaterial, costAmount));
        }

        upgrades.sort((a, b) -> Integer.compare(a.level(), b.level()));

        return upgrades;
    }

    // ============ Recipes ============

    private List<ItemRecipeDef> parseRecipes(List<?> raw) {

        List<ItemRecipeDef> recipes = new ArrayList<>();

        if (raw == null) {
            return recipes;
        }

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map) || map.get("type") == null) {
                continue;
            }

            RecipeType type;
            try {
                type = RecipeType.valueOf(map.get("type").toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                continue;
            }

            List<String> shape = map.get("shape") instanceof List<?> shapeList
                    ? shapeList.stream().map(Object::toString).toList()
                    : List.of();

            Map<String, String> key = new HashMap<>();
            if (map.get("key") instanceof Map<?, ?> keyMap) {
                for (var keyEntry : keyMap.entrySet()) {
                    key.put(keyEntry.getKey().toString(), keyEntry.getValue().toString());
                }
            }

            List<String> ingredients = map.get("ingredients") instanceof List<?> ingredientList
                    ? ingredientList.stream().map(Object::toString).toList()
                    : List.of();

            String baseMaterial = map.get("base-material") != null ? map.get("base-material").toString() : null;
            int cookingTime = map.get("cooking-time") != null ? parseInt(map.get("cooking-time").toString(), 200) : 200;
            String sourceId = map.get("source") != null ? map.get("source").toString() : null;

            recipes.add(new ItemRecipeDef(type, shape, key, ingredients, baseMaterial, cookingTime, sourceId));
        }

        return recipes;
    }

    // ============ Helpers ============

    private int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}
