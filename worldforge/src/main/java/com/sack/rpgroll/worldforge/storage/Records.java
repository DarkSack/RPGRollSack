package com.sack.rpgroll.worldforge.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Queries for managed worlds, profile versions and the history. All run on the database thread. */
public final class Records {

    public record ManagedWorld(String name, String profileId, String profileHash, String status, long createdAt,
                               String createdBy) {
    }

    public record HistoryEntry(long ts, String actor, String action, String target, String detail) {
    }

    private final Database db;

    public Records(Database db) {
        this.db = db;
    }

    public CompletableFuture<Void> history(String actor, String action, String target, String detail) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO history(ts, actor, action, target, detail) VALUES (?, ?, ?, ?, ?)")) {
                ps.setLong(1, System.currentTimeMillis());
                ps.setString(2, actor);
                ps.setString(3, action);
                ps.setString(4, target);
                ps.setString(5, detail);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<List<HistoryEntry>> recentHistory(int limit) {
        return db.submit(c -> {
            List<HistoryEntry> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT ts, actor, action, target, detail FROM history ORDER BY id DESC LIMIT ?")) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new HistoryEntry(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                                rs.getString(5)));
                    }
                }
            }
            return out;
        });
    }

    public CompletableFuture<Void> saveWorld(ManagedWorld w) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO managed_worlds(name, profile_id, profile_hash, status, created_at, created_by)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT(name) DO UPDATE SET profile_id = excluded.profile_id,
                      profile_hash = excluded.profile_hash, status = excluded.status""")) {
                ps.setString(1, w.name());
                ps.setString(2, w.profileId());
                ps.setString(3, w.profileHash());
                ps.setString(4, w.status());
                ps.setLong(5, w.createdAt());
                ps.setString(6, w.createdBy());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Void> setWorldStatus(String name, String status) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE managed_worlds SET status = ? WHERE name = ?")) {
                ps.setString(1, status);
                ps.setString(2, name);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<List<ManagedWorld>> worlds() {
        return db.submit(c -> {
            List<ManagedWorld> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT name, profile_id, profile_hash, status, created_at, created_by FROM managed_worlds ORDER BY name");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(world(rs));
                }
            }
            return out;
        });
    }

    public CompletableFuture<Optional<ManagedWorld>> world(String name) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT name, profile_id, profile_hash, status, created_at, created_by FROM managed_worlds WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(world(rs)) : Optional.empty();
                }
            }
        });
    }

    public CompletableFuture<Void> saveProfileVersion(String profileId, String hash, String actor) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT OR IGNORE INTO profile_versions(profile_id, hash, saved_at, saved_by) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, profileId);
                ps.setString(2, hash);
                ps.setLong(3, System.currentTimeMillis());
                ps.setString(4, actor);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private static ManagedWorld world(ResultSet rs) throws SQLException {
        return new ManagedWorld(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getLong(5),
                rs.getString(6));
    }
}
