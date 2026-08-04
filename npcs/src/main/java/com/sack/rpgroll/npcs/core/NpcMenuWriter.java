package com.sack.rpgroll.npcs.core;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NpcMenuWriter {

    private final Plugin plugin;

    public NpcMenuWriter(Plugin plugin) {
        this.plugin = plugin;
    }

    public void save(NpcMenuDefinition definition) {

        File folder = new File(plugin.getDataFolder(), "menus");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", definition.id());
        config.set("title", definition.title());
        config.set("rows", definition.rows());

        List<Map<String, Object>> itemMaps = new ArrayList<>();
        for (NpcMenuItem item : definition.items()) {

            List<Map<String, Object>> actionMaps = new ArrayList<>();
            for (NpcAction action : item.actions()) {
                actionMaps.add(Map.of("type", action.type().name(), "value", action.value()));
            }

            Map<String, Object> itemMap = new java.util.LinkedHashMap<>();
            itemMap.put("slot", item.slot());
            itemMap.put("material", item.material());
            itemMap.put("name", item.displayName());
            itemMap.put("lore", item.lore());
            itemMap.put("actions", actionMaps);
            itemMaps.add(itemMap);
        }
        config.set("items", itemMaps);

        try {
            config.save(new File(folder, definition.id() + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("✘ Error al guardar menú '" + definition.id() + "': " + e.getMessage());
        }
    }

}
