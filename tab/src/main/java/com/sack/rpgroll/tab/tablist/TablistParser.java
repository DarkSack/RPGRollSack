package com.sack.rpgroll.tab.tablist;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.tab.format.PingTier;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TablistParser implements ContentParser<TablistDefinition> {

    @Override
    public TablistDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        List<String> headerLines = config.getStringList("header.lines");
        String headerAnimationId = config.getString("header.animation");

        List<String> footerLines = config.getStringList("footer.lines");
        String footerAnimationId = config.getString("footer.animation");

        String playerFormat = config.getString("player-format", "{prefix}{player}{suffix}");

        List<PingTier> pingTiers = parsePingTiers(config.getMapList("ping"));

        boolean gamemodeShown = config.getBoolean("gamemode.enabled", false);
        boolean gamemodeShortIcon = config.getBoolean("gamemode.short-icon", true);

        List<String> worldFilter = config.getStringList("worlds");

        return new TablistDefinition(id, headerLines, blankToNull(headerAnimationId), footerLines,
                blankToNull(footerAnimationId), playerFormat, pingTiers, gamemodeShown, gamemodeShortIcon,
                worldFilter);
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
