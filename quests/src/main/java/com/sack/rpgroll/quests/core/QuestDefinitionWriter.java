package com.sack.rpgroll.quests.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Serializa un {@link Quest} completo de vuelta a YAML — inverso de
 * {@link QuestParser}. Necesario para que el editor visual pueda guardar
 * sin perder stages/diálogos/objetivos que el propio editor no expone
 * (solo edita campos de nivel superior + la lista de ids de stage).
 */
public class QuestDefinitionWriter {

    private final File folder;
    private final Logger logger;

    public QuestDefinitionWriter(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public void save(Quest quest) {

        YamlConfiguration config = new YamlConfiguration();

        config.set("id", quest.id());
        config.set("display-name", quest.displayName());
        config.set("category", quest.category().name());
        config.set("difficulty", quest.difficulty().name());
        config.set("repeatable", quest.repeatable());
        config.set("cooldown", quest.cooldownMillis() / 1000 + "s");

        writeRequirements(config, quest.requirements());
        config.set("stages", writeStages(quest.stages()));
        writeRewards(config, quest.rewards());
        writeEvents(config, "events", quest.events());

        try {
            if (!folder.exists()) {
                folder.mkdirs();
            }
            config.save(new File(folder, quest.id() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ Error guardando quest '" + quest.id() + "': " + e.getMessage());
        }
    }

    private void writeRequirements(YamlConfiguration config, QuestRequirements req) {

        config.set("requirements.level", req.level());
        config.set("requirements.race", req.race());
        config.set("requirements.class", req.playerClass());
        config.set("requirements.profession", req.profession());
        config.set("requirements.trait", req.trait());
        config.set("requirements.permission", req.permission());
        config.set("requirements.money", req.money());
        config.set("requirements.completed", req.completedQuests());
        config.set("requirements.world", req.world());
        config.set("requirements.region", req.region());
        config.set("requirements.biome", req.biome());
        config.set("requirements.weather", req.weather());

        if (req.hasTimeRange()) {
            config.set("requirements.hour", req.hourMin() + "-" + req.hourMax());
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (ItemRequirement item : req.items()) {
            items.add(Map.of("material", item.material().name(), "amount", item.amount()));
        }
        config.set("requirements.items", items);
    }

    private List<Map<String, Object>> writeStages(List<QuestStage> stages) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (QuestStage stage : stages) {

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", stage.id());

            if (!stage.objectives().isEmpty()) {
                List<Map<String, Object>> objectives = new ArrayList<>();
                for (QuestObjective objective : stage.objectives()) {
                    Map<String, Object> objMap = new LinkedHashMap<>(objective.params());
                    objMap.put("type", objective.type());
                    objMap.put("description", objective.description());
                    objMap.put("amount", objective.amount());
                    objectives.add(objMap);
                }
                map.put("objectives", objectives);
            }

            if (!stage.conditions().isEmpty()) {
                map.put("conditions", stage.conditions());
            }

            Dialog dialog = stage.dialog();
            if (dialog != null) {

                List<Map<String, String>> lines = new ArrayList<>();
                for (DialogLine line : dialog.lines()) {
                    lines.add(Map.of(line.speaker() == DialogSpeaker.NPC ? "npc" : "player", line.text()));
                }
                if (!lines.isEmpty()) {
                    map.put("dialog", lines);
                }

                if (dialog.hasOptions()) {
                    List<Map<String, String>> options = new ArrayList<>();
                    for (DialogOption option : dialog.options()) {
                        options.add(Map.of("label", option.label(), "next-stage", option.nextStage()));
                    }
                    map.put("options", options);
                }
            }

            for (var entry : stage.events().entrySet()) {
                if (entry.getValue().isEmpty()) {
                    continue;
                }
                String yamlKey = "on-" + entry.getKey().name().substring(3).toLowerCase(Locale.ROOT)
                        .replace('_', '-');
                map.put(yamlKey, writeActions(entry.getValue()));
            }

            result.add(map);
        }

        return result;
    }

    private List<Map<String, Object>> writeActions(List<QuestAction> actions) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (QuestAction action : actions) {
            Map<String, Object> map = new LinkedHashMap<>(action.params());
            map.put("type", action.type());
            result.add(map);
        }

        return result;
    }

    private void writeRewards(YamlConfiguration config, QuestRewards rewards) {

        config.set("rewards.money", rewards.money());
        config.set("rewards.experience", rewards.experience());

        List<String> items = new ArrayList<>();
        for (ItemRequirement item : rewards.items()) {
            items.add(item.material().name());
        }
        config.set("rewards.items", items);
        config.set("rewards.commands", rewards.commands());
        config.set("rewards.quests", rewards.quests());

        if (!rewards.extra().isEmpty()) {
            config.set("rewards.extra", writeActions(rewards.extra()));
        }
    }

    private void writeEvents(YamlConfiguration config, String path, Map<QuestEventType, List<QuestAction>> events) {

        for (var entry : events.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            String yamlKey = "on-" + entry.getKey().name().substring(3).toLowerCase(Locale.ROOT).replace('_', '-');
            config.set(path + "." + yamlKey, writeActions(entry.getValue()));
        }
    }

}
