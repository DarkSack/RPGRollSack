package com.sack.rpgroll.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigRegistry {

    private final List<ConfigFile> files = new ArrayList<>();

    public ConfigRegistry() {
        registerDefaults();
    }

    private void registerDefaults() {

        files.add(new ConfigFile(
                "config/config.yml",
                "config.yml",
                true));

        files.add(new ConfigFile(
                "config/database.yml",
                "database.yml",
                true));

        files.add(new ConfigFile(
                "config/gameplay.yml",
                "gameplay.yml",
                true));

    }

    public List<ConfigFile> getFiles() {
        return Collections.unmodifiableList(files);
    }

}