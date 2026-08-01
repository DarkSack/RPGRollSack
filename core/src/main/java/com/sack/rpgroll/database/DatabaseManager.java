package com.sack.rpgroll.database;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.ConfigManager;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * DatabaseManager es el coordinador central de la capa de persistencia.
 * 
 * Responsabilidades:
 * - Leer configuración de database.yml
 * - Seleccionar el proveedor de BD (SQLite, MySQL, PostgreSQL)
 * - Coordinar la inicialización y cierre de conexiones
 * - NO contiene lógica de SQL
 * - NO conoce detalles de migraciones
 * 
 * Patrón: Service Locator
 */
public class DatabaseManager {

    private final RPGRoll plugin;
    private DatabaseProvider provider;
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

            // 3. Conectar a la base de datos
            if (provider != null) {
                provider.connect();
            }

            // 4. Ejecutar migraciones
            if (isConnected()) {
                runMigrations();
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

        databaseConfig = configManager.getConfig("config/database.yml");

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

            case "postgresql" -> {
                plugin.getLogger().severe("PostgreSQL aún no está implementado");
                provider = null;
            }

            default -> throw new Exception("Tipo de BD desconocido: " + databaseType);

        }

    }

    /**
     * Ejecuta las migraciones pendientes
     */
    private void runMigrations() {

        try {

            plugin.getLogger().info("");
            plugin.getLogger().info("========== Database Migrations ==========");

            DatabaseMigrator migrator = new DatabaseMigrator(plugin, provider.getConnection());
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
