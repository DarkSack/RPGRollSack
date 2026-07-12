package com.sack.rpgroll.config;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.creator.DirectoryCreator;
import com.sack.rpgroll.config.creator.ResourceCopier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigManager {

    private final RPGRoll plugin;

    private final List<ConfigFile> configFiles = new ArrayList<>();

    private final DirectoryCreator directoryCreator;
    private final ResourceCopier resourceCopier;

    public ConfigManager(RPGRoll plugin) {

        this.plugin = plugin;

        registerFiles();

        this.directoryCreator = new DirectoryCreator(plugin);
        this.resourceCopier = new ResourceCopier(plugin, configFiles);

    }

    /**
     * Inicializa todo el sistema de configuración.
     */
    public void initialize() {

        plugin.getLogger().info("");
        plugin.getLogger().info("========== ConfigManager ==========");

        directoryCreator.create();

        resourceCopier.copy();

        plugin.getLogger().info("Configuración inicializada correctamente.");
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

        // Idioma por defecto
        configFiles.add(new ConfigFile(
                "lang/es_MX.yml",
                "lang/es_MX.yml",
                true));

    }

    /**
     * Devuelve todos los archivos registrados.
     */
    public List<ConfigFile> getConfigFiles() {
        return Collections.unmodifiableList(configFiles);
    }

}