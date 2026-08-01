package com.sack.rpgroll.player.repository;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.database.DatabaseManager;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.identity.PlayerIdentity;
import com.sack.rpgroll.player.jobs.JobProgress;
import com.sack.rpgroll.player.jobs.PlayerJobs;
import com.sack.rpgroll.player.progression.PlayerProgression;
import com.sack.rpgroll.player.skills.PlayerSkills;
import com.sack.rpgroll.player.stats.PlayerStats;
import com.sack.rpgroll.player.traits.PlayerTraits;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * PlayerRepository maneja la persistencia de jugadores en la base de datos.
 * 
 * Responsabilidades:
 * - Cargar jugador desde BD
 * - Guardar jugador en BD
 * - Actualizar datos de jugador
 * - NO conoce caché
 * - NO valida datos (eso lo hace la entidad)
 */
public class PlayerRepository {

    private final RPGRoll plugin;
    private final DatabaseManager databaseManager;

    public PlayerRepository(RPGRoll plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /**
     * Carga un jugador completo desde la BD.
     * 
     * @param uuid UUID del jugador
     * @return Optional con el jugador, o vacío si no existe
     */
    public Optional<RPGPlayer> findByUUID(UUID uuid) {

        try {

            Connection connection = databaseManager.getConnection();

            // Cargar datos del jugador
            String playerQuery = "SELECT * FROM players WHERE uuid = ?";

            try (PreparedStatement statement = connection.prepareStatement(playerQuery)) {

                statement.setString(1, uuid.toString());

                try (ResultSet playerResult = statement.executeQuery()) {

                    if (!playerResult.next()) {
                        return Optional.empty();
                    }

                    // Cargar identidad
                    PlayerIdentity identity = new PlayerIdentity(
                            UUID.fromString(playerResult.getString("uuid")),
                            playerResult.getString("username"),
                            playerResult.getString("race"),
                            playerResult.getString("class"));

                    // Cargar progresión
                    PlayerProgression progression = new PlayerProgression(
                            playerResult.getInt("level"),
                            playerResult.getInt("experience"),
                            playerResult.getLong("created_at"),
                            playerResult.getLong("last_login"),
                            playerResult.getInt("unspent_stat_points"));

                    // Cargar estadísticas
                    String statsQuery = "SELECT * FROM player_stats WHERE uuid = ?";

                    PlayerStats stats = PlayerStats.createDefault();
                    CombatStats persistedCombatStats = null;

                    try (PreparedStatement statsStatement = connection.prepareStatement(statsQuery)) {

                        statsStatement.setString(1, uuid.toString());

                        try (ResultSet statsResult = statsStatement.executeQuery()) {

                            if (statsResult.next()) {
                                stats = new PlayerStats(
                                        statsResult.getInt("strength"),
                                        statsResult.getInt("dexterity"),
                                        statsResult.getInt("constitution"),
                                        statsResult.getInt("intelligence"),
                                        statsResult.getInt("wisdom"),
                                        statsResult.getInt("charisma"));

                                persistedCombatStats = CombatStats.of(
                                        statsResult.getInt("max_health"),
                                        statsResult.getInt("current_health"),
                                        statsResult.getInt("max_mana"),
                                        statsResult.getInt("current_mana"),
                                        stats.getDexterityModifier(),
                                        progression.level());
                            }

                        }

                    }

                    // Cargar skills
                    String skillsQuery = "SELECT skill_id, skill_level FROM player_skills WHERE uuid = ?";
                    Map<String, Integer> learnedSkills = new HashMap<>();

                    try (PreparedStatement skillsStatement = connection.prepareStatement(skillsQuery)) {

                        skillsStatement.setString(1, uuid.toString());

                        try (ResultSet skillsResult = skillsStatement.executeQuery()) {

                            while (skillsResult.next()) {
                                learnedSkills.put(
                                        skillsResult.getString("skill_id"),
                                        skillsResult.getInt("skill_level"));
                            }

                        }

                    }

                    PlayerSkills skills = new PlayerSkills(learnedSkills);

                    // Cargar traits
                    String traitsQuery = "SELECT trait_id FROM player_traits WHERE uuid = ?";
                    Set<String> acquiredTraits = new HashSet<>();

                    try (PreparedStatement traitsStatement = connection.prepareStatement(traitsQuery)) {

                        traitsStatement.setString(1, uuid.toString());

                        try (ResultSet traitsResult = traitsStatement.executeQuery()) {

                            while (traitsResult.next()) {
                                acquiredTraits.add(traitsResult.getString("trait_id"));
                            }

                        }

                    }

                    PlayerTraits traits = new PlayerTraits(acquiredTraits);

                    // Cargar trabajos
                    String jobsQuery = "SELECT job_id, level, experience FROM player_jobs WHERE uuid = ?";
                    Map<String, JobProgress> activeJobs = new LinkedHashMap<>();

                    try (PreparedStatement jobsStatement = connection.prepareStatement(jobsQuery)) {

                        jobsStatement.setString(1, uuid.toString());

                        try (ResultSet jobsResult = jobsStatement.executeQuery()) {

                            while (jobsResult.next()) {
                                String jobId = jobsResult.getString("job_id");
                                activeJobs.put(jobId, new JobProgress(
                                        jobId,
                                        jobsResult.getInt("level"),
                                        jobsResult.getInt("experience")));
                            }

                        }

                    }

                    PlayerJobs jobs = new PlayerJobs(activeJobs);

                    // Combat stats: se cargan tal cual de la BD (para no perder el
                    // progreso acumulado de salud/maná) o se derivan de las
                    // estadísticas base si el jugador no tenía fila en player_stats.
                    CombatStats combatStats = persistedCombatStats != null
                            ? persistedCombatStats
                            : CombatStats.create(
                                    stats.getConstitutionModifier(),
                                    stats.getIntelligenceModifier(),
                                    stats.getDexterityModifier(),
                                    progression.level());

                    // Construir jugador completo
                    RPGPlayer player = RPGPlayer.from(identity, stats, progression, skills, traits, combatStats,
                            jobs);
                    return Optional.of(player);

                }

            }

        } catch (SQLException exception) {

            plugin.getLogger().severe("Error al cargar jugador " + uuid + ": " + exception.getMessage());
            return Optional.empty();

        }

    }

    /**
     * Guarda un jugador nuevo en la BD.
     * 
     * @param player el jugador a guardar
     * @return true si se guardó exitosamente
     */
    public boolean save(RPGPlayer player) {

        try {

            Connection connection = databaseManager.getConnection();

            // Insertar en tabla players
            String insertPlayer = "INSERT INTO players (uuid, username, race, class, level, experience, created_at, last_login, unspent_stat_points) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(insertPlayer)) {

                statement.setString(1, player.getUUID().toString());
                statement.setString(2, player.getUsername());
                statement.setString(3, player.getRace());
                statement.setString(4, player.getPlayerClass());
                statement.setInt(5, player.getLevel());
                statement.setInt(6, player.getExperience());
                statement.setLong(7, player.getProgression().createdAt());
                statement.setLong(8, player.getProgression().lastLogin());
                statement.setInt(9, player.getUnspentStatPoints());

                statement.executeUpdate();

            }

            // Insertar en tabla player_stats
            String insertStats = "INSERT INTO player_stats (uuid, strength, dexterity, constitution, intelligence, wisdom, charisma, "
                    +
                    "max_health, current_health, max_mana, current_mana) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(insertStats)) {

                CombatStats combatStats = player.getCombatStats();

                statement.setString(1, player.getUUID().toString());
                statement.setInt(2, player.getStats().strength());
                statement.setInt(3, player.getStats().dexterity());
                statement.setInt(4, player.getStats().constitution());
                statement.setInt(5, player.getStats().intelligence());
                statement.setInt(6, player.getStats().wisdom());
                statement.setInt(7, player.getStats().charisma());
                statement.setInt(8, combatStats.maxHealth());
                statement.setInt(9, combatStats.currentHealth());
                statement.setInt(10, combatStats.maxMana());
                statement.setInt(11, combatStats.currentMana());

                statement.executeUpdate();

            }

            plugin.getLogger().info("✔ Jugador guardado: " + player.getUsername());

            // Guardar skills
            saveSkills(connection, player);

            // Guardar traits
            saveTraits(connection, player);

            // Guardar trabajos
            saveJobs(connection, player);

            return true;

        } catch (SQLException exception) {

            plugin.getLogger().severe("Error al guardar jugador " + player.getUUID() + ": " + exception.getMessage());
            return false;

        }

    }

    /**
     * Actualiza un jugador existente en la BD.
     * 
     * @param player el jugador con datos actualizados
     * @return true si se actualizó exitosamente
     */
    public boolean update(RPGPlayer player) {

        try {

            Connection connection = databaseManager.getConnection();

            // Actualizar tabla players
            String updatePlayer = "UPDATE players SET race = ?, class = ?, level = ?, experience = ?, last_login = ?, "
                    +
                    "unspent_stat_points = ? WHERE uuid = ?";

            try (PreparedStatement statement = connection.prepareStatement(updatePlayer)) {

                statement.setString(1, player.getRace());
                statement.setString(2, player.getPlayerClass());
                statement.setInt(3, player.getLevel());
                statement.setInt(4, player.getExperience());
                statement.setLong(5, System.currentTimeMillis());
                statement.setInt(6, player.getUnspentStatPoints());
                statement.setString(7, player.getUUID().toString());

                statement.executeUpdate();

            }

            // Actualizar tabla player_stats
            String updateStats = "UPDATE player_stats SET strength = ?, dexterity = ?, constitution = ?, " +
                    "intelligence = ?, wisdom = ?, charisma = ?, max_health = ?, current_health = ?, " +
                    "max_mana = ?, current_mana = ? WHERE uuid = ?";

            try (PreparedStatement statement = connection.prepareStatement(updateStats)) {

                CombatStats combatStats = player.getCombatStats();

                statement.setInt(1, player.getStats().strength());
                statement.setInt(2, player.getStats().dexterity());
                statement.setInt(3, player.getStats().constitution());
                statement.setInt(4, player.getStats().intelligence());
                statement.setInt(5, player.getStats().wisdom());
                statement.setInt(6, player.getStats().charisma());
                statement.setInt(7, combatStats.maxHealth());
                statement.setInt(8, combatStats.currentHealth());
                statement.setInt(9, combatStats.maxMana());
                statement.setInt(10, combatStats.currentMana());
                statement.setString(11, player.getUUID().toString());

                statement.executeUpdate();

            }

            // Actualizar skills
            saveSkills(connection, player);

            // Actualizar traits
            saveTraits(connection, player);

            // Actualizar trabajos
            saveJobs(connection, player);

            return true;

        } catch (SQLException exception) {

            plugin.getLogger()
                    .severe("Error al actualizar jugador " + player.getUUID() + ": " + exception.getMessage());
            return false;

        }

    }

    /**
     * Verifica si un jugador existe en la BD.
     */
    public boolean exists(UUID uuid) {

        try {

            Connection connection = databaseManager.getConnection();
            String query = "SELECT 1 FROM players WHERE uuid = ? LIMIT 1";

            try (PreparedStatement statement = connection.prepareStatement(query)) {

                statement.setString(1, uuid.toString());

                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }

            }

        } catch (SQLException exception) {

            plugin.getLogger().severe("Error al verificar existencia de jugador: " + exception.getMessage());
            return false;

        }

    }

    /**
     * Guarda o actualiza las skills del jugador.
     */
    private void saveSkills(Connection connection, RPGPlayer player) throws SQLException {

        UUID uuid = player.getUUID();
        PlayerSkills skills = player.getSkills();

        // Limpiar skills existentes
        String deleteSkills = "DELETE FROM player_skills WHERE uuid = ?";

        try (PreparedStatement statement = connection.prepareStatement(deleteSkills)) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        }

        // Insertar nuevas skills
        String insertSkill = "INSERT INTO player_skills (uuid, skill_id, skill_level) VALUES (?, ?, ?)";

        for (String skillId : skills.getLearnedSkillIds()) {
            try (PreparedStatement statement = connection.prepareStatement(insertSkill)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, skillId);
                statement.setInt(3, skills.getSkillLevel(skillId));
                statement.executeUpdate();
            }
        }

    }

    /**
     * Guarda o actualiza los traits del jugador.
     */
    private void saveTraits(Connection connection, RPGPlayer player) throws SQLException {

        UUID uuid = player.getUUID();
        PlayerTraits traits = player.getTraits();

        // Limpiar traits existentes
        String deleteTraits = "DELETE FROM player_traits WHERE uuid = ?";

        try (PreparedStatement statement = connection.prepareStatement(deleteTraits)) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        }

        // Insertar nuevos traits
        String insertTrait = "INSERT INTO player_traits (uuid, trait_id) VALUES (?, ?)";

        for (String traitId : traits.getTraitIds()) {
            try (PreparedStatement statement = connection.prepareStatement(insertTrait)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, traitId);
                statement.executeUpdate();
            }
        }

    }

    /**
     * Guarda o actualiza los trabajos activos del jugador (máximo 3),
     * incluyendo su nivel y experiencia actuales.
     */
    private void saveJobs(Connection connection, RPGPlayer player) throws SQLException {

        UUID uuid = player.getUUID();
        PlayerJobs jobs = player.getJobs();

        // Limpiar trabajos existentes
        String deleteJobs = "DELETE FROM player_jobs WHERE uuid = ?";

        try (PreparedStatement statement = connection.prepareStatement(deleteJobs)) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        }

        // Insertar trabajos activos
        String insertJob = "INSERT INTO player_jobs (uuid, job_id, level, experience) VALUES (?, ?, ?, ?)";

        for (String jobId : jobs.getActiveJobIds()) {

            JobProgress progress = jobs.getProgress(jobId).orElseThrow();

            try (PreparedStatement statement = connection.prepareStatement(insertJob)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, jobId);
                statement.setInt(3, progress.level());
                statement.setInt(4, progress.experience());
                statement.executeUpdate();
            }
        }

    }

}