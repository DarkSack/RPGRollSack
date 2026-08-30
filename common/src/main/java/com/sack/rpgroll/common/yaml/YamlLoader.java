package com.sack.rpgroll.common.yaml;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga archivos YAML desde la carpeta de datos del plugin.
 *
 * Responsabilidad única: leer archivo(s) del disco
 * y convertirlos en YamlConfiguration.
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
     * Útil para trabajar con datos generales de estructura plana (sin
     * secciones anidadas). Para YAML con estructuras anidadas (como las
     * de Race, con base-attributes o listas de traits), preferir
     * loadConfig() o loadAllInFolder() y navegar el YamlConfiguration
     * directamente con getConfigurationSection() / getStringList().
     *
     * @param relativePath ejemplo: "skills.yml" o "traits.yml"
     * @return mapa con los datos del YAML, o Map vacío si no existe
     */
    public Map<String, Object> load(String relativePath) {

        YamlConfiguration config = loadConfig(relativePath);

        if (config == null) {
            return new HashMap<>();
        }

        Map<String, Object> map = new HashMap<>();
        for (String key : config.getKeys(false)) {
            map.put(key, config.get(key));
        }

        return map;

    }

    /**
     * Lista y carga todos los archivos .yml que existen dentro de una carpeta
     * relativa a plugins/RPGRoll/, sin conocer sus nombres de antemano.
     * <p>
     * Pensado para directorios de contenido variable (races/, classes/,
     * skills/, professions/, items/, quests/) donde el administrador puede
     * agregar, renombrar o quitar archivos libremente sin tocar código.
     * <p>
     * A diferencia de {@link #loadConfig(String)}, no reporta warning si la
     * carpeta está vacía o no existe — es un estado válido (ej. antes de que
     * exista contenido de ejemplo para esa carpeta).
     *
     * @param relativeFolder ejemplo: "races" o "classes"
     * @return lista de YamlConfiguration cargadas, vacía si la carpeta no existe o
     *         no tiene .yml
     */
    /**
     * Un archivo cuenta como contenido si es .yml y NO empieza con "_". El
     * guion bajo marca archivos internos del propio plugin que viven en la
     * misma carpeta que el contenido (ej. {@code market/_state.yml}, donde
     * RPGRoll-Economy persiste la oferta/demanda): sin este filtro, el loader
     * intenta parsearlos como definiciones y avisa en cada arranque que les
     * falta el campo 'id'.
     */
    private static boolean isContentFile(String fileName) {
        return fileName.toLowerCase().endsWith(".yml") && !fileName.startsWith("_");
    }

    public List<YamlConfiguration> loadAllInFolder(String relativeFolder) {

        File folder = new File(plugin.getDataFolder(), relativeFolder);

        if (!folder.exists() || !folder.isDirectory()) {
            return List.of();
        }

        File[] files = folder.listFiles((dir, name) -> isContentFile(name));

        if (files == null || files.length == 0) {
            return List.of();
        }

        List<YamlConfiguration> configs = new ArrayList<>();

        for (File file : files) {
            try {
                configs.add(YamlConfiguration.loadConfiguration(file));
            } catch (Exception e) {
                plugin.getLogger().warning(
                        "✘ Error al parsear YAML: " + relativeFolder + "/" + file.getName()
                                + " — " + e.getMessage());
            }
        }

        return configs;

    }

    /**
     * Igual que {@link #loadAllInFolder(String)}, pero recorre subcarpetas
     * recursivamente. Pensado para contenido organizado en categorías (ej.
     * items/sword/flame_blade.yml, items/armor/knight_helmet.yml) donde el
     * administrador agrupa archivos libremente sin que la estructura de
     * carpetas tenga significado para el loader — solo se usa para ordenar.
     *
     * @param relativeFolder ejemplo: "items"
     * @return lista de YamlConfiguration cargadas, vacía si la carpeta no existe
     */
    public List<YamlConfiguration> loadAllInFolderRecursive(String relativeFolder) {

        File folder = new File(plugin.getDataFolder(), relativeFolder);

        if (!folder.exists() || !folder.isDirectory()) {
            return List.of();
        }

        List<YamlConfiguration> configs = new ArrayList<>();
        collectRecursive(folder, relativeFolder, configs);

        return configs;

    }

    private void collectRecursive(File folder, String relativeFolder, List<YamlConfiguration> configs) {

        File[] files = folder.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {

            if (file.isDirectory()) {
                collectRecursive(file, relativeFolder, configs);
                continue;
            }

            if (!isContentFile(file.getName())) {
                continue;
            }

            try {
                configs.add(YamlConfiguration.loadConfiguration(file));
            } catch (Exception e) {
                plugin.getLogger().warning(
                        "✘ Error al parsear YAML: " + relativeFolder + "/" + file.getName()
                                + " — " + e.getMessage());
            }
        }

    }

    /**
     * Igual que {@link #loadAllInFolder(String)}, pero además expone el nombre
     * de archivo (sin extensión) de cada configuración cargada. Útil cuando
     * el loader que consume esto necesita el filename como fallback de id
     * (ej. si el YAML no declara explícitamente su propio "id").
     *
     * @param relativeFolder ejemplo: "races" o "classes"
     * @return mapa de nombre de archivo (sin .yml) -> YamlConfiguration cargada
     */
    public Map<String, YamlConfiguration> loadAllInFolderWithNames(String relativeFolder) {

        File folder = new File(plugin.getDataFolder(), relativeFolder);

        if (!folder.exists() || !folder.isDirectory()) {
            return Map.of();
        }

        File[] files = folder.listFiles((dir, name) -> isContentFile(name));

        if (files == null || files.length == 0) {
            return Map.of();
        }

        Map<String, YamlConfiguration> configs = new HashMap<>();

        for (File file : files) {
            try {
                String nameWithoutExtension = file.getName().replaceFirst("\\.yml$", "");
                configs.put(nameWithoutExtension, YamlConfiguration.loadConfiguration(file));
            } catch (Exception e) {
                plugin.getLogger().warning(
                        "✘ Error al parsear YAML: " + relativeFolder + "/" + file.getName()
                                + " — " + e.getMessage());
            }
        }

        return configs;

    }

}