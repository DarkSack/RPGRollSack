package com.sack.rpgroll.crates.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Serializa un {@link Crate} completo de vuelta a YAML — inverso de {@link CrateParser}. */
public class CrateDefinitionWriter {

    private final File folder;
    private final Logger logger;

    public CrateDefinitionWriter(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public void save(Crate crate) {

        YamlConfiguration config = new YamlConfiguration();

        config.set("id", crate.id());
        config.set("display-name", crate.displayName());
        config.set("gui-title", crate.guiTitle());
        config.set("require-key", crate.requireKey());
        config.set("key.material", crate.keyMaterial().name());
        config.set("key.name", crate.keyDisplayName());
        config.set("key.lore", crate.keyLore());
        config.set("hologram", crate.hologramLines());

        List<Map<String, Object>> rewards = new ArrayList<>();
        for (CrateReward reward : crate.rewards()) {

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", reward.id());
            map.put("display-name", reward.displayName());
            map.put("icon", reward.icon().name());
            map.put("lore", reward.lore());
            map.put("weight", reward.weight());
            map.put("announce", reward.announceGlobally());

            List<Map<String, String>> actions = new ArrayList<>();
            for (CrateAction action : reward.actions()) {
                actions.add(Map.of("type", action.type().name(), "value", action.value()));
            }
            map.put("actions", actions);

            rewards.add(map);
        }
        config.set("rewards", rewards);

        try {
            if (!folder.exists()) {
                folder.mkdirs();
            }
            config.save(new File(folder, crate.id() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ Error guardando crate '" + crate.id() + "': " + e.getMessage());
        }
    }

}
