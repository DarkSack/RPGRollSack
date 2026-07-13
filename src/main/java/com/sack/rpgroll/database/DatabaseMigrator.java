package com.sack.rpgroll.database;

import com.sack.rpgroll.RPGRoll;

import java.sql.Connection;
import java.util.List;

public class DatabaseMigrator {

    private final RPGRoll plugin;

    private final Connection connection;

    public DatabaseMigrator(
            RPGRoll plugin,
            Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    public void migrate() {

        MigrationRegistry registry = new MigrationRegistry(plugin);

        List<Migration> migrations = registry.load();

        MigrationExecutor executor = new MigrationExecutor(connection);

        for (Migration migration : migrations) {

            try {

                plugin.getLogger().info(
                        "Aplicando " + migration.filename());

                executor.execute(migration);

            } catch (Exception exception) {

                plugin.getLogger().severe(
                        "Error aplicando " + migration.filename());

                exception.printStackTrace();

            }

        }

    }

}