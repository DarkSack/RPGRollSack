package com.sack.rpgroll.items.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializa una {@link ItemDefinition} completa de vuelta a YAML — inverso
 * de {@link ItemParser}. Necesario para que el editor gráfico pueda
 * guardar cambios sin perder los componentes que no tocó (sockets,
 * habilidades, recetas, etc.), ya que el archivo se reescribe entero.
 */
public class ItemDefinitionWriter {

    public void save(ItemDefinition definition, File file) throws IOException {

        YamlConfiguration config = new YamlConfiguration();

        config.set("id", definition.id());
        config.set("pack", definition.pack());
        config.set("material", definition.material().name());
        config.set("display-name", definition.displayName());
        config.set("lore", definition.lore());

        if (definition.customModelData() != null) {
            config.set("custom-model-data", definition.customModelData());
        }

        config.set("rarity", definition.rarityId());

        if (definition.glowOverride() != null) {
            config.set("glow", definition.glowOverride());
        }

        if (!definition.flags().isEmpty()) {
            config.set("flags", definition.flags());
        }

        config.set("unbreakable", definition.unbreakable());

        if (definition.dyeColor() != null) {
            config.set("color", definition.dyeColor());
        }
        if (definition.skullTexture() != null) {
            config.set("skull-texture", definition.skullTexture());
        }
        if (definition.trim() != null) {
            config.set("trim.material", definition.trim().material());
            config.set("trim.pattern", definition.trim().pattern());
        }

        writeMap(config, "stats", definition.stats());
        writeMap(config, "attributes", definition.attributeModifiers());
        writeRequirements(config, definition.requirements());
        writeDurability(config, definition.durability());
        writeIntMap(config, "enchantments", definition.vanillaEnchantments());
        writeIntMap(config, "custom-enchantments", definition.customEnchantments());
        writeEffects(config, definition.effects());
        writeTriggers(config, definition.triggers());
        writeAbilities(config, definition.abilities());
        writeSockets(config, definition.sockets());
        writeSkins(config, definition.skins());
        writeUpgrades(config, definition.upgrades());
        writeRecipes(config, definition.recipes());

        if (definition.sellPrice() > 0) {
            config.set("economy.sell", definition.sellPrice());
        }
        if (definition.buyPrice() > 0) {
            config.set("economy.buy", definition.buyPrice());
        }

        writeMap(config, "custom-data", definition.customData());

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        config.save(file);
    }

    private void writeMap(YamlConfiguration config, String path, Map<String, ?> map) {
        for (var entry : map.entrySet()) {
            config.set(path + "." + entry.getKey(), entry.getValue());
        }
    }

    private void writeIntMap(YamlConfiguration config, String path, Map<String, Integer> map) {
        for (var entry : map.entrySet()) {
            config.set(path + "." + entry.getKey(), entry.getValue());
        }
    }

    private void writeRequirements(YamlConfiguration config, ItemRequirements req) {

        if (req.level() > 0) {
            config.set("requirements.level", req.level());
        }
        if (req.race() != null) {
            config.set("requirements.race", req.race());
        }
        if (req.playerClass() != null) {
            config.set("requirements.class", req.playerClass());
        }
        if (req.profession() != null) {
            config.set("requirements.profession", req.profession());
        }
        if (req.skill() != null) {
            config.set("requirements.skill", req.skill());
        }
        if (req.trait() != null) {
            config.set("requirements.trait", req.trait());
        }
        if (req.permission() != null) {
            config.set("requirements.permission", req.permission());
        }
        if (req.money() > 0) {
            config.set("requirements.money", req.money());
        }
        if (!req.completedQuests().isEmpty()) {
            config.set("requirements.completed-quests", req.completedQuests());
        }
    }

    private void writeDurability(YamlConfiguration config, ItemDurabilityDef durability) {

        if (!durability.enabled()) {
            return;
        }

        config.set("durability.max", durability.maxDurability());
        config.set("durability.repairable", durability.repairable());
        config.set("durability.degrade-per-use", durability.degradePerUse());
        config.set("durability.auto-repair-per-minute", durability.autoRepairPerMinute());
    }

