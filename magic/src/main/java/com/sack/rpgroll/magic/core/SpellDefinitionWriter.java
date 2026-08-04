package com.sack.rpgroll.magic.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpellDefinitionWriter {

    private final File folder;

    public SpellDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "spells");
    }

    public void save(Spell spell) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", spell.id());
        config.set("display-name", spell.displayName());
        config.set("icon", spell.icon());
        config.set("color", spell.color());
        config.set("school", spell.schoolId());
        config.set("rarity", spell.rarity().name());
        config.set("level", spell.level());
        config.set("cast-time", spell.castTimeTicks());
        config.set("cooldown", spell.cooldownTicks());
        config.set("trigger", spell.trigger().name());
        config.set("tree-parent", spell.treeParentId());
        config.set("tree-tier", spell.treeTier());
        config.set("tags", List.copyOf(spell.tags()));
        config.set("description", spell.description());

        SpellCost cost = spell.cost();
        config.set("cost.mana", cost.mana());
        config.set("cost.health", cost.health());
        config.set("cost.experience", cost.experience());
        config.set("cost.reagent-material", cost.reagentMaterial());
        config.set("cost.reagent-amount", cost.reagentAmount());

        List<Map<String, Object>> components = new java.util.ArrayList<>();

        for (SpellComponent component : spell.components()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", component.type().name());
            map.putAll(component.params());
            components.add(map);
        }

        config.set("components", components);

        try {
            folder.mkdirs();
            config.save(new File(folder, spell.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el hechizo " + spell.id(), e);
        }
    }

}
