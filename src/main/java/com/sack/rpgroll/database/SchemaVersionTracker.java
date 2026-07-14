package com.sack.rpgroll.database;

import com.sack.rpgroll.RPGRoll;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * SchemaVersionTracker mantiene el registro de migraciones aplicadas.
 * 
 * Responsabilidades:
 * - Consultar schema_version para obtener migraciones ya aplicadas
 * - Determinar migraciones pendientes
 * - Registrar nuevas migraciones en schema_version
 */
public class SchemaVersionTracker {

    private final RPGRoll plugin;
    private final Connection connection;
    private Set<Integer> appliedVersions;

    public SchemaVersionTracker(RPGRoll plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    /**
     * Inicializa el tracker.
     * 
     * Crea la tabla schema_version si no existe.
     * Carga las versiones ya aplicadas.
     */
    public void initialize() throws SQLException {

        // Crear tabla si no existe
        String createTable = "CREATE TABLE IF NOT EXISTS schema_version (" +
                "version INTEGER PRIMARY KEY, " +
                "applied_at INTEGER NOT NULL" +
                ");";

        try (PreparedStatement statement = connection.prepareStatement(createTable)) {
            statement.executeUpdate();
        }

        plugin.getLogger().info("✔ Tabla schema_version validada");

        // Cargar versiones ya aplicadas
        loadAppliedVersions();

    }

    /**
     * Carga las versiones ya aplicadas desde la BD
     */
    private void loadAppliedVersions() throws SQLException {

        appliedVersions = new HashSet<>();

        String query = "SELECT version FROM schema_version ORDER BY version ASC";

        try (PreparedStatement statement = connection.prepareStatement(query);
                ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                int version = result.getInt("version");
                appliedVersions.add(version);
            }

        }

        plugin.getLogger().info("✔ " + appliedVersions.size() + " migraciones ya aplicadas");

        // Listar versiones aplicadas
        if (!appliedVersions.isEmpty()) {
            plugin.getLogger().info("  Versiones: " + appliedVersions);
        }

    }

    /**
     * Determina cuáles migraciones están pendientes
     * 
     * @param allMigrations todas las migraciones disponibles
     * @return solo las migraciones que no han sido aplicadas
     */
    public List<Migration> getPendingMigrations(List<Migration> allMigrations) {

        List<Migration> pending = new ArrayList<>();

        for (Migration migration : allMigrations) {

            if (!appliedVersions.contains(migration.version())) {
                pending.add(migration);
            }

        }

        return pending;

    }

    /**
     * Registra una migración como aplicada
     * 
     * @param version número de versión
     */
    public void recordMigration(int version) throws SQLException {

        long currentTime = System.currentTimeMillis();

        String insert = "INSERT INTO schema_version (version, applied_at) VALUES (?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(insert)) {

            statement.setInt(1, version);
            statement.setLong(2, currentTime);

            statement.executeUpdate();

            appliedVersions.add(version);

        }

    }

    /**
     * Verifica si una versión ya fue aplicada
     */
    public boolean isApplied(int version) {
        return appliedVersions.contains(version);
    }

    /**
     * Obtiene la versión más reciente aplicada
     */
    public int getLatestVersion() {

        if (appliedVersions.isEmpty()) {
            return 0;
        }

        return Collections.max(appliedVersions);

    }

    /**
     * Obtiene todas las versiones aplicadas
     */
    public Set<Integer> getAppliedVersions() {
        return Collections.unmodifiableSet(appliedVersions);
    }

}
