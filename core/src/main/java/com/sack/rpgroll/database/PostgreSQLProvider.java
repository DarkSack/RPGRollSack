package com.sack.rpgroll.database;

import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementación PostgreSQL del DatabaseProvider, para compartir el progreso entre varios
 * servidores (una network). SQLite sigue siendo el valor por defecto.
 * <p>
 * Igual que con SQLite, una sola conexión compartida: RPGRoll serializa su acceso en
 * {@link DatabaseManager}. A diferencia de SQLite, la conexión va por red y se puede cortar
 * (reinicio de PostgreSQL, red): {@link #getConnection()} la comprueba cada pocos segundos y la
 * rehace si hace falta, sin que el resto del plugin se entere.
 * <p>
 * El driver va dentro del jar, reubicado, y se instancia directamente (sin DriverManager): así no
 * choca con otro PostgreSQL que traiga otro plugin.
 */
public class PostgreSQLProvider implements DatabaseProvider {

    /** Cada cuánto se comprueba que la conexión sigue viva (una ida y vuelta a la base). */
    private static final long CHECK_EVERY_MILLIS = 5_000;

    private final Logger logger;
    private final Settings settings;

    private Connection connection;
    private long lastCheck;

    public PostgreSQLProvider(Logger logger, Settings settings) {
        this.logger = logger;
        this.settings = settings;
    }

    /** Lo que hay en {@code database.postgresql} de database.yml. */
    public record Settings(String host, int port, String database, String schema, String user, String password,
                           boolean ssl, String applicationName) {

        public static Settings from(ConfigurationSection section, String applicationName) {
            if (section == null) {
                throw new IllegalArgumentException("Falta la sección database.postgresql en database.yml");
            }
            return new Settings(
                    section.getString("host", "localhost"),
                    section.getInt("port", 5432),
                    section.getString("database", "rpgroll"),
                    section.getString("schema", "public"),
                    section.getString("user", "rpgroll"),
                    section.getString("password", ""),
                    section.getBoolean("ssl", false),
                    applicationName);
        }

        String url() {
            return "jdbc:postgresql://" + host + ":" + port + "/" + database;
        }

        @Override
        public String toString() {
            return user + "@" + host + ":" + port + "/" + database + " (esquema " + schema + ")";
        }
    }

    @Override
    public void connect() {

        if (isConnected()) {
            logger.warning("Ya existe una conexión PostgreSQL.");
            return;
        }

        try {
            connection = open();
            lastCheck = System.currentTimeMillis();
            logger.info("✔ PostgreSQL conectado: " + settings);
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "No fue posible conectar PostgreSQL (" + settings + ")", exception);
        }

    }

    private Connection open() throws SQLException {

        Properties properties = new Properties();
        properties.setProperty("user", settings.user());
        properties.setProperty("password", settings.password());
        properties.setProperty("currentSchema", settings.schema());
        properties.setProperty("ApplicationName", settings.applicationName());
        properties.setProperty("connectTimeout", "5");
        properties.setProperty("socketTimeout", "30");
        properties.setProperty("tcpKeepAlive", "true");
        properties.setProperty("sslmode", settings.ssl() ? "require" : "disable");

        Connection opened = new org.postgresql.Driver().connect(settings.url(), properties);
        if (opened == null) {
            throw new SQLException("URL no válida para PostgreSQL: " + settings.url());
        }
        return opened;

    }

    @Override
    public void disconnect() {

        if (connection == null) {
            return;
        }

        try {
            connection.close();
            logger.info("✔ PostgreSQL desconectado.");
        } catch (SQLException exception) {
            logger.log(Level.WARNING, "Error cerrando PostgreSQL", exception);
        } finally {
            connection = null;
        }

    }

    @Override
    public boolean isConnected() {

        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException ignored) {
            return false;
        }

    }

    @Override
    public synchronized Connection getConnection() {

        long now = System.currentTimeMillis();
        boolean alive;
        try {
            alive = connection != null && !connection.isClosed()
                    && (now - lastCheck < CHECK_EVERY_MILLIS || connection.isValid(2));
        } catch (SQLException ignored) {
            alive = false;
        }

        if (alive) {
            lastCheck = now;
            return connection;
        }

        // Una conexión dentro de una transacción no se rehace a escondidas: que falle esa
        // operación (hace rollback) y la siguiente ya encuentra una conexión nueva.
        logger.warning("La conexión con PostgreSQL se perdió; reconectando…");
        disconnect();
        try {
            connection = open();
            lastCheck = now;
            logger.info("✔ PostgreSQL reconectado.");
            return connection;
        } catch (SQLException exception) {
            throw new IllegalStateException("PostgreSQL no responde: " + exception.getMessage(), exception);
        }

    }

    @Override
    public DatabaseDialect dialect() {
        return DatabaseDialect.POSTGRESQL;
    }

}
