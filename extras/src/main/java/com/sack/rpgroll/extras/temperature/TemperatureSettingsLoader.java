package com.sack.rpgroll.extras.temperature;

import com.sack.rpgroll.extras.action.ActionParsingUtil;
import com.sack.rpgroll.extras.action.ExtrasAction;
import com.sack.rpgroll.extras.stat.PotionSpec;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Carga plugins/RPGRoll-Extras/temperature.yml (config singleton, no multi-archivo) — si falta, usa {@link TemperatureSettings#defaults()}. */
public class TemperatureSettingsLoader {

    public TemperatureSettings load(JavaPlugin plugin) {

        File file = new File(plugin.getDataFolder(), "temperature.yml");

        if (!file.exists()) {
            return TemperatureSettings.defaults();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        double exchangeRate = config.getDouble("exchange-rate", 0.05);
        int updateInterval = config.getInt("update-interval", 40);

        List<Map<?, ?>> rawStates = config.getMapList("states");

        if (rawStates.isEmpty()) {
            return new TemperatureSettings(exchangeRate, updateInterval, TemperatureSettings.defaults().states());
        }

        List<TemperatureStateRange> states = new ArrayList<>();

        for (Map<?, ?> entry : rawStates) {

            Object idObj = entry.get("id");
            if (idObj == null) {
                continue;
            }

            String label = entry.get("label") != null ? entry.get("label").toString() : idObj.toString();
            double min = entry.get("min") != null ? Double.parseDouble(entry.get("min").toString()) : Double.NEGATIVE_INFINITY;
            double max = entry.get("max") != null ? Double.parseDouble(entry.get("max").toString()) : Double.POSITIVE_INFINITY;

            List<PotionSpec> potions = parsePotions(entry.get("potions"));

            List<Map<?, ?>> rawActions = new ArrayList<>();
            if (entry.get("actions") instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        rawActions.add(map);
                    }
                }
            }

            List<ExtrasAction> actions = ActionParsingUtil.parseActions(rawActions);

            states.add(new TemperatureStateRange(idObj.toString(), label, min, max, potions, actions));
        }

        return new TemperatureSettings(exchangeRate, updateInterval, states);
    }

    private List<PotionSpec> parsePotions(Object raw) {

        List<PotionSpec> potions = new ArrayList<>();

        if (!(raw instanceof List<?> list)) {
            return potions;
        }

        for (Object item : list) {

            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }

            Object typeObj = map.get("type");
            if (typeObj == null) {
                continue;
            }

            int amplifier = map.get("amplifier") != null ? Integer.parseInt(map.get("amplifier").toString()) : 0;
            potions.add(new PotionSpec(typeObj.toString(), amplifier));
        }

        return potions;
    }

}
