package com.sack.rpgroll.config;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.common.resource.ResourceFile;
import com.sack.rpgroll.common.yaml.YamlLoader;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigManager {

    /** Carpetas de datos que RPGRoll necesita, tengan o no contenido de ejemplo. */
    private static final List<String> DIRECTORIES = List.of(
            "classes",
            "races",
            "skills",
            "traits",
            "jobs",
            "lang",
            "database",
            "professions",
            "items",
            "quests");

    /** Carpetas de contenido variable que deben poblarse con YAML de ejemplo desde resources/. */
    private static final List<String> CONTENT_DIRECTORIES = List.of(
            "races",
            "classes",
            "skills",
            "traits",
            "professions",
            "items",
            "quests",
            "jobs");

    private final RPGRoll plugin;

    private final List<ResourceFile> configFiles = new ArrayList<>();

    private final DirectoryCreator directoryCreator;
    private final ResourceCopier resourceCopier;
    private final YamlLoader yamlLoader;

    public ConfigManager(RPGRoll plugin) {

        this.plugin = plugin;

        registerFiles();

        this.directoryCreator = new DirectoryCreator(plugin);
        this.resourceCopier = new ResourceCopier(plugin);
        this.yamlLoader = new YamlLoader(plugin);

    }

    /**
     * Inicializa todo el sistema de configuración.
     */
    public void initialize() {

        plugin.getLogger().info("");
        plugin.getLogger().info("========== ConfigManager ==========");

        try {
            directoryCreator.create(DIRECTORIES);
            resourceCopier.copyFiles(configFiles);
            resourceCopier.copyDirectories(CONTENT_DIRECTORIES);
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
        configFiles.add(new ResourceFile(
                "config/config.yml",
                "config.yml",
                true));

        // Configuración de la base de datos
        configFiles.add(new ResourceFile(
                "config/database.yml",
                "database.yml",
                true));

        // Configuración del gameplay
        configFiles.add(new ResourceFile(
                "config/gameplay.yml",
                "gameplay.yml",
                true));

        // Configuración de recompensas de level up
        configFiles.add(new ResourceFile(
                "config/levelup-rewards.yml",
                "levelup-rewards.yml",
                true));

        // Idioma por defecto
        configFiles.add(new ResourceFile(
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
    public List<ResourceFile> getConfigFiles() {
        return Collections.unmodifiableList(configFiles);
    }

}
