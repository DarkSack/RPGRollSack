package com.sack.rpgroll.database;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.player.jobs.ExplorerProgress;
import com.sack.rpgroll.gameplay.job.ExplorerProgressStorage;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.identity.PlayerIdentity;
import com.sack.rpgroll.player.jobs.PlayerJobs;
import com.sack.rpgroll.player.progression.PlayerProgression;
import com.sack.rpgroll.player.repository.PlayerRepository;
import com.sack.rpgroll.player.skills.PlayerSkills;
import com.sack.rpgroll.player.stats.PlayerStats;
import com.sack.rpgroll.player.traits.PlayerTraits;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * La misma persistencia de jugadores con SQLite (lo de siempre, para quien compra RPGRoll) y con
 * PostgreSQL (una network), contra un PostgreSQL 18 de verdad.
 */
class DatabaseProvidersTest {

    private static final Logger LOGGER = Logger.getLogger("RPGRoll-test");

    private static EmbeddedPostgres postgres;

    @BeforeAll
    static void start() throws Exception {
        postgres = EmbeddedPostgres.builder().start();
    }

    @AfterAll
    static void stop() throws Exception {
        postgres.close();
    }

    private static RPGRoll plugin(File dataFolder) {
        RPGRoll plugin = mock(RPGRoll.class);
        when(plugin.getLogger()).thenReturn(LOGGER);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        when(plugin.getResource(anyString())).thenAnswer(invocation ->
                DatabaseProvidersTest.class.getClassLoader().getResourceAsStream(invocation.getArgument(0)));
        return plugin;
    }

