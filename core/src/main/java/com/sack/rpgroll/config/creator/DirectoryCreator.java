package com.sack.rpgroll.config.creator;

import com.sack.rpgroll.RPGRoll;

import java.io.File;
import java.util.List;

public class DirectoryCreator {

    private final RPGRoll plugin;

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

    public DirectoryCreator(RPGRoll plugin) {
        this.plugin = plugin;
    }

    public void create() {

        File dataFolder = plugin.getDataFolder();

        if (!dataFolder.exists()) {
            if (dataFolder.mkdirs()) {
                plugin.getLogger().info("✔ Carpeta principal creada.");
            } else {
                plugin.getLogger().severe("✘ No se pudo crear la carpeta principal: " + dataFolder.getPath());
            }
        }

        for (String directory : DIRECTORIES) {

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