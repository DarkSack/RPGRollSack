package com.sack.rpgroll.ranching.core.health;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MedicineParser implements ContentParser<Medicine> {

    @Override
    public Medicine parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new Medicine(
                id,
                config.getString("display-name", id),
                config.getString("icon", "POTION"),
                config.getString("description", ""),
                parseType(config.getString("type")),
                lowercaseSet(config.getStringList("cures-diseases")),
                config.getDouble("cure-chance", 0.5),
                config.getLong("recovery-boost-ticks", 0L),
                config.getDouble("health-bonus", 0),
                config.getDouble("happiness-bonus", 0));
    }

    private MedicineType parseType(String raw) {

        if (raw == null || raw.isBlank()) {
            return MedicineType.VITAMIN;
        }

        try {
            return MedicineType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return MedicineType.VITAMIN;
        }
    }

    private Set<String> lowercaseSet(java.util.List<String> raw) {

        Set<String> result = new HashSet<>();

        for (String entry : raw) {
            result.add(entry.trim().toLowerCase(Locale.ROOT));
        }

        return result;
    }

}
