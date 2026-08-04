package com.sack.rpgroll.seasons.climate;

import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Locale;

/**
 * Temperatura base en °C por bioma, en un archivo plano configurable
 * (plugins/RPGRoll-Seasons/biome-temperatures.yml) — Bukkit no expone la
 * temperatura interna real del bioma como un double consultable de forma
 * estable entre versiones, así que este módulo mantiene su propia tabla
 * aproximada en vez de depender de eso.
 */
public class BiomeTemperatureTable {

    private static final double DEFAULT_TEMPERATURE = 15.0;

    private final YamlConfiguration config;

    public BiomeTemperatureTable(Plugin plugin) {

        File file = new File(plugin.getDataFolder(), "biome-temperatures.yml");
        this.config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
    }

    public double getBaseTemperature(Biome biome) {
        return config.getDouble(biome.name().toLowerCase(Locale.ROOT), DEFAULT_TEMPERATURE);
    }

}
