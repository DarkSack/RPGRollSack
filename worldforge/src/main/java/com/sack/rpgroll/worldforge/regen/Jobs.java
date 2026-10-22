package com.sack.rpgroll.worldforge.regen;

import com.sack.rpgroll.worldforge.storage.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/** The {@code jobs} and {@code backups} tables. Every state change is conditional on the current state. */
public final class Jobs {

    /** {@code dimensionDir} is relative to the world container; {@code plan} is the job's work list. */
    public record Job(String id, String type, String world, String dimensionDir, JobState state, String token,
                      String plan, int total, int done, int failed, String backupId, String message,
                      String createdBy, long createdAt, long updatedAt) {
    }

    public record Backup(String id, String jobId, String world, String path, int files, long bytes,
                         String manifestSha256, boolean verified, long createdAt) {
    }

    private static final String COLUMNS = "id, type, world, dimension_dir, state, token, plan, total, done, failed, "
            + "backup_id, message, created_by, created_at, updated_at";

    private final Database db;

    public Jobs(Database db) {
        this.db = db;
    }

    public CompletableFuture<Void> insert(Job j) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO jobs(" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, j.id());
                ps.setString(2, j.type());
                ps.setString(3, j.world());
                ps.setString(4, j.dimensionDir());
                ps.setString(5, j.state().name());
                ps.setString(6, j.token());
                ps.setString(7, j.plan());
                ps.setInt(8, j.total());
                ps.setInt(9, j.done());
                ps.setInt(10, j.failed());
                ps.setString(11, j.backupId());
                ps.setString(12, j.message());
                ps.setString(13, j.createdBy());
                ps.setLong(14, j.createdAt());
                ps.setLong(15, j.updatedAt());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Optional<Job>> get(String id) {
        return query("WHERE id = ?", id).thenApply(l -> l.stream().findFirst());
    }

    public CompletableFuture<List<Job>> recent(int limit) {
        return query("ORDER BY created_at DESC, id LIMIT " + Math.max(1, Math.min(limit, 100)));
    }

    public CompletableFuture<List<Job>> inState(JobState... states) {
        String in = Arrays.stream(states).map(s -> "'" + s.name() + "'").collect(Collectors.joining(","));
        return query("WHERE state IN (" + in + ") ORDER BY created_at, id");
    }

    /** Jobs of one world that block other destructive operations on it. */
    public CompletableFuture<List<Job>> locking(String world) {
        return query("WHERE world = ? ORDER BY created_at", world)
                .thenApply(l -> l.stream().filter(j -> j.state().locksWorld()).toList());
    }

    /**
     * Moves a job only if it is still in {@code from} and the move is allowed.
     *
     * @return false if someone else moved it first (double click, second command) or the move is not allowed
     */
    public CompletableFuture<Boolean> move(String id, JobState from, JobState to, String message) {
        if (!from.canMoveTo(to)) {
            return CompletableFuture.completedFuture(false);
        }
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE jobs SET state = ?, message = COALESCE(?, message), updated_at = ? WHERE id = ? AND state = ?")) {
                ps.setString(1, to.name());
                ps.setString(2, message);
                ps.setLong(3, System.currentTimeMillis());
                ps.setString(4, id);
                ps.setString(5, from.name());
                return ps.executeUpdate() == 1;
            }
        });
    }

    public CompletableFuture<Void> progress(String id, int done, int failed) {
        return update("UPDATE jobs SET done = ?, failed = ?, updated_at = ? WHERE id = ?", done, failed, System.currentTimeMillis(), id);
    }

    public CompletableFuture<Void> saveBackup(Backup b) {
        return db.transaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO backups(id, job_id, world, path, files, bytes, manifest_sha256, verified, created_at) VALUES (?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, b.id());
                ps.setString(2, b.jobId());
                ps.setString(3, b.world());
                ps.setString(4, b.path());
                ps.setInt(5, b.files());
                ps.setLong(6, b.bytes());
                ps.setString(7, b.manifestSha256());
                ps.setInt(8, b.verified() ? 1 : 0);
                ps.setLong(9, b.createdAt());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE jobs SET backup_id = ?, updated_at = ? WHERE id = ?")) {
                ps.setString(1, b.id());
                ps.setLong(2, System.currentTimeMillis());
                ps.setString(3, b.jobId());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Optional<Backup>> backup(String id) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, job_id, world, path, files, bytes, manifest_sha256, verified, created_at FROM backups WHERE id = ?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next()
                            ? Optional.of(new Backup(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5),
                                    rs.getLong(6), rs.getString(7), rs.getInt(8) == 1, rs.getLong(9)))
                            : Optional.empty();
                }
            }
        });
    }

    private CompletableFuture<Void> update(String sql, Object... args) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < args.length; i++) {
                    ps.setObject(i + 1, args[i]);
                }
                ps.executeUpdate();
            }
            return null;
        });
    }

    private CompletableFuture<List<Job>> query(String where, String... args) {
        return db.submit(c -> {
            List<Job> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM jobs " + where)) {
                for (int i = 0; i < args.length; i++) {
                    ps.setString(i + 1, args[i]);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(job(rs));
                    }
                }
            }
            return out;
        });
    }

    private static Job job(ResultSet rs) throws SQLException {
        return new Job(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), JobState.valueOf(rs.getString(5)),
                rs.getString(6), rs.getString(7), rs.getInt(8), rs.getInt(9), rs.getInt(10), rs.getString(11),
                rs.getString(12), rs.getString(13), rs.getLong(14), rs.getLong(15));
    }
}
