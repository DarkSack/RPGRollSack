package com.sack.rpgroll.gameplay.job;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.database.DatabaseManager;
import com.sack.rpgroll.player.jobs.ExplorerProgress;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Persiste el progreso de exploración (biomas visitados, distancia
 * acumulada) por separado de PlayerRepository, ya que es estado propio
 * del trabajo Explorador y no de RPGPlayer en general.
 */
public class ExplorerProgressStorage {

    private final RPGRoll plugin;
    private final DatabaseManager databaseManager;

    public ExplorerProgressStorage(RPGRoll plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public ExplorerProgress load(UUID uuid) {

        Set<String> biomes = new HashSet<>();
        double distance = 0.0;

        try {
            distance = databaseManager.withConnection(connection -> {

                String biomesQuery = "SELECT biome FROM explorer_biomes WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(biomesQuery)) {
                    statement.setString(1, uuid.toString());
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            biomes.add(result.getString("biome"));
                        }
                    }
                }

                String distanceQuery = "SELECT distance_since_payout FROM explorer_distance WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(distanceQuery)) {
                    statement.setString(1, uuid.toString());
                    try (ResultSet result = statement.executeQuery()) {
                        return result.next() ? result.getDouble("distance_since_payout") : 0.0;
                    }
                }

            });
        } catch (SQLException exception) {
            plugin.getLogger().warning("✘ Error al cargar progreso de exploración: " + exception.getMessage());
        }

        return new ExplorerProgress(biomes, distance);
    }

    public void markBiomeVisited(UUID uuid, String biome) {

        // ON CONFLICT vale en SQLite y en PostgreSQL (INSERT OR IGNORE solo en SQLite).
        String sql = "INSERT INTO explorer_biomes (uuid, biome) VALUES (?, ?) ON CONFLICT (uuid, biome) DO NOTHING";

        try {
            databaseManager.withConnection(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, uuid.toString());
                    statement.setString(2, biome);
                    return statement.executeUpdate();
                }
            });
        } catch (SQLException exception) {
            plugin.getLogger().warning("✘ Error al guardar bioma visitado: " + exception.getMessage());
        }
    }

    public void saveDistance(UUID uuid, double distance) {

        String sql = "INSERT INTO explorer_distance (uuid, distance_since_payout) VALUES (?, ?) "
                + "ON CONFLICT (uuid) DO UPDATE SET distance_since_payout = excluded.distance_since_payout";

        try {
            databaseManager.withConnection(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, uuid.toString());
                    statement.setDouble(2, distance);
                    return statement.executeUpdate();
                }
            });
        } catch (SQLException exception) {
            plugin.getLogger().warning("✘ Error al guardar distancia de exploración: " + exception.getMessage());
        }
    }

}