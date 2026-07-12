package com.sack.rpgroll.config.creator;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.ConfigFile;

import java.io.File;
import java.util.List;

public class ResourceCopier {

    private final RPGRoll plugin;
    private final List<ConfigFile> configFiles;

    public ResourceCopier(RPGRoll plugin, List<ConfigFile> configFiles) {
        this.plugin = plugin;
        this.configFiles = configFiles;
    }

    public void copy() {

        plugin.getLogger().info("Copiando recursos...");

        for (ConfigFile config : configFiles) {
            copyIfMissing(config);
        }

    }

    private void copyIfMissing(ConfigFile config) {

        File destination = new File(
                plugin.getDataFolder(),
                config.destination());

        if (destination.exists()) {
            return;
        }

        File parent = destination.getParentFile();

        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        plugin.saveResource(config.resource(), false);

        plugin.getLogger().info("✔ Archivo creado: " + config.destination());

    }

}