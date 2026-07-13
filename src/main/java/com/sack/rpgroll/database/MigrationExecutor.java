package com.sack.rpgroll.database;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class MigrationExecutor {

    private final Connection connection;

    public MigrationExecutor(Connection connection) {
        this.connection = connection;
    }

    public void execute(Migration migration) throws IOException, SQLException {

        String sql = Files.readString(migration.path());

        try (Statement statement = connection.createStatement()) {

            statement.execute(sql);

        }

    }

}