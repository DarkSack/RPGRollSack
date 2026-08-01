package com.sack.rpgroll.gameplay.job;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.database.DatabaseManager;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Tarea periódica que limpia placed_blocks huérfanos — registros de
 * bloques colocados por jugadores hace más de RETENTION_DAYS que nunca
 * se rompieron por BlockBreakEvent (probablemente destruidos por otra vía,
 * como explosiones, que no dispara ese evento).
 * <p>
 * placed_blocks no tiene columna de timestamp hoy — este método requiere
 * agregarla (ver migración V8) para poder filtrar por antigüedad.
 */
public class PlacedBlockCleanupTask extends BukkitRunnable {

    private static final long PERIOD_TICKS = 20L * 60 * 60 * 6; // cada 6 horas
    private static final int RETENTION_DAYS = 7;

    private final RPGRoll plugin;
    private final DatabaseManager databaseManager;

    public PlacedBlockCleanupTask(RPGRoll plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @Override
    public void run() {

        String sql = "DELETE FROM placed_blocks WHERE placed_at < (unixepoch('now') - " + (RETENTION_DAYS * 86400)
                + ")";

        try {
            Connection connection = databaseManager.getConnection();

            try (Statement statement = connection.createStatement()) {
                int deleted = statement.executeUpdate(sql);

                if (deleted > 0) {
                    plugin.getLogger()
                            .info("✔ Limpieza: " + deleted + " bloques huérfanos eliminados de placed_blocks.");
                }
            }

        } catch (SQLException exception) {
            plugin.getLogger().warning("✘ Error en limpieza de placed_blocks: " + exception.getMessage());
        }
    }

    public void start() {
        runTaskTimerAsynchronously(plugin, PERIOD_TICKS, PERIOD_TICKS);
    }

}