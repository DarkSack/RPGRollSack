package com.sack.rpgroll.race.loader;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.loader.YamlLoader;
import com.sack.rpgroll.gameplay.stats.StatType;
import com.sack.rpgroll.race.Race;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsable de leer los archivos YAML en plugins/RPGRoll/races/
 * y convertirlos en instancias de {@link Race}.
 * <p>
 * No decide qué hacer con las razas cargadas (eso es responsabilidad
 * de RaceRegistry) ni conoce TraitRegistry — los passive-traits se
 * cargan como simples IDs de texto, sin resolverlos.
 * <p>
 * Cada archivo se procesa de forma independiente: si uno falla (falta
 * un campo obligatorio, un Material inválido, etc.) se descarta ese
 * archivo con un warning y se continúa con los demás.
 */
public class RaceLoader {

    private final RPGRoll plugin;
    private final YamlLoader yamlLoader;

    public RaceLoader(RPGRoll plugin, YamlLoader yamlLoader) {
        this.plugin = plugin;
        this.yamlLoader = yamlLoader;
    }

    /**
     * Carga todas las razas definidas en plugins/RPGRoll/races/*.yml
     *
     * @return lista de razas cargadas exitosamente (excluye archivos inválidos)
     */
    public List<Race> load() {

        List<YamlConfiguration> files = yamlLoader.loadAllInFolder("races");
        List<Race> races = new ArrayList<>();

        for (YamlConfiguration config : files) {
            try {
                races.add(parse(config));
            } catch (Exception e) {
                plugin.getLogger().warning("✘ Error cargando raza: " + e.getMessage());
            }
        }

        plugin.getLogger().info("✔ " + races.size() + " raza(s) cargada(s).");

        return races;
    }

    private Race parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String displayName = config.getString("display-name", id);
        String description = config.getString("description", "");

        Map<StatType, Integer> baseAttributes = parseBaseAttributes(config, id);
        List<String> passiveTraits = config.getStringList("passive-traits");
        List<String> lore = config.getStringList("lore");
        Material icon = parseIcon(config, id);

        return new Race(id, displayName, description, baseAttributes, passiveTraits, icon, lore);
    }

    private Map<StatType, Integer> parseBaseAttributes(YamlConfiguration config, String raceId) {

        Map<StatType, Integer> attributes = new HashMap<>();

        ConfigurationSection section = config.getConfigurationSection("base-attributes");
        if (section == null) {
            return attributes;
        }

        for (String key : section.getKeys(false)) {

            StatType stat = StatType.fromString(key);

            if (stat == null) {
                plugin.getLogger().warning(
                        "✘ Raza '" + raceId + "': atributo desconocido '" + key + "', ignorado.");
                continue;
            }

            attributes.put(stat, section.getInt(key));
        }

        return attributes;
    }

    private Material parseIcon(YamlConfiguration config, String raceId) {

        String iconName = config.getString("icon");

        if (iconName == null || iconName.isBlank()) {
            plugin.getLogger().warning(
                    "✘ Raza '" + raceId + "' sin campo 'icon', usando BARRIER por defecto.");
            return Material.BARRIER;
        }

        Material material = Material.matchMaterial(iconName);

        if (material == null) {
            plugin.getLogger().warning(
                    "✘ Raza '" + raceId + "' tiene un icon inválido: '" + iconName + "', usando BARRIER por defecto.");
            return Material.BARRIER;
        }

        return material;
    }

}