package com.sack.rpgroll.gameplay.job;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.database.DatabaseManager;
import org.bukkit.block.Block;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Rastrea bloques colocados por jugadores para evitar farmear XP/dinero de
 * trabajos (y progreso de quests y logros) rompiendo y re-colocando el mismo
 * bloque repetidamente.
 * <p>
 * Persiste en SQLite para sobrevivir a reinicios, pero las consultas van a
 * una copia en memoria: antes cada bloque puesto o roto por cualquier
 * jugador hacía una consulta a SQLite en el hilo principal (dos, desde que
 * Quests y Ascension también preguntan). La tabla nunca guarda más de
 * {@link PlacedBlockCleanupTask#RETENTION_DAYS} días de datos, así que
 * cabe en memoria. Las escrituras van a la base de datos en un hilo propio,
 * de una en una y en orden.
 */
public class PlacedBlockTracker {

    private final RPGRoll plugin;
    private final DatabaseManager databaseManager;

    /** Mundo → posición empaquetada → cuándo se colocó (segundos Unix). */
    private final Map<String, Map<Long, Long>> placed = new ConcurrentHashMap<>();

    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "RPGRoll-PlacedBlocks");
        thread.setDaemon(true);
        return thread;
    });

    public PlacedBlockTracker(RPGRoll plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /** Carga la tabla en memoria. Se llama una vez al arrancar. */
    public void load() {

        String sql = "SELECT world, x, y, z, placed_at FROM placed_blocks";
        int count = 0;

        try {
            Connection connection = databaseManager.getConnection();

            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet result = statement.executeQuery()) {

                while (result.next()) {
                    worldMap(result.getString(1))
                            .put(pack(result.getInt(2), result.getInt(3), result.getInt(4)), result.getLong(5));
                    count++;
                }
            }

        } catch (SQLException exception) {
            plugin.getLogger().warning("✘ Error al cargar bloques colocados: " + exception.getMessage());
        }

        plugin.getLogger().info("✔ " + count + " bloque(s) colocado(s) por jugadores cargados en memoria.");
    }

    /**
     * Marca un bloque como colocado por un jugador.
     */
    public void markPlaced(Block block) {

        String world = block.getWorld().getName();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        worldMap(world).put(pack(x, y, z), System.currentTimeMillis() / 1000);

        write("INSERT OR REPLACE INTO placed_blocks (world, x, y, z) VALUES (?, ?, ?, ?)", world, x, y, z,
                "marcar bloque colocado");
    }

    /**
     * Como {@link #isPlayerPlacedAndClear}, pero sin borrar la marca. Para
     * otros addons que quieren saber si un bloque roto era natural (RPGRoll-
     * Ascension no cuenta para logros los bloques que el jugador puso él
     * mismo): tienen que preguntar antes que el listener del minero, que es
     * el que la borra, a prioridad MONITOR.
     */
    public boolean isPlayerPlaced(Block block) {

        Map<Long, Long> world = placed.get(block.getWorld().getName());
        return world != null && world.containsKey(pack(block.getX(), block.getY(), block.getZ()));
    }

    /**
     * Verifica si un bloque fue colocado por un jugador. Si lo fue, además
     * elimina el registro (el bloque está a punto de romperse — ya no
     * existirá, así que no tiene sentido mantenerlo trackeado).
     *
     * @return true si el bloque era "artificial" (colocado por jugador)
     */
    public boolean isPlayerPlacedAndClear(Block block) {

        String world = block.getWorld().getName();
        Map<Long, Long> worldMap = placed.get(world);

        if (worldMap == null || worldMap.remove(pack(block.getX(), block.getY(), block.getZ())) == null) {
            return false;
        }

        write("DELETE FROM placed_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?", world, block.getX(),
                block.getY(), block.getZ(), "borrar bloque colocado");
        return true;
    }

    /** Olvida en memoria las marcas anteriores a {@code cutoffSeconds}; la limpieza ya las borró de la tabla. */
    public void forgetOlderThan(long cutoffSeconds) {
        placed.values().forEach(world -> world.values().removeIf(placedAt -> placedAt < cutoffSeconds));
    }

    /** Termina de escribir lo pendiente. Hay que llamarlo antes de cerrar la base de datos. */
    public void shutdown() {

        writer.shutdown();

        try {
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("✘ Quedaron escrituras de bloques colocados sin terminar al apagar.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void write(String sql, String world, int x, int y, int z, String what) {

        writer.execute(() -> {
            try {
                Connection connection = databaseManager.getConnection();

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, world);
                    statement.setInt(2, x);
                    statement.setInt(3, y);
                    statement.setInt(4, z);
                    statement.executeUpdate();
                }

            } catch (SQLException exception) {
                plugin.getLogger().warning("✘ Error al " + what + ": " + exception.getMessage());
            }
        });
    }

    private Map<Long, Long> worldMap(String world) {
        return placed.computeIfAbsent(world, ignored -> new ConcurrentHashMap<>());
    }

    /** x y z en un long: 26 bits para x y z (±33 millones), 12 para y (-2048..2047). */
    static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

}
