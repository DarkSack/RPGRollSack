package com.sack.rpgroll.extras.hud;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Carga plugins/RPGRoll-Extras/hud.yml (config singleton) — sección 25/26/27. */
public class HudSettingsLoader {

    public HudSettings load(JavaPlugin plugin) {

        File file = new File(plugin.getDataFolder(), "hud.yml");

        if (!file.exists()) {
            return HudSettings.disabled();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        boolean enabled = config.getBoolean("enabled", false);
        int interval = config.getInt("interval", 20);
        String separator = config.getString("separator", "  ");

        List<HudLineFormat> lines = new ArrayList<>();

        for (Map<?, ?> entry : config.getMapList("lines")) {

            Object statObj = entry.get("stat");
            Object formatObj = entry.get("format");

            if (statObj == null || formatObj == null) {
                continue;
            }

            boolean bar = entry.get("bar") != null && Boolean.parseBoolean(entry.get("bar").toString());
            int barLength = entry.get("bar-length") != null ? Integer.parseInt(entry.get("bar-length").toString()) : 10;
            String filled = entry.get("filled-char") != null ? entry.get("filled-char").toString() : "█";
            String empty = entry.get("empty-char") != null ? entry.get("empty-char").toString() : "░";

            lines.add(new HudLineFormat(statObj.toString(), formatObj.toString(), bar, barLength,
                    filled.charAt(0), empty.charAt(0)));
        }

        return new HudSettings(enabled, interval, separator, lines);
    }

}
