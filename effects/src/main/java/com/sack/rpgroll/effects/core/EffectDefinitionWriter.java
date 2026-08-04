package com.sack.rpgroll.effects.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EffectDefinitionWriter {

    private final File folder;

    public EffectDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "effects");
    }

    public void save(EffectDefinition effect) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", effect.id());
        config.set("display-name", effect.displayName());
        config.set("icon", effect.icon());
        config.set("color", effect.color());
        config.set("category", effect.category().name());
        config.set("rarity", effect.rarity().name());
        config.set("description", effect.description());
        config.set("duration", effect.durationTicks());
        config.set("priority", effect.priority());
        config.set("visible", effect.visible());
        config.set("tags", new ArrayList<>(effect.tags()));
        config.set("conflicts", new ArrayList<>(effect.conflicts()));

        config.set("stacking.mode", effect.stackingMode().name());
        config.set("stacking.max-stacks", effect.maxStacks());
        if (effect.upgradeToEffectId() != null) {
            config.set("stacking.upgrade-to", effect.upgradeToEffectId());
        }

        List<Map<String, Object>> conditions = new ArrayList<>();
        for (EffectCondition condition : effect.conditions()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", condition.type().name());
            map.putAll(condition.params());
            conditions.add(map);
        }
        config.set("conditions", conditions);

        List<Map<String, Object>> components = new ArrayList<>();
        for (EffectComponent component : effect.components()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", component.type().name());
            map.put("trigger", component.trigger().name());
            map.putAll(component.params());
            components.add(map);
        }
        config.set("components", components);

        try {
            folder.mkdirs();
            config.save(new File(folder, effect.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el efecto " + effect.id(), e);
        }
    }

}
