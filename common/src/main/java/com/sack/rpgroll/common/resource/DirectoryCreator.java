package com.sack.rpgroll.common.resource;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.List;

/**
 * Crea las carpetas de datos que un plugin necesita en su primer arranque.
 * Genérico — no asume ningún nombre de carpeta específico, cada plugin
 * (RPGRoll o cualquiera de sus addons) pasa la lista que le corresponde.
 * Compartido vía :common para no reimplementar esta lógica en cada uno.
 */
public class DirectoryCreator {

    private final Plugin plugin;

    public DirectoryCreator(Plugin plugin) {
        this.plugin = plugin;
    }

    public void create(List<String> directories) {

        File dataFolder = plugin.getDataFolder();

        if (!dataFolder.exists()) {
            if (dataFolder.mkdirs()) {
                plugin.getLogger().info("✔ Carpeta principal creada.");
            } else {
                plugin.getLogger().severe("✘ No se pudo crear la carpeta principal: " + dataFolder.getPath());
            }
        }

        for (String directory : directories) {

            File folder = new File(dataFolder, directory);

            if (!folder.exists()) {
                if (folder.mkdirs()) {
                    plugin.getLogger().info("✔ Carpeta creada: " + directory);
                } else {
                    plugin.getLogger().severe("✘ No se pudo crear la carpeta: " + directory);
                }
            }
        }
    }

}
