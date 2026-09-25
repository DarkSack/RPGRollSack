package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.ascension.core.AscensionRequirementsParser;
import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class TitleParser implements ContentParser<Title> {

    @Override
    public Title parse(YamlConfiguration config) {

        String id = config.getString("id");

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        // Sin bloque requirements, el título no se desbloquea solo.
        var requirements = config.isConfigurationSection("requirements")
                ? AscensionRequirementsParser.parse(config.getConfigurationSection("requirements"))
                : null;

        return new Title(id, config.getString("display-name", id), requirements);
    }

}
