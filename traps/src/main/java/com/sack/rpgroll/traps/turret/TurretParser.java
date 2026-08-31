package com.sack.rpgroll.traps.turret;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.traps.core.TrapAction;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TurretParser implements ContentParser<TurretDefinition> {

    @Override
    public TurretDefinition parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String displayName = config.getString("display-name", id);
        String description = config.getString("description", "");
        double radius = config.getDouble("radius", 12.0);
        boolean targetPlayers = config.getBoolean("target-players", true);
        boolean targetHostileMobs = config.getBoolean("target-hostile-mobs", true);
        long fireIntervalTicks = config.getLong("fire-interval-ticks", 20);
        List<String> conditions = config.getStringList("conditions");
        TrapAction impact = parseImpact(config);

        Material model = parseMaterial(config.getString("model.material"), Material.CROSSBOW);
        Integer customModelData = config.contains("model.custom-model-data")
                ? config.getInt("model.custom-model-data")
                : null;

        // Sin "model.base-block" no se coloca ningún bloque: una torreta puede
        // ser solo el ítem flotante, como era antes de esta opción.
        Material baseBlock = config.contains("model.base-block")
                ? parseMaterial(config.getString("model.base-block"), null)
                : null;

        // -1 (y no 0) señala "sin definir" para que el record aplique su
        // default: 0 es un valor válido que significa "quieto a propósito".
        double hoverHeight = config.getDouble("model.hover-height", -1);
        double spinDegreesPerTick = config.contains("model.spin-degrees-per-tick")
                ? config.getDouble("model.spin-degrees-per-tick")
                : -1;

        return new TurretDefinition(id, displayName, description, radius, targetPlayers, targetHostileMobs,
                fireIntervalTicks, conditions, impact, model, customModelData,
                baseBlock, hoverHeight, spinDegreesPerTick);
    }

    /** Sin sección "impact": null — el compact constructor de TurretDefinition rellena el default (flecha). */
    private TrapAction parseImpact(YamlConfiguration config) {

        String type = config.getString("impact.type");
        if (type == null || type.isBlank()) {
            return null;
        }

        Map<String, String> params = new LinkedHashMap<>();
        ConfigurationSection paramsSection = config.getConfigurationSection("impact.params");

        if (paramsSection != null) {
            for (String key : paramsSection.getKeys(false)) {
                Object value = paramsSection.get(key);
                if (value != null) {
                    params.put(key, value.toString());
                }
            }
        }

        return new TrapAction(type.trim().toUpperCase(Locale.ROOT), params);
    }

    private Material parseMaterial(String raw, Material fallback) {

        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

}
