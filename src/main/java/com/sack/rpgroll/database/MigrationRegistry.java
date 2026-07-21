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
 * resources/database/migrations/
 */
public class MigrationRegistry {

    private final RPGRoll plugin;

    private final List<Migration> migrations = new ArrayList<>();

    public MigrationRegistry(RPGRoll plugin) {

        this.plugin = plugin;

        registerMigrations();

    }

    private void registerMigrations() {

        register(1, "V1__create_players.sql");
        register(2, "V2__create_player_stats.sql");
        register(3, "V3__create_player_skills.sql");
        register(4, "V4__create_player_traits.sql");
        register(5, "V5__create_player_jobs.sql");
        register(6, "V6__create_player_quests.sql");

    }

    private void register(int version, String filename) {

        migrations.add(new Migration(
                version,
                filename,
                "database/migrations/" + filename));

    }

    public List<Migration> load() {

        Collections.sort(migrations);

        plugin.getLogger().info(
                "✔ " + migrations.size() + " migraciones registradas.");

        return List.copyOf(migrations);

    }

}