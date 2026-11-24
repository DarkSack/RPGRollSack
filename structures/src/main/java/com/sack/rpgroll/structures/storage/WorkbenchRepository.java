package com.sack.rpgroll.structures.storage;

import com.sack.rpgroll.structures.workbench.WorkbenchTier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Mesas guardadas. Todo corre en el hilo de {@link Database}. */
public final class WorkbenchRepository {

    private static final String COLUMNS = "id, owner, world, x, y, z, tier, serial, orphan, created_at";

    private final Database db;
    private final Dialect dialect;

    public WorkbenchRepository(Database db, Dialect dialect) {
        this.db = db;
        this.dialect = dialect;
    }

    public CompletableFuture<Void> insert(WorkbenchRecord r) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO workbenches(" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, r.id().toString());
                ps.setString(2, r.owner().toString());
                ps.setString(3, r.world());
                ps.setInt(4, r.x());
                ps.setInt(5, r.y());
                ps.setInt(6, r.z());
                ps.setString(7, r.tier().name());
                ps.setLong(8, r.serial());
                ps.setInt(9, r.orphan() ? 1 : 0);
                ps.setLong(10, r.createdAt());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Optional<WorkbenchRecord>> find(UUID id) {
        return db.submit(c -> one(c, "WHERE id = ?", ps -> ps.setString(1, id.toString())));
    }

    public CompletableFuture<Optional<WorkbenchRecord>> findAt(String world, int x, int y, int z) {
        return db.submit(c -> one(c, "WHERE world = ? AND x = ? AND y = ? AND z = ?", ps -> {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
        }));
    }

    /** true solo si la mesa estaba en {@code from} y cambió una fila: un segundo clic devuelve false. */
    public CompletableFuture<Boolean> upgrade(UUID id, WorkbenchTier from, WorkbenchTier to) {
        return db.submit(c -> update(c, "UPDATE workbenches SET tier = ? WHERE id = ? AND tier = ?", ps -> {
            ps.setString(1, to.name());
            ps.setString(2, id.toString());
            ps.setString(3, from.name());
        }) == 1);
    }

    public CompletableFuture<Boolean> move(UUID id, String world, int x, int y, int z) {
        return db.submit(c -> update(c,
                "UPDATE workbenches SET world = ?, x = ?, y = ?, z = ?, orphan = 0 WHERE id = ?", ps -> {
                    ps.setString(1, world);
                    ps.setInt(2, x);
                    ps.setInt(3, y);
                    ps.setInt(4, z);
                    ps.setString(5, id.toString());
                }) == 1);
    }

    public CompletableFuture<Boolean> remove(UUID id) {
        return db.submit(c -> update(c, "DELETE FROM workbenches WHERE id = ?",
                ps -> ps.setString(1, id.toString())) == 1);
    }

    public CompletableFuture<Boolean> markOrphan(UUID id) {
        return db.submit(c -> update(c, "UPDATE workbenches SET orphan = 1 WHERE id = ?",
                ps -> ps.setString(1, id.toString())) == 1);
    }

    public CompletableFuture<List<WorkbenchRecord>> all() {
        return all(id -> {});
    }

    /** Todas las filas legibles; las que no (UUID o nivel inválidos) se saltan y su id va a {@code bad}. */
    public CompletableFuture<List<WorkbenchRecord>> all(Consumer<String> bad) {
        return db.submit(c -> {
            List<WorkbenchRecord> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM workbenches");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        out.add(read(rs));
                    } catch (RuntimeException e) {
                        bad.accept(rs.getString("id"));
                    }
                }
            }
            return out;
        });
    }

    public CompletableFuture<Void> trust(UUID id, UUID player) {
        return db.submit(c -> {
            update(c, dialect.insertIgnore() + " INTO workbench_access(workbench_id, player) VALUES (?,?)", ps -> {
                ps.setString(1, id.toString());
                ps.setString(2, player.toString());
            });
            return null;
        });
    }

    public CompletableFuture<Void> untrust(UUID id, UUID player) {
        return db.submit(c -> {
            update(c, "DELETE FROM workbench_access WHERE workbench_id = ? AND player = ?", ps -> {
                ps.setString(1, id.toString());
                ps.setString(2, player.toString());
            });
            return null;
        });
    }

    public CompletableFuture<Set<UUID>> trusted(UUID id) {
        return db.submit(c -> {
            Set<UUID> out = new HashSet<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT player FROM workbench_access WHERE workbench_id = ?")) {
                ps.setString(1, id.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(UUID.fromString(rs.getString(1)));
                    }
                }
            }
            return out;
        });
    }

    /** Devuelve el siguiente número de serie y lo gasta. Seguro porque todo corre en un solo hilo. */
    public CompletableFuture<Long> nextSerial() {
        return db.submit(c -> {
            c.setAutoCommit(false);
            try {
                long next;
                try (PreparedStatement ps = c.prepareStatement("SELECT next FROM serials");
                     ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Tabla serials vacía");
                    }
                    next = rs.getLong(1);
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE serials SET next = ?")) {
                    ps.setLong(1, next + 1);
                    ps.executeUpdate();
                }
                c.commit();
                return next;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        });
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private static int update(Connection c, String sql, Binder b) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            b.bind(ps);
            return ps.executeUpdate();
        }
    }

    private static Optional<WorkbenchRecord> one(Connection c, String where, Binder b) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM workbenches " + where)) {
            b.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        }
    }

    private static WorkbenchRecord read(ResultSet rs) throws SQLException {
        return new WorkbenchRecord(UUID.fromString(rs.getString("id")), UUID.fromString(rs.getString("owner")),
                rs.getString("world"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"),
                WorkbenchTier.valueOf(rs.getString("tier")), rs.getLong("serial"), rs.getInt("orphan") != 0,
                rs.getLong("created_at"));
    }
}
