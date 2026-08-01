package com.sack.rpgroll.database;

import com.sack.rpgroll.RPGRoll;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Implementación SQLite del DatabaseProvider.
 */
public class SQLiteProvider implements DatabaseProvider {

    private final RPGRoll plugin;

    private Connection connection;

    private final String databasePath;

    public SQLiteProvider(RPGRoll plugin) {

        this.plugin = plugin;

        File folder = new File(plugin.getDataFolder(), "database");

        if (!folder.exists()) {
            folder.mkdirs();
        }

        databasePath = new File(folder, "rpgroll.db").getAbsolutePath();

    }

    @Override
    public void connect() {

        if (isConnected()) {
            plugin.getLogger().warning("Ya existe una conexión SQLite.");
            return;
        }

        try {

            Class.forName("org.sqlite.JDBC");

            connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + databasePath);

            configureSQLite();

            plugin.getLogger().info("✔ SQLite conectado.");
            plugin.getLogger().info("Ruta: " + databasePath);

        }

        catch (Exception exception) {

            plugin.getLogger().severe("No fue posible conectar SQLite.");

            exception.printStackTrace();

        }

    }

    /**
     * Configura SQLite.
     */
    private void configureSQLite() throws SQLException {

        executePragma("PRAGMA foreign_keys = ON;");
        executePragma("PRAGMA journal_mode = WAL;");
        executePragma("PRAGMA synchronous = NORMAL;");
        executePragma("PRAGMA busy_timeout = 5000;");

    }

    /**
     * Ejecuta un PRAGMA cerrando correctamente el Statement.
     */
    private void executePragma(String sql) throws SQLException {

        try (Statement statement = connection.createStatement()) {

            statement.execute(sql);

        }

    }

    @Override
    public void disconnect() {

        if (!isConnected()) {
            return;
        }

        try {

            connection.close();

            connection = null;

            plugin.getLogger().info("✔ SQLite desconectado.");

        }

        catch (SQLException exception) {

            plugin.getLogger().severe("Error cerrando SQLite.");

            exception.printStackTrace();

        }

    }

    @Override
    public boolean isConnected() {

        try {

            return connection != null &&
                    !connection.isClosed();

        }

        catch (SQLException ignored) {

            return false;

        }

    }

    @Override
    public Connection getConnection() {

        if (!isConnected()) {
            throw new IllegalStateException(
                    "No existe una conexión activa.");
        }

        return connection;

    }

}