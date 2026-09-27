package com.sack.rpgroll.database;

import com.sack.rpgroll.RPGRoll;

import java.sql.Connection;
import java.util.List;

/**
 * Coordina la ejecución de migraciones.
 */
public class DatabaseMigrator {

    private final RPGRoll plugin;
    private final Connection connection;
    private final MigrationRegistry.Set set;

    public DatabaseMigrator(RPGRoll plugin, Connection connection) {
        this(plugin, connection, MigrationRegistry.Set.SQLITE_ALL);
    }

    public DatabaseMigrator(RPGRoll plugin, Connection connection, MigrationRegistry.Set set) {
        this.plugin = plugin;
        this.connection = connection;
        this.set = set;
    }

    public void migrate() throws Exception {

        SchemaVersionTracker versionTracker = new SchemaVersionTracker(plugin, connection);

        versionTracker.initialize();

        MigrationRegistry registry = new MigrationRegistry(plugin, set);

        List<Migration> migrations = registry.load();

        if (migrations.isEmpty()) {
            plugin.getLogger().info("✔ No hay migraciones registradas.");
            return;
        }

        List<Migration> pending = versionTracker.getPendingMigrations(migrations);

        if (pending.isEmpty()) {
            plugin.getLogger().info("✔ Base de datos actualizada.");
            return;
        }

        plugin.getLogger().info("Migraciones pendientes: " + pending.size());

        MigrationExecutor executor = new MigrationExecutor(plugin, connection, versionTracker);

        for (Migration migration : pending) {

            executor.execute(migration);

        }

        plugin.getLogger().info("✔ Todas las migraciones fueron aplicadas.");

    }

}