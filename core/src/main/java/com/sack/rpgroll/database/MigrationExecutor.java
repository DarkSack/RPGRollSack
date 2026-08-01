package com.sack.rpgroll.database;

import com.sack.rpgroll.RPGRoll;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Ejecuta una migración SQL dentro de una transacción.
 */
public class MigrationExecutor {

    private final RPGRoll plugin;
    private final Connection connection;
    private final SchemaVersionTracker tracker;

    public MigrationExecutor(
            RPGRoll plugin,
            Connection connection,
            SchemaVersionTracker tracker) {

        this.plugin = plugin;
        this.connection = connection;
        this.tracker = tracker;

    }

    public void execute(Migration migration)
            throws IOException, SQLException {

        if (tracker.isApplied(migration.version())) {

            plugin.getLogger().info(
                    "↺ V" + migration.version() + " ya aplicada.");

            return;

        }

        boolean previousAutoCommit = connection.getAutoCommit();

        try {

            connection.setAutoCommit(false);

            String sql = loadMigration(migration);

            plugin.getLogger().info(
                    "Ejecutando "
                            + migration.filename());

            try (Statement statement = connection.createStatement()) {

                String[] statements = sql.split(";");

                for (String current : statements) {

                    String trimmed = current.trim();

                    if (trimmed.isEmpty()) {
                        continue;
                    }

                    statement.execute(trimmed);

                }

            }

            tracker.recordMigration(migration.version());

            connection.commit();

            plugin.getLogger().info(
                    "✔ V"
                            + migration.version()
                            + " aplicada correctamente.");

        }

        catch (Exception exception) {

            try {

                connection.rollback();

                plugin.getLogger().warning(
                        "Rollback ejecutado.");

            }

            catch (SQLException rollbackException) {

                plugin.getLogger().severe(
                        "No fue posible hacer rollback.");

                rollbackException.printStackTrace();

            }

            throw exception;

        }

        finally {

            connection.setAutoCommit(previousAutoCommit);

        }

    }

    /**
     * Lee una migración desde el JAR.
     */
    private String loadMigration(Migration migration)
            throws IOException {

        try (InputStream stream = plugin.getResource(migration.resourcePath())) {

            if (stream == null) {

                throw new IOException(
                        "No existe el recurso "
                                + migration.resourcePath());

            }

            return new String(
                    stream.readAllBytes(),
                    StandardCharsets.UTF_8);

        }

    }

}