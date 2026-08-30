package com.sack.rpgroll.traps.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Serializa un {@link TrapDefinition} completo de vuelta a YAML — inverso de {@link TrapParser}. */
public class TrapDefinitionWriter {

    private final File folder;
    private final Logger logger;

    public TrapDefinitionWriter(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public void save(TrapDefinition trap) {

        YamlConfiguration config = new YamlConfiguration();

        config.set("id", trap.id());
        config.set("display-name", trap.displayName());
        config.set("description", trap.description());
        config.set("icon", trap.icon().name());

        config.set("trigger.type", trap.trigger().name());
        for (var entry : trap.triggerParams().entrySet()) {
            config.set("trigger.params." + entry.getKey(), entry.getValue());
        }

        config.set("radius", trap.radius());
        config.set("conditions", trap.conditions());
        config.set("cooldown-millis", trap.cooldownMillis());
        config.set("charges", trap.charges());
        config.set("chain", trap.chain());

        List<Map<String, Object>> actions = new ArrayList<>();
        for (TrapAction action : trap.actions()) {
            actions.add(Map.of("type", action.type(), "params", action.params()));
        }
        config.set("actions", actions);

        TrapBlockConfig block = trap.block();
        if (block.isConfigured()) {
            config.set("block.material", block.material().name());
            config.set("block.breakable", block.breakable());
            config.set("block.required-item-id", block.requiredItemId());
            config.set("block.break-time-seconds", block.breakTimeSeconds());
            config.set("block.explosion-immune", block.explosionImmune());
            config.set("block.piston-immune", block.pistonImmune());
        }

        TrapDisguiseConfig disguise = trap.disguise();
        if (disguise.isActive()) {
            config.set("disguise.mode", disguise.mode().name());
            config.set("disguise.visible-material", disguise.visibleMaterial().name());
            config.set("disguise.revealed-material",
                    disguise.revealedMaterial() == null ? null : disguise.revealedMaterial().name());
        }

        try {
            if (!folder.exists()) {
                folder.mkdirs();
            }
            config.save(new File(folder, trap.id() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ Error guardando trampa '" + trap.id() + "': " + e.getMessage());
        }
    }

}
