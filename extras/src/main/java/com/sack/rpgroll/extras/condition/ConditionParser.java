package com.sack.rpgroll.extras.condition;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.extras.action.ActionParsingUtil;
import com.sack.rpgroll.extras.action.ExtrasAction;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.Map;

public class ConditionParser implements ContentParser<ConditionDefinition> {

    @Override
    public ConditionDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        int duration = config.getInt("duration", -1);
        double damage = config.getDouble("damage", 0);
        int interval = config.getInt("interval", 20);
        List<String> potionEffects = config.getStringList("effects");

        List<ExtrasAction> onApply = ActionParsingUtil.parseActions(mapList(config, "on-apply"));
        List<ExtrasAction> onTick = ActionParsingUtil.parseActions(mapList(config, "on-tick"));
        List<ExtrasAction> onExpire = ActionParsingUtil.parseActions(mapList(config, "on-expire"));

        return new ConditionDefinition(id, duration, damage, interval, potionEffects, onApply, onTick, onExpire);
    }

    private List<Map<?, ?>> mapList(YamlConfiguration config, String path) {
        return config.getMapList(path);
    }

}
