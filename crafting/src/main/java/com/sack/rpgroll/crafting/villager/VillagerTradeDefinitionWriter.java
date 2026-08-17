package com.sack.rpgroll.crafting.villager;

import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.recipe.RecipeResult;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class VillagerTradeDefinitionWriter {

    private final File folder;

    public VillagerTradeDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "villager-trades");
    }

    public void save(VillagerTradeDefinition trade) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", trade.id());
        config.set("display-name", trade.displayName());
        config.set("icon", trade.icon());
        config.set("max-uses", trade.maxUses());
        config.set("villager-experience", trade.villagerExperience());
        config.set("rewards-experience", trade.rewardsExperience());
        config.set("xp-amount", trade.xpAmount());
        config.set("economy-currency-id", trade.economyCurrencyId());
        config.set("economy-cost", trade.economyCost());
        config.set("quality-enabled", trade.qualityEnabled());

        config.set("costs", trade.costs().stream().map(this::serializeResult).toList());

        config.set("result.type", trade.result().type().name());
        config.set("result.value", trade.result().value());
        config.set("result.amount", trade.result().amount());

        config.set("conditions", trade.conditions().stream().map(this::serializeCondition).toList());

        try {
            folder.mkdirs();
            config.save(new File(folder, trade.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el comercio " + trade.id(), e);
        }
    }

    public void delete(String id) {
        new File(folder, id + ".yml").delete();
    }

    private Map<String, Object> serializeResult(RecipeResult result) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", result.type().name());
        map.put("value", result.value());
        map.put("amount", result.amount());
        return map;
    }

    private Map<String, Object> serializeCondition(RecipeCondition condition) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", condition.type().name());
        if (condition.value() != null) {
            map.put("value", condition.value());
        }
        map.put("min-value", condition.minValue());
        return map;
    }

}