    private void writeEffects(YamlConfiguration config, List<ItemEffectDef> effects) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (ItemEffectDef effect : effects) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("potion", effect.potion());
            entry.put("amplifier", effect.amplifier());
            entry.put("requires-held", effect.requiresHeld());
            raw.add(entry);
        }

        if (!raw.isEmpty()) {
            config.set("effects", raw);
        }
    }

    private void writeTriggers(YamlConfiguration config, Map<ItemTrigger, List<ItemAction>> triggers) {

        for (var entry : triggers.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                config.set("triggers." + entry.getKey().name(), actionsToRaw(entry.getValue()));
            }
        }
    }

    private void writeAbilities(YamlConfiguration config, List<ItemAbility> abilities) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (ItemAbility ability : abilities) {

            Map<String, Object> entry = new HashMap<>();
            entry.put("id", ability.id());
            entry.put("display-name", ability.displayName());
            entry.put("passive", ability.passive());

            if (ability.trigger() != null) {
                entry.put("trigger", ability.trigger().name());
            }

            entry.put("cooldown", (ability.cooldownMillis() / 1000) + "s");
            entry.put("conditions", ability.conditions());
            entry.put("actions", actionsToRaw(ability.actions()));

            raw.add(entry);
        }

        if (!raw.isEmpty()) {
            config.set("abilities", raw);
        }
    }

    private void writeSockets(YamlConfiguration config, List<SocketDefinition> sockets) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (SocketDefinition socket : sockets) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", socket.id());
            entry.put("accepted-types", socket.acceptedTypes());
            raw.add(entry);
        }

        if (!raw.isEmpty()) {
            config.set("sockets", raw);
        }
    }

    private void writeSkins(YamlConfiguration config, List<ItemSkin> skins) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (ItemSkin skin : skins) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", skin.id());
            entry.put("display-name", skin.displayName());
            if (skin.material() != null) {
                entry.put("material", skin.material());
            }
            if (skin.customModelData() != null) {
                entry.put("custom-model-data", skin.customModelData());
            }
            raw.add(entry);
        }

        if (!raw.isEmpty()) {
            config.set("skins", raw);
        }
    }

    private void writeUpgrades(YamlConfiguration config, List<UpgradeLevel> upgrades) {

        for (UpgradeLevel upgrade : upgrades) {

            String path = "upgrades." + upgrade.level();

            for (var entry : upgrade.statBonus().entrySet()) {
                config.set(path + ".stats." + entry.getKey(), entry.getValue());
            }

            if (upgrade.displayNameOverride() != null) {
                config.set(path + ".display-name", upgrade.displayNameOverride());
            }
            if (upgrade.rarityOverride() != null) {
                config.set(path + ".rarity", upgrade.rarityOverride());
            }
            if (upgrade.cost() > 0) {
                config.set(path + ".cost", upgrade.cost());
            }
            if (upgrade.costMaterial() != null) {
                config.set(path + ".cost-material", upgrade.costMaterial());
                config.set(path + ".cost-amount", upgrade.costAmount());
            }
        }
    }

    private void writeRecipes(YamlConfiguration config, List<ItemRecipeDef> recipes) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (ItemRecipeDef recipe : recipes) {

            Map<String, Object> entry = new HashMap<>();
            entry.put("type", recipe.type().name());

            if (!recipe.shape().isEmpty()) {
                entry.put("shape", recipe.shape());
            }
            if (!recipe.key().isEmpty()) {
                entry.put("key", recipe.key());
            }
            if (!recipe.ingredients().isEmpty()) {
                entry.put("ingredients", recipe.ingredients());
            }
            if (recipe.baseMaterial() != null) {
                entry.put("base-material", recipe.baseMaterial());
            }
            entry.put("cooking-time", recipe.cookingTimeTicks());

            if (recipe.sourceId() != null) {
                entry.put("source", recipe.sourceId());
            }

            raw.add(entry);
        }

        if (!raw.isEmpty()) {
            config.set("recipes", raw);
        }
    }

    private List<Map<String, Object>> actionsToRaw(List<ItemAction> actions) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (ItemAction action : actions) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("type", action.type());
            entry.putAll(action.params());
            raw.add(entry);
        }

        return raw;
    }

}
