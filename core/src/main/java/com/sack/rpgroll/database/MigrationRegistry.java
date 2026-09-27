package com.sack.rpgroll.database;

import com.sack.rpgroll.RPGRoll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registro de todas las migraciones disponibles.
 *
 * Las migraciones viven dentro del JAR en:
 *
 * resources/database/migrations/            (SQLite)
 * resources/database/migrations/postgresql/ (PostgreSQL)
 *
 * Con PostgreSQL, los datos del jugador van a la base compartida y los del mundo
 * (placed_blocks: qué bloques puso un jugador en ESTE servidor) se quedan en el
 * SQLite local, porque dos servidores pueden tener un mundo con el mismo nombre.
 * Por eso hay tres conjuntos; las versiones son las mismas en todos, cada base
 * aplica las suyas.
 */
public class MigrationRegistry {

    /** Qué migraciones corresponden a una base. */
    public enum Set {

        /** Todo en SQLite: lo de siempre, sin cambios para quien no use PostgreSQL. */
        SQLITE_ALL,

        /** La base compartida de PostgreSQL: todo menos placed_blocks. */
        POSTGRESQL,

        /** El SQLite local cuando se usa PostgreSQL: solo placed_blocks. */
        SQLITE_LOCAL

    }

    /** Versiones que solo tocan placed_blocks. */
    private static final java.util.Set<Integer> LOCAL_ONLY = java.util.Set.of(6, 8);

    private final RPGRoll plugin;

    private final Set set;

    private final List<Migration> migrations = new ArrayList<>();

    public MigrationRegistry(RPGRoll plugin) {
        this(plugin, Set.SQLITE_ALL);
    }

    public MigrationRegistry(RPGRoll plugin, Set set) {

        this.plugin = plugin;
        this.set = set;

        registerMigrations();

    }

    private void registerMigrations() {

        register(1, "V1__create_players.sql");
        register(2, "V2__create_player_stats.sql");
        register(3, "V3__create_player_skills.sql");
        register(4, "V4__create_player_traits.sql");
        register(5, "V5__create_player_jobs.sql");
        register(6, "V6__create_placed_blocks.sql");
        register(7, "V7__create_explorer_progress.sql");
        register(8, "V8__add_placed_at_to_placed_blocks.sql");
        register(9, "V9__add_stat_points_and_resources.sql");

    }

    private void register(int version, String filename) {

        boolean local = LOCAL_ONLY.contains(version);

        switch (set) {

            case SQLITE_ALL -> migrations.add(new Migration(
                    version,
                    filename,
                    "database/migrations/" + filename));

            case POSTGRESQL -> {
                if (!local) {
                    migrations.add(new Migration(
                            version,
                            filename,
                            "database/migrations/postgresql/" + filename));
                }
            }

            case SQLITE_LOCAL -> {
                if (local) {
                    migrations.add(new Migration(
                            version,
                            filename,
                            "database/migrations/" + filename));
                }
            }

        }

    }

    public List<Migration> load() {

        Collections.sort(migrations);

        plugin.getLogger().info(
                "✔ " + migrations.size() + " migraciones registradas (" + set + ").");

        return List.copyOf(migrations);

    }

}
