package com.sack.rpgroll.ranching.core.health;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class VaccineParser implements ContentParser<Vaccine> {

    @Override
    public Vaccine parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new Vaccine(
                id,
                config.getString("display-name", id),
                config.getString("icon", "POTION"),
                config.getString("description", ""),
                lowercaseSet(config.getStringList("prevents-diseases")),
                config.getDouble("risk-reduction", 0.8),
                config.getLong("immunity-duration-ticks", 0L));
    }

    private Set<String> lowercaseSet(java.util.List<String> raw) {

        Set<String> result = new HashSet<>();

        for (String entry : raw) {
            result.add(entry.trim().toLowerCase(Locale.ROOT));
        }

        return result;
    }

}
