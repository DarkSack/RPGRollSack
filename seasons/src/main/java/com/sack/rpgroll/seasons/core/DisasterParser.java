package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DisasterParser implements ContentParser<Disaster> {

    @Override
    public Disaster parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        List<DisasterPhenomenon> phenomena = parsePhenomena(config.getMapList("phenomena"), id);
        List<String> combine = config.getStringList("combine");

        if (phenomena.isEmpty() && combine.isEmpty()) {
            throw new IllegalArgumentException("desastre '" + id + "' sin 'phenomena' ni 'combine'");
        }

        return new Disaster(id, config.getString("display-name", id), config.getString("icon", "TNT"),
                config.getString("description", ""), config.getInt("duration", 1200),
                config.getDouble("intensity", 1.0), config.getDouble("chance", 0.0),
                config.getStringList("seasons"), config.getStringList("worlds"), combine, phenomena,
                messages(config), config.getString("start-sound", "EVENT_RAID_HORN 1 0.8"),
                config.getStringList("commands.start"), config.getStringList("commands.end"));
    }

    private Map<String, String> messages(YamlConfiguration config) {
        Map<String, String> messages = new LinkedHashMap<>();
        var section = config.getConfigurationSection("messages");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                messages.put(key, section.getString(key, ""));
            }
        }
        return messages;
    }

    private List<DisasterPhenomenon> parsePhenomena(List<Map<?, ?>> raw, String disasterId) {

        List<DisasterPhenomenon> phenomena = new ArrayList<>();

        for (Map<?, ?> map : raw) {

            Object rawType = map.get("type");
            DisasterType type;

            try {
                type = DisasterType.valueOf(String.valueOf(rawType).trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("desastre '" + disasterId + "' tiene un fenómeno con type inválido: "
                        + rawType + " (válidos: TORNADO, METEOR_STORM, ACID_RAIN, EARTHQUAKE)");
            }

            Map<String, Object> params = new LinkedHashMap<>();

            for (var entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (!key.equals("type") && entry.getValue() != null) {
                    params.put(key, entry.getValue());
                }
            }

            phenomena.add(new DisasterPhenomenon(type, params));
        }

        return phenomena;
    }

}
