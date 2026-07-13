package com.sack.rpgroll.database;

import com.sack.rpgroll.RPGRoll;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MigrationRegistry {

    private static final Pattern PATTERN = Pattern.compile("^V(\\d+)__.*\\.sql$");

    private final RPGRoll plugin;

    public MigrationRegistry(RPGRoll plugin) {
        this.plugin = plugin;
    }

    public List<Migration> load() {

        List<Migration> migrations = new ArrayList<>();

        Path folder = plugin.getDataFolder()
                .toPath()
                .resolve("database")
                .resolve("migrations");

        if (!Files.exists(folder)) {

            plugin.getLogger().warning("No existe la carpeta de migraciones.");

            return migrations;

        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.sql")) {

            for (Path path : stream) {

                String filename = path.getFileName().toString();

                Matcher matcher = PATTERN.matcher(filename);

                if (!matcher.matches()) {
                    continue;
                }

                int version = Integer.parseInt(matcher.group(1));

                migrations.add(new Migration(
                        version,
                        filename,
                        path));

            }

        } catch (IOException exception) {

            exception.printStackTrace();

        }

        migrations.sort(Comparator.naturalOrder());

        plugin.getLogger().info(
                "✔ " + migrations.size() + " migraciones encontradas.");

        return migrations;

    }

}