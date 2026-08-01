package com.sack.rpgroll.gameplay.job;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.database.DatabaseManager;
import org.bukkit.block.Block;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Rastrea bloques colocados por jugadores para evitar farmear XP/dinero de
 * trabajos rompiendo y re-colocando el mismo bloque repetidamente.
 * <p>
 * Persiste en SQLite (no en memoria) para sobrevivir a reinicios del servidor.
 */
public class PlacedBlockTracker {

    private final RPGRoll plugin;
    private final DatabaseManager databaseManager;

    public PlacedBlockTracker(RPGRoll plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /**
     * Marca un bloque como colocado por un jugador.
     */
    public void markPlaced(Block block) {

        String sql = "INSERT OR REPLACE INTO placed_blocks (world, x, y, z) VALUES (?, ?, ?, ?)";

        try {
            Connection connection = databaseManager.getConnection();

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, block.getWorld().getName());
                statement.setInt(2, block.getX());
                statement.setInt(3, block.getY());
                statement.setInt(4, block.getZ());
                statement.executeUpdate();
            }

        } catch (SQLException exception) {
            plugin.getLogger().warning("✘ Error al marcar bloque colocado: " + exception.getMessage());
        }
    }

    /**
     * Verifica si un bloque fue colocado por un jugador. Si lo fue, además
     * elimina el registro (el bloque está a punto de romperse — ya no
     * existirá, así que no tiene sentido mantenerlo trackeado).
     *
     * @return true si el bloque era "artificial" (colocado por jugador)
     */
    public boolean isPlayerPlacedAndClear(Block block) {

        String selectSql = "SELECT 1 FROM placed_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?";
        String deleteSql = "DELETE FROM placed_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?";

        try {
            Connection connection = databaseManager.getConnection();

            boolean wasPlaced;

            try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
                statement.setString(1, block.getWorld().getName());
                statement.setInt(2, block.getX());
                statement.setInt(3, block.getY());
                statement.setInt(4, block.getZ());

                try (ResultSet result = statement.executeQuery()) {
                    wasPlaced = result.next();
                }
            }

            if (wasPlaced) {
                try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
                    statement.setString(1, block.getWorld().getName());
                    statement.setInt(2, block.getX());
                    statement.setInt(3, block.getY());
                    statement.setInt(4, block.getZ());
                    statement.executeUpdate();
                }
            }

            return wasPlaced;

        } catch (SQLException exception) {
            plugin.getLogger().warning("✘ Error al verificar bloque colocado: " + exception.getMessage());
            // Ante un error de BD, es más seguro asumir "sí fue colocado" (no pagar)
            // que arriesgarse a permitir farmeo por un fallo de infraestructura.
            return true;
        }
    }

}