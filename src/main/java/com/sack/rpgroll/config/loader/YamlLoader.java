package com.sack.rpgroll.config.loader;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Carga archivos YAML desde la carpeta de datos del plugin.
 *
 * Responsabilidad única: leer un archivo del disco
 * y convertirlo en YamlConfiguration.
 *
 * No conoce qué archivos existen ni cuándo cargarlos.
 */
public class YamlLoader {

    private final JavaPlugin plugin;

    public YamlLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Carga un YAML relativo a plugins/RPGRoll/ como YamlConfiguration.
     *
     * @param relativePath ejemplo: "database.yml" o "lang/es_MX.yml"
     * @return configuración cargada, o null si el archivo no existe
     */
    public YamlConfiguration loadConfig(String relativePath) {

        File file = new File(plugin.getDataFolder(), relativePath);

        if (!file.exists()) {

            plugin.getLogger().warning(
                    "No se encontró el archivo: " + relativePath);

            return null;
        }

        return YamlConfiguration.loadConfiguration(file);

    }

    /**
     * Carga un YAML relativo a plugins/RPGRoll/ como Map<String, Object>.
     * Útil para trabajar con datos generales.
     *
     * @param relativePath ejemplo: "skills.yml" o "traits.yml"
     * @return mapa con los datos del YAML, o Map vacío si no existe
     */
    public Map<String, Object> load(String relativePath) {

        YamlConfiguration config = loadConfig(relativePath);

        if (config == null) {
            return new HashMap<>();
        }

        // Convertir YamlConfiguration a Map
        Map<String, Object> map = new HashMap<>();
        for (String key : config.getKeys(false)) {
            map.put(key, config.get(key));
        }

        return map;

    }

}