package com.sack.rpgroll.config;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.creator.DirectoryCreator;
import com.sack.rpgroll.config.creator.ResourceCopier;
import com.sack.rpgroll.common.yaml.YamlLoader;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigManager {

    private final RPGRoll plugin;

    private final List<ConfigFile> configFiles = new ArrayList<>();

    private final DirectoryCreator directoryCreator;
    private final ResourceCopier resourceCopier;
    private final YamlLoader yamlLoader;

    public ConfigManager(RPGRoll plugin) {

        this.plugin = plugin;

        registerFiles();

        this.directoryCreator = new DirectoryCreator(plugin);
        this.resourceCopier = new ResourceCopier(plugin, configFiles);
        this.yamlLoader = new YamlLoader(plugin);

    }

    /**
     * Inicializa todo el sistema de configuración.
     */
    public void initialize() {

        plugin.getLogger().info("");
        plugin.getLogger().info("========== ConfigManager ==========");

        try {
            directoryCreator.create();
            resourceCopier.copy();
            plugin.getLogger().info("Configuración inicializada correctamente.");
        } catch (Exception e) {
            plugin.getLogger().severe("✘ Error crítico inicializando configuración: " + e.getMessage());
            throw new IllegalStateException("Fallo al inicializar ConfigManager", e);
        }

        plugin.getLogger().info("===================================");
    }

    /**
     * Registra todos los archivos que administra el plugin.
     */
    private void registerFiles() {

        // Configuración principal
        configFiles.add(new ConfigFile(
                "config/config.yml",
                "config.yml",
                true));

        // Configuración de la base de datos
        configFiles.add(new ConfigFile(
                "config/database.yml",
                "database.yml",
                true));

        // Configuración del gameplay
        configFiles.add(new ConfigFile(
                "config/gameplay.yml",
                "gameplay.yml",
                true));

        // Configuración de recompensas de level up
        configFiles.add(new ConfigFile(
                "config/levelup-rewards.yml",
                "levelup-rewards.yml",
                true));

        // Idioma por defecto
        configFiles.add(new ConfigFile(
                "lang/es_MX.yml",
                "lang/es_MX.yml",
                true));

    }

    /**
     * Carga un archivo YAML específico como YamlConfiguration.
     * 
     * Útil para cargar configuraciones después de la inicialización.
     * 
     * @param filename nombre del archivo (relativo a plugins/RPGRoll/)
     * @return YamlConfiguration cargada, o null si no existe
     */
    public YamlConfiguration getConfig(String filename) {
        return yamlLoader.loadConfig(filename);
    }

    /**
     * Obtiene el cargador de YAML para cargas manuales.
     */
    public YamlLoader getYamlLoader() {
        return yamlLoader;
    }

    /**
     * Devuelve todos los archivos registrados.
     */
    public List<ConfigFile> getConfigFiles() {
        return Collections.unmodifiableList(configFiles);
    }

}