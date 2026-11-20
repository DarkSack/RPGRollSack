package com.sack.rpgroll.tab.tablist;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.tab.format.PingTier;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TablistParser implements ContentParser<TablistDefinition> {

    @Override
    public TablistDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        List<TablistLine> headerLines = parseLines(config.getList("header.lines"));
        String headerAnimationId = config.getString("header.animation");

        List<TablistLine> footerLines = parseLines(config.getList("footer.lines"));
        String footerAnimationId = config.getString("footer.animation");

        String playerFormat = config.getString("player-format", "{prefix}{player}{suffix}");

        List<PingTier> pingTiers = parsePingTiers(config.getMapList("ping"));

        boolean gamemodeShown = config.getBoolean("gamemode.enabled", false);
        boolean gamemodeShortIcon = config.getBoolean("gamemode.short-icon", true);

        List<String> worldFilter = config.getStringList("worlds");

        return new TablistDefinition(id, headerLines, blankToNull(headerAnimationId), footerLines,
                blankToNull(footerAnimationId), playerFormat, pingTiers, gamemodeShown, gamemodeShortIcon,
                worldFilter, parseBlocks(config, headerLines, footerLines));
    }

    /** Cada línea es un texto o un mapa {@code {text, condition, block}}. */
    private List<TablistLine> parseLines(List<?> raw) {

        List<TablistLine> lines = new ArrayList<>();

        if (raw == null) {
            return lines;
        }

        for (Object entry : raw) {

            if (entry instanceof Map<?, ?> map) {
                Object text = map.get("text");
                if (text == null) {
                    continue;
                }
                lines.add(new TablistLine(text.toString(), blankToNull(str(map.get("condition"))),
                        blankToNull(str(map.get("block")))));
            } else if (entry != null) {
                lines.add(new TablistLine(entry.toString(), null, null));
            }
        }

        return lines;
    }

    /** Los bloques que usan las líneas, en orden, con su sección {@code blocks.<id>} (icono, nombre, default). */
    private List<TablistBlock> parseBlocks(YamlConfiguration config, List<TablistLine> header, List<TablistLine> footer) {

        Set<String> ids = new LinkedHashSet<>();
        for (TablistLine line : header) {
            if (line.block() != null) {
                ids.add(line.block());
            }
        }
        for (TablistLine line : footer) {
            if (line.block() != null) {
                ids.add(line.block());
            }
        }

        List<TablistBlock> blocks = new ArrayList<>();
        for (String id : ids) {
            String path = "blocks." + id;
            blocks.add(new TablistBlock(id, blankToNull(config.getString(path + ".name")),
                    blankToNull(config.getString(path + ".icon")), config.getBoolean(path + ".default", true)));
        }
        return blocks;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private List<PingTier> parsePingTiers(List<Map<?, ?>> raw) {

        List<PingTier> tiers = new ArrayList<>();

        for (Map<?, ?> entry : raw) {

            Object formatObj = entry.get("format");
            if (formatObj == null) {
                continue;
            }

            Object maxObj = entry.get("max");
            Integer max = maxObj == null ? null : Integer.parseInt(maxObj.toString());

            tiers.add(new PingTier(formatObj.toString(), max));
        }

        return tiers;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

}
