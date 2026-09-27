package com.sack.rpgroll.database;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.ConfigManager;
import org.bukkit.configuration.file.YamlConfiguration;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * DatabaseManager es el coordinador central de la capa de persistencia.
 * 
 * Responsabilidades:
 * - Leer configuración de database.yml
 * - Seleccionar el proveedor de BD (SQLite o PostgreSQL)
 * - Coordinar la inicialización y cierre de conexiones
 * - Serializar el acceso a la conexión compartida y dar transacciones
 * - NO contiene lógica de SQL
 * - NO conoce detalles de migraciones
 *
 * Con PostgreSQL hay dos bases: la compartida (jugadores, para una network) y un
 * SQLite local para lo que es de este servidor y su mundo (placed_blocks), que se
 * usa con {@link #withLocalConnection}. Con SQLite las dos son la misma.
 * 
 * Patrón: Service Locator
 */
public class DatabaseManager {

    /** Un trabajo con la conexión; puede lanzar SQLException. */
    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }

    private final RPGRoll plugin;
    private DatabaseProvider provider;
    private DatabaseProvider localProvider;
    private YamlConfiguration databaseConfig;

    public DatabaseManager(RPGRoll plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicializa el DatabaseManager.
     * 
     * Flujo:
     * 1. Cargar database.yml
     * 2. Seleccionar provider según configuración
     * 3. Conectar a la base de datos
     * 4. Ejecutar migraciones
     */
    public void initialize() {

        plugin.getLogger().info("");
        plugin.getLogger().info("========== Database Manager ==========");

        try {

            // 1. Cargar configuración de base de datos
            loadConfiguration();

            // 2. Seleccionar e instanciar el proveedor
            selectProvider();

            // 3 y 4. Conectar y ejecutar migraciones
            if (provider != null) {
                start(provider, localProvider);
            }

            plugin.getLogger().info("✔ Database Manager inicializado correctamente.");
            plugin.getLogger().info("=====================================");
            plugin.getLogger().info("");

        } catch (Exception exception) {

            plugin.getLogger().severe("Error al inicializar Database Manager:");
            exception.printStackTrace();

        }

    }

    /**
     * Carga el archivo database.yml
     */
    private void loadConfiguration() throws Exception {

        ConfigManager configManager = plugin.getBootstrap()
                .getServices()
                .get(ConfigManager.class);

        databaseConfig = configManager.getConfig("database.yml");

        if (databaseConfig == null) {
            throw new Exception("No se pudo cargar database.yml");
        }

        plugin.getLogger().info("✔ Configuración de BD cargada");

    }

    /**
     * Selecciona el proveedor de BD según database.yml
     */
    private void selectProvider() throws Exception {

        String databaseType = databaseConfig.getString("database.type", "sqlite").toLowerCase();

        plugin.getLogger().info("Tipo de BD: " + databaseType);

        switch (databaseType) {

            case "sqlite" -> {
                provider = new SQLiteProvider(plugin);
                plugin.getLogger().info("✔ Provider: SQLite");
            }

            case "mysql" -> {
                plugin.getLogger().severe("MySQL aún no está implementado");
                provider = null;
            }

            case "postgresql", "postgres" -> {
                provider = new PostgreSQLProvider(plugin.getLogger(), PostgreSQLProvider.Settings.from(
                        databaseConfig.getConfigurationSection("database.postgresql"),
                        "RPGRoll " + plugin.getServer().getPort()));
                localProvider = new SQLiteProvider(plugin);
                plugin.getLogger().info("✔ Provider: PostgreSQL (placed_blocks en SQLite local)");
            }

            default -> throw new Exception("Tipo de BD desconocido: " + databaseType);

        }

    }

    /**
     * Conecta los proveedores y ejecuta sus migraciones. {@code local} es null con SQLite
     * (la local es la misma). Público para los tests, que lo llaman con sus proveedores.
     */
    public void start(DatabaseProvider main, DatabaseProvider local) {

        this.provider = main;
        this.localProvider = local;

        provider.connect();

        if (localProvider != null) {
            localProvider.connect();
        }

        if (isConnected()) {
            if (localProvider == null) {
                runMigrations(provider, MigrationRegistry.Set.SQLITE_ALL);
            } else {
                runMigrations(provider, MigrationRegistry.Set.POSTGRESQL);
                if (localProvider.isConnected()) {
                    runMigrations(localProvider, MigrationRegistry.Set.SQLITE_LOCAL);
                }
            }
        }

    }

    /**
     * Ejecuta las migraciones pendientes
     */
    private void runMigrations(DatabaseProvider target, MigrationRegistry.Set set) {

        try {

            plugin.getLogger().info("");
            plugin.getLogger().info("========== Database Migrations ==========");

            DatabaseMigrator migrator = new DatabaseMigrator(plugin, target.getConnection(), set);
            migrator.migrate();

            plugin.getLogger().info("✔ Migraciones completadas");
            plugin.getLogger().info("=========================================");
            plugin.getLogger().info("");

        } catch (Exception exception) {

            plugin.getLogger().severe("Error durante migraciones:");
            exception.printStackTrace();

        }

    }

    /**
     * Cierra la conexión con la BD
     */
    public void shutdown() {

        if (provider != null && provider.isConnected()) {
            provider.disconnect();
        }

        if (localProvider != null && localProvider.isConnected()) {
            localProvider.disconnect();
        }

    }

    /** La base de este servidor (placed_blocks): el SQLite local, o la principal si es SQLite. */
    private DatabaseProvider local() {
        return localProvider != null ? localProvider : provider;
    }

    /**
     * Trabajo con la conexión compartida, sin que otro hilo la use a la vez (dentro de una
     * transacción de otro hilo acabaría escribiendo en ella).
     */
    public <T> T withConnection(SqlWork<T> work) throws SQLException {
        synchronized (provider) {
            return work.run(getConnection());
        }
    }

    /** Como {@link #withConnection}, pero en una transacción: todo o nada. */
    public <T> T inTransaction(SqlWork<T> work) throws SQLException {

        synchronized (provider) {

            Connection connection = getConnection();
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            } finally {
                try {
                    connection.setAutoCommit(previousAutoCommit);
                } catch (SQLException ignored) {
                    // La conexión murió: el proveedor la rehace en el próximo uso.
                }
            }

        }

    }

    /** Trabajo con la base de este servidor (placed_blocks). */
    public <T> T withLocalConnection(SqlWork<T> work) throws SQLException {

        DatabaseProvider local = local();

        if (local == null || !local.isConnected()) {
            throw new SQLException("No hay conexión con la base local");
        }

        synchronized (local) {
            return work.run(local.getConnection());
        }

    }

    /** El motor de la base compartida. */
    public DatabaseDialect getDialect() {
        return provider == null ? DatabaseDialect.SQLITE : provider.dialect();
    }

    /**
     * Devuelve el proveedor actual
     */
    public DatabaseProvider getProvider() {
        return provider;
    }

    /**
     * Devuelve la conexión activa
     */
    public java.sql.Connection getConnection() {

        if (provider == null || !provider.isConnected()) {
            throw new IllegalStateException("No hay conexión activa con la base de datos");
        }

        return provider.getConnection();

    }

    /**
     * Verifica si está conectado
     */
    public boolean isConnected() {
        return provider != null && provider.isConnected();
    }

    /**
     * Devuelve la configuración de BD
     */
    public YamlConfiguration getDatabaseConfig() {
        return databaseConfig;
    }

}
