package com.sack.rpgroll.tab.scoreboard;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScoreboardParser implements ContentParser<ScoreboardDefinition> {

    @Override
    public ScoreboardDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String title = config.getString("title");
        String titleAnimationId = config.getString("title-animation");

        List<ScoreboardLine> lines = parseLines(config.getMapList("lines"), config.getStringList("lines"));

        String extendsId = config.getString("extends");
        Map<String, String> replacements = parseReplacements(config.getConfigurationSection("replacements"));
        int priority = config.getInt("priority", 0);

        return new ScoreboardDefinition(id, title, titleAnimationId, lines, extendsId, replacements, priority);
    }

    /**
     * Soporta dos formas de {@code lines}: lista de strings simples (sin condición)
     * o lista de mapas {@code {text: ..., condition: ...}} (sección 25, placeholder conditions).
     */
    private List<ScoreboardLine> parseLines(List<Map<?, ?>> mapLines, List<String> stringLines) {

        List<ScoreboardLine> lines = new ArrayList<>();

        if (!mapLines.isEmpty()) {

            for (Map<?, ?> entry : mapLines) {

                Object textObj = entry.get("text");
                if (textObj == null) {
                    continue;
                }

                Object conditionObj = entry.get("condition");
                lines.add(new ScoreboardLine(textObj.toString(), conditionObj == null ? null : conditionObj.toString()));
            }

            return lines;
        }

        for (String line : stringLines) {
            lines.add(new ScoreboardLine(line, null));
        }

        return lines;
    }

    private Map<String, String> parseReplacements(org.bukkit.configuration.ConfigurationSection section) {

        Map<String, String> replacements = new LinkedHashMap<>();

        if (section == null) {
            return replacements;
        }

        for (String key : section.getKeys(false)) {
            replacements.put(key, String.valueOf(section.get(key)));
        }

        return replacements;
    }

}