    /** Un esquema nuevo por test, como el {@code rpg} de la network. */
    private static PostgreSQLProvider postgres(String schema) throws SQLException {
        try (Connection admin = postgres.getPostgresDatabase().getConnection();
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
        return new PostgreSQLProvider(LOGGER, new PostgreSQLProvider.Settings("localhost", postgres.getPort(),
                "postgres", schema, "postgres", "postgres", false, "RPGRoll test"));
    }

    private static RPGPlayer newPlayer(String name) {
        PlayerStats stats = PlayerStats.createDefault();
        PlayerProgression progression = PlayerProgression.createNew();
        return RPGPlayer.from(PlayerIdentity.create(UUID.randomUUID(), name), stats, progression,
                new PlayerSkills(java.util.Map.of()), new PlayerTraits(Set.of()),
                CombatStats.create(stats.getConstitutionModifier(), stats.getIntelligenceModifier(),
                        stats.getDexterityModifier(), progression.level()),
                new PlayerJobs(new java.util.LinkedHashMap<>()));
    }

    /** Guardar, cambiar y volver a cargar: lo mismo en los dos motores. */
    private static void playerRoundTrip(RPGRoll plugin, DatabaseManager manager) {
        PlayerRepository repository = new PlayerRepository(plugin, manager);
        RPGPlayer player = newPlayer("Alex");
        assertFalse(repository.exists(player.getUUID()));
        assertTrue(repository.save(player));
        assertTrue(repository.exists(player.getUUID()));

        RPGPlayer changed = player.addExperience(250).learnSkill("golpe_certero").acquireTrait("valiente")
                .joinJob("minero");
        assertTrue(repository.update(changed));

        RPGPlayer loaded = repository.findByUUID(player.getUUID()).orElseThrow();
        assertEquals("Alex", loaded.getUsername());
        assertEquals(changed.getExperience(), loaded.getExperience());
        assertEquals(Set.of("golpe_certero"), loaded.getSkills().getLearnedSkillIds());
        assertEquals(Set.of("valiente"), loaded.getTraits().getTraitIds());
        assertTrue(loaded.getJobs().getActiveJobIds().contains("minero"));

        ExplorerProgressStorage explorer = new ExplorerProgressStorage(plugin, manager);
        explorer.markBiomeVisited(player.getUUID(), "minecraft:plains");
        explorer.markBiomeVisited(player.getUUID(), "minecraft:plains");
        explorer.markBiomeVisited(player.getUUID(), "minecraft:desert");
        explorer.saveDistance(player.getUUID(), 120.5);
        explorer.saveDistance(player.getUUID(), 300.0);
        ExplorerProgress progress = explorer.load(player.getUUID());
        assertEquals(2, progress.visitedBiomes().size(), "el mismo bioma dos veces cuenta una");
        assertEquals(300.0, progress.distanceSinceLastPayout(), 0.001, "la distancia se sobrescribe");
    }

    private static boolean hasTable(Connection connection, String table) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, table, null)) {
            return tables.next();
        }
    }

    @Test
    void sqliteSigueComoSiempre(@TempDir Path folder) throws Exception {
        RPGRoll plugin = plugin(folder.toFile());
        DatabaseManager manager = new DatabaseManager(plugin);
        manager.start(new SQLiteProvider(plugin), null);
        try {
            assertEquals(DatabaseDialect.SQLITE, manager.getDialect());
            playerRoundTrip(plugin, manager);
            assertTrue(manager.<Boolean>withLocalConnection(c -> hasTable(c, "placed_blocks")),
                    "con SQLite la base local es la misma");
            assertTrue(manager.<Boolean>withConnection(c -> hasTable(c, "players")));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void postgresqlGuardaLosJugadoresYElMundoSeQuedaEnLocal(@TempDir Path folder) throws Exception {
        RPGRoll plugin = plugin(folder.toFile());
        DatabaseManager manager = new DatabaseManager(plugin);
        manager.start(postgres("rpg_uno"), new SQLiteProvider(plugin));
        try {
            assertEquals(DatabaseDialect.POSTGRESQL, manager.getDialect());
            playerRoundTrip(plugin, manager);

            assertTrue(manager.<Boolean>withConnection(c -> hasTable(c, "players")));
            assertFalse(manager.<Boolean>withConnection(c -> hasTable(c, "placed_blocks")),
                    "placed_blocks no va a la base compartida");
            assertTrue(manager.<Boolean>withLocalConnection(c -> hasTable(c, "placed_blocks")));
            assertFalse(manager.<Boolean>withLocalConnection(c -> hasTable(c, "players")),
                    "los jugadores no se duplican en el SQLite local");

            int versions = manager.withConnection(c -> {
                try (Statement statement = c.createStatement();
                     ResultSet result = statement.executeQuery("SELECT count(*) FROM schema_version")) {
                    result.next();
                    return result.getInt(1);
                }
            });
            assertEquals(7, versions, "V1-V5, V7 y V9");
        } finally {
            manager.shutdown();
        }

        // Arrancar otra vez no repite migraciones ni pierde nada.
        DatabaseManager again = new DatabaseManager(plugin);
        again.start(new PostgreSQLProvider(LOGGER, new PostgreSQLProvider.Settings("localhost", postgres.getPort(),
                "postgres", "rpg_uno", "postgres", "postgres", false, "RPGRoll test")), new SQLiteProvider(plugin));
        try {
            int players = again.withConnection(c -> {
                try (Statement statement = c.createStatement();
                     ResultSet result = statement.executeQuery("SELECT count(*) FROM players")) {
                    result.next();
                    return result.getInt(1);
                }
            });
            assertEquals(1, players);
        } finally {
            again.shutdown();
        }
    }

    @Test
    void unaTransaccionQueFallaNoDejaNadaAMedias(@TempDir Path folder) throws Exception {
        RPGRoll plugin = plugin(folder.toFile());
        DatabaseManager manager = new DatabaseManager(plugin);
        manager.start(postgres("rpg_dos"), new SQLiteProvider(plugin));
        try {
            PlayerRepository repository = new PlayerRepository(plugin, manager);
            RPGPlayer player = newPlayer("Steve").learnSkill("escudo");
            assertTrue(repository.save(player));

            assertThrows(SQLException.class, () -> manager.inTransaction(c -> {
                try (Statement statement = c.createStatement()) {
                    statement.executeUpdate("DELETE FROM player_skills");
                    statement.executeUpdate("INSERT INTO tabla_que_no_existe VALUES (1)");
                }
                return null;
            }));

            RPGPlayer loaded = repository.findByUUID(player.getUUID()).orElseThrow();
            assertEquals(Set.of("escudo"), loaded.getSkills().getLearnedSkillIds(),
                    "el borrado se deshizo con el rollback");
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void siPostgresqlCortaLaConexionSeRehace(@TempDir Path folder) throws Exception {
        RPGRoll plugin = plugin(folder.toFile());
        DatabaseManager manager = new DatabaseManager(plugin);
        manager.start(postgres("rpg_tres"), new SQLiteProvider(plugin));
        try {
            PlayerRepository repository = new PlayerRepository(plugin, manager);
            RPGPlayer player = newPlayer("Kai");
            assertTrue(repository.save(player));

            // Lo que pasa al reiniciar PostgreSQL: el servidor corta la sesión de RPGRoll.
            try (Connection admin = postgres.getPostgresDatabase().getConnection();
                 Statement statement = admin.createStatement()) {
                statement.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity "
                        + "WHERE application_name = 'RPGRoll test'");
            }
            Thread.sleep(5_200);

            assertTrue(repository.exists(player.getUUID()), "tras reconectar, sigue leyendo");
        } finally {
            manager.shutdown();
        }
    }
}
