package com.sack.rpgroll.core;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.ConfigManager;

public class Bootstrap {

    private final RPGRoll plugin;
    private final ServiceRegistry services;

    public Bootstrap(RPGRoll plugin) {
        this.plugin = plugin;
        this.services = new ServiceRegistry();
    }

    public void initialize() {

        printBanner();

        registerCoreServices();

        plugin.getLogger().info("==================================");
        plugin.getLogger().info("RPGRoll iniciado correctamente.");
        plugin.getLogger().info("==================================");

    }

    public void shutdown() {

        plugin.getLogger().info("Deteniendo servicios...");

        // Aquí cerraremos SQLite, cache, etc.
    }

    private void registerCoreServices() {

        plugin.getLogger().info("Inicializando servicios...");

        ConfigManager configManager = new ConfigManager(plugin);

        configManager.initialize();

        services.register(ConfigManager.class, configManager);

        plugin.getLogger().info("✔ ConfigManager registrado");

    }

    private void printBanner() {

        plugin.getLogger().info("");
        plugin.getLogger().info("==================================");
        plugin.getLogger().info("          RPGRoll");
        plugin.getLogger().info("     RPG Framework for Paper");
        plugin.getLogger().info("==================================");

    }

    public ServiceRegistry getServices() {
        return services;
    }

}