package com.sack.rpgroll.structures.storage;

import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.structures.project.ProjectKind;
import com.sack.rpgroll.structures.project.ProjectService;
import com.sack.rpgroll.structures.project.ProjectState;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Obras, su libro de materiales y dinero, devoluciones pendientes y construcciones terminadas. Corre en el hilo de {@link Database}. */
public final class ProjectRepository {

    private static final String COLUMNS = "id, bench_id, structure_id, owner, state, world, x, y, z, rotation,"
            + " `cursor`, placed, skipped, total, error, created_at, updated_at, source_hash, money_cents, kind,"
            + " build_id, repair_blocks";
    private static final String ACTIVE = "state NOT IN ('COMPLETED', 'CANCELLED')";
    private static final String BUILD_COLUMNS = "id, project_id, structure_id, owner, world, x, y, z, rotation,"
            + " min_x, min_y, min_z, max_x, max_y, max_z, completed_at, source_hash";

    private final Database db;
    private final Dialect dialect;

    public ProjectRepository(Database db, Dialect dialect) {
        this.db = db;
        this.dialect = dialect;
    }

    /** Id del apunte MONEY_CHARGE de una obra: uno por obra, así un alta repetida no cobra dos veces en el libro. */
    public static UUID chargeId(UUID project) {
        return UUID.nameUUIDFromBytes(("money-charge:" + project).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Con {@code moneyCents > 0} apunta también el MONEY_CHARGE del dueño ({@link #chargeId}) en la misma
     * transacción: o quedan la obra y su cobro, o nada.
     *
     * @param sizeX medida ya girada de la estructura; la caja de la obra va de (x,y,z) a (x+sizeX-1, ...), o sea
     *              que (x,y,z) del registro es la esquina MÍNIMA de la caja ya girada
     */
    public CompletableFuture<Void> insert(ProjectRecord r, Map<String, Integer> required, int sizeX, int sizeY,
                                          int sizeZ) {
        return db.submit(c -> tx(c, () -> {
            if (r.kind() == ProjectKind.UPGRADE) {
                // La construcción que se mejora tiene que seguir registrada y ser del dueño: si no, sus bloques ya
                // no se sabe de quién son.
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT COUNT(*) FROM builds WHERE id = ? AND owner = ?")) {
                    ps.setString(1, String.valueOf(r.buildId()));
                    ps.setString(2, r.owner().toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || rs.getInt(1) == 0) {
                            throw new ProjectService.RepairException(ProjectService.RepairException.UPGRADE_GONE);
                        }
                    }
                }
                // La caja nueva de una mejora no puede pisar otra construcción terminada; la que se mejora no cuenta.
                try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM builds WHERE world = ?"
                        + " AND id <> ? AND min_x <= ? AND max_x >= ? AND min_y <= ? AND max_y >= ?"
                        + " AND min_z <= ? AND max_z >= ?")) {
                    ps.setString(1, r.world());
                    ps.setString(2, String.valueOf(r.buildId()));
                    ps.setInt(3, r.x() + sizeX - 1);
                    ps.setInt(4, r.x());
                    ps.setInt(5, r.y() + sizeY - 1);
                    ps.setInt(6, r.y());
                    ps.setInt(7, r.z() + sizeZ - 1);
                    ps.setInt(8, r.z());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            throw new ProjectService.OverlapException("La mejora " + r.id()
                                    + " se cruza con otra construcción");
                        }
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO projects(" + COLUMNS
                    + ", size_x, size_y, size_z) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, r.id().toString());
                ps.setString(2, r.bench().toString());
                ps.setString(3, r.structure());
                ps.setString(4, r.owner().toString());
                ps.setString(5, r.state().name());
                ps.setString(6, r.world());
                ps.setInt(7, r.x());
                ps.setInt(8, r.y());
                ps.setInt(9, r.z());
                ps.setString(10, r.rotation().name());
                ps.setInt(11, r.cursor());
                ps.setInt(12, r.placed());
                ps.setInt(13, r.skipped());
                ps.setInt(14, r.total());
                ps.setString(15, r.error());
                ps.setLong(16, r.createdAt());
                ps.setLong(17, r.updatedAt());
                ps.setString(18, r.sourceHash());
                ps.setLong(19, r.moneyCents());
                ps.setString(20, r.kind().name());
                ps.setString(21, r.buildId() == null ? null : r.buildId().toString());
                ps.setInt(22, r.repairBlocks());
                ps.setInt(23, sizeX);
                ps.setInt(24, sizeY);
                ps.setInt(25, sizeZ);
                ps.executeUpdate();
            }
            if (r.moneyCents() > 0 && !applyIn(c, new Movement(chargeId(r.id()), r.id(), r.owner(),
                    Movement.Kind.MONEY_CHARGE, Movement.MONEY_KEY, Math.toIntExact(r.moneyCents())))) {
                throw new SQLException("El cobro de la obra " + r.id() + " ya estaba en el libro");
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO project_materials(project_id, material_key, required) VALUES (?,?,?)")) {
                for (Map.Entry<String, Integer> e : required.entrySet()) {
                    ps.setString(1, r.id().toString());
                    ps.setString(2, e.getKey());
                    ps.setInt(3, e.getValue());
                    ps.executeUpdate();
                }
            }
            return null;
        }));
    }

    /** Obras que no están COMPLETED ni CANCELLED. */
    public CompletableFuture<List<ProjectRecord>> active() {
        return db.submit(c -> {
            List<ProjectRecord> out = new ArrayList<>();
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT " + COLUMNS + " FROM projects WHERE " + ACTIVE)) {
                while (rs.next()) {
                    out.add(new ProjectRecord(UUID.fromString(rs.getString("id")),
                            UUID.fromString(rs.getString("bench_id")), rs.getString("structure_id"),
                            UUID.fromString(rs.getString("owner")), ProjectState.valueOf(rs.getString("state")),
                            rs.getString("world"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"),
                            Rotation.valueOf(rs.getString("rotation")), rs.getInt("cursor"), rs.getInt("placed"),
                            rs.getInt("skipped"), rs.getInt("total"), rs.getString("error"),
                            rs.getLong("created_at"), rs.getLong("updated_at"), rs.getString("source_hash"),
                            rs.getLong("money_cents"), ProjectKind.valueOf(rs.getString("kind")),
                            rs.getString("build_id") == null ? null : UUID.fromString(rs.getString("build_id")),
                            rs.getInt("repair_blocks")));
                }
            }
            return out;
        });
    }

    public CompletableFuture<Map<String, MaterialRow>> materials(UUID project) {
        return db.submit(c -> {
            Map<String, MaterialRow> out = new LinkedHashMap<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT material_key, required, deposited, consumed"
                    + " FROM project_materials WHERE project_id = ?")) {
                ps.setString(1, project.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.put(rs.getString(1), new MaterialRow(rs.getInt(2), rs.getInt(3), rs.getInt(4)));
                    }
                }
            }
            return out;
        });
    }

    /**
     * Apunta el movimiento en el libro y ajusta el material, todo en una transacción. false (sin cambios) si su id
     * ya estaba, si el material no es de la obra, si la cantidad no es positiva o si dejaría {@code deposited}
     * por debajo de {@code consumed} o de cero.
     */
    public CompletableFuture<Boolean> applyMovement(Movement m) {
        return db.submit(c -> {
            if (m.amount() <= 0) {
                return false;
            }
            return tx(c, () -> applyIn(c, m));
        });
    }

    /**
     * Varios movimientos en un solo envío y una sola transacción, cada uno con las reglas de {@link #applyMovement}.
     * Un movimiento rechazado da false sin deshacer los demás (punto de guardado propio); un error de SQL deshace
     * todo y falla el futuro.
     *
     * @return un resultado por movimiento, en el mismo orden
     */
    public CompletableFuture<List<Boolean>> applyMovements(List<Movement> moves) {
        return db.submit(c -> tx(c, () -> {
            List<Boolean> out = new ArrayList<>(moves.size());
            for (Movement m : moves) {
                if (m.amount() <= 0) {
                    out.add(false);
                    continue;
                }
                Savepoint sp = c.setSavepoint();
                boolean ok = applyIn(c, m);
                if (ok) {
                    c.releaseSavepoint(sp);
                } else {
                    c.rollback(sp);
                }
                out.add(ok);
            }
            return out;
        }));
    }

    /**
     * Todos los REFUND, el MONEY_REFUND (con su fila en {@code pending_money} para el DUEÑO de la obra, no para quien
     * cancela) y el paso a CANCELLED en una sola transacción: o entra todo o no cambia nada. false si algún
     * reembolso no es válido (mismas reglas que {@link #applyMovement}) o si la obra no existe o ya es final.
     */
    public CompletableFuture<Boolean> cancel(UUID project, List<Movement> refunds) {
        return db.submit(c -> tx(c, () -> {
            for (Movement m : refunds) {
                boolean money = m.kind() == Movement.Kind.MONEY_REFUND;
                if (m.kind() != Movement.Kind.REFUND && !money || m.amount() <= 0 || !m.project().equals(project)
                        || !applyIn(c, m)) {
                    return false;
                }
                if (money) {
                    try (PreparedStatement ps = c.prepareStatement("INSERT INTO pending_money(player, amount_cents,"
                            + " created_at) SELECT owner, ?, ? FROM projects WHERE id = ?")) {
                        ps.setLong(1, m.amount());
                        ps.setLong(2, System.currentTimeMillis());
                        ps.setString(3, project.toString());
                        if (ps.executeUpdate() != 1) {
                            return false;
                        }
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE projects SET state = 'CANCELLED', updated_at = ? WHERE id = ? AND " + ACTIVE)) {
                ps.setLong(1, System.currentTimeMillis());
                ps.setString(2, project.toString());
                return ps.executeUpdate() == 1;
            }
        }));
    }

    /** Dentro de una transacción ya abierta. */
    private boolean applyIn(Connection c, Movement m) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(dialect.insertIgnore()
                + " INTO ledger(id, project_id, player, kind, material_key, amount, created_at)"
                + " VALUES (?,?,?,?,?,?,?)")) {
            ps.setString(1, m.id().toString());
            ps.setString(2, m.project().toString());
            ps.setString(3, m.player().toString());
            ps.setString(4, m.kind().name());
            ps.setString(5, m.materialKey());
            ps.setInt(6, m.amount());
            ps.setLong(7, System.currentTimeMillis());
            if (ps.executeUpdate() != 1) {
                return false;
            }
        }
        // CLAIM y el dinero solo dejan rastro en el libro: el material ya salió antes; el dinero lo mueve Vault.
        String set = switch (m.kind()) {
            case DEPOSIT -> "deposited = deposited + ? WHERE project_id = ? AND material_key = ?";
            case WITHDRAW, REFUND -> "deposited = deposited - ? WHERE project_id = ? AND material_key = ?"
                    + " AND deposited - ? >= consumed";
            case CONSUME -> "consumed = consumed + ? WHERE project_id = ? AND material_key = ?"
                    + " AND consumed + ? <= deposited";
            case CLAIM, MONEY_CHARGE, MONEY_REFUND -> null;
        };
        if (set == null) {
            return true;
        }
        try (PreparedStatement ps = c.prepareStatement("UPDATE project_materials SET " + set)) {
            ps.setInt(1, m.amount());
            ps.setString(2, m.project().toString());
            ps.setString(3, m.materialKey());
            if (m.kind() != Movement.Kind.DEPOSIT) {
                ps.setInt(4, m.amount());
            }
            return ps.executeUpdate() == 1;
        }
    }

    /** true si actualizó; false si la obra no existe o ya está COMPLETED/CANCELLED (un estado final no revive). */
    public CompletableFuture<Boolean> saveProgress(UUID id, ProjectState state, int cursor, int placed, int skipped,
                                                String error) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE projects SET state = ?, `cursor` = ?, placed = ?,"
                    + " skipped = ?, error = ?, updated_at = ? WHERE id = ? AND " + ACTIVE)) {
                ps.setString(1, state.name());
                ps.setInt(2, cursor);
                ps.setInt(3, placed);
                ps.setInt(4, skipped);
                ps.setString(5, error);
                ps.setLong(6, System.currentTimeMillis());
                ps.setString(7, id.toString());
                return ps.executeUpdate() == 1;
            }
        });
    }

    /**
     * Pasa la obra a COMPLETED y escribe su construcción, en una transacción. false (sin cambios) si la obra no
     * existe o ya estaba COMPLETED/CANCELLED.
     */
    public CompletableFuture<Boolean> complete(UUID id, BuildRecord b) {
        return complete(id, b, null);
    }

    private CompletableFuture<Boolean> complete(UUID id, BuildRecord b, UUID replaced) {
        return db.submit(c -> tx(c, () -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE projects SET state = 'COMPLETED', updated_at = ? WHERE id = ? AND " + ACTIVE)) {
                ps.setLong(1, b.completedAt());
                ps.setString(2, id.toString());
                if (ps.executeUpdate() != 1) {
                    return false;
                }
            }
            if (replaced != null) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM builds WHERE id = ?")) {
                    ps.setString(1, replaced.toString());
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO builds(" + BUILD_COLUMNS + ")"
                    + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, b.id().toString());
                ps.setString(2, b.project().toString());
                ps.setString(3, b.structure());
                ps.setString(4, b.owner().toString());
                ps.setString(5, b.world());
                ps.setInt(6, b.x());
                ps.setInt(7, b.y());
                ps.setInt(8, b.z());
                ps.setString(9, b.rotation().name());
                ps.setInt(10, b.box().minX());
                ps.setInt(11, b.box().minY());
                ps.setInt(12, b.box().minZ());
                ps.setInt(13, b.box().maxX());
                ps.setInt(14, b.box().maxY());
                ps.setInt(15, b.box().maxZ());
                ps.setLong(16, b.completedAt());
                ps.setString(17, b.sourceHash());
                ps.executeUpdate();
            }
            return true;
        }));
    }

    /**
     * Fin de una reparación: la obra pasa a COMPLETED y su construcción ({@code build}) toma la fecha {@code at}, en
     * una transacción; no se crea otra construcción. false (sin cambios) si la obra no existe o ya era final. Si la
     * construcción ya no está (se borró mientras tanto), la obra termina igual.
     */
    public CompletableFuture<Boolean> completeRepair(UUID project, UUID build, long at) {
        return db.submit(c -> tx(c, () -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE projects SET state = 'COMPLETED', updated_at = ? WHERE id = ? AND " + ACTIVE)) {
                ps.setLong(1, at);
                ps.setString(2, project.toString());
                if (ps.executeUpdate() != 1) {
                    return false;
                }
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE builds SET completed_at = ? WHERE id = ?")) {
                ps.setLong(1, at);
                ps.setString(2, build.toString());
                ps.executeUpdate();
            }
            return true;
        }));
    }

    /**
     * Fin de una mejora: la obra pasa a COMPLETED, la construcción vieja ({@code old}) se borra y entra la nueva
     * ({@code b}), en una transacción. false (sin cambios) si la obra no existe o ya era final. Si la vieja ya no
     * estaba (borrada mientras tanto), entra la nueva igual.
     */
    public CompletableFuture<Boolean> completeUpgrade(UUID project, UUID old, BuildRecord b) {
        return complete(project, b, old);
    }

    /** @return id de la fila nueva */
    public CompletableFuture<Long> addPending(UUID player, byte[] item) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO pending_returns(player, item, created_at) VALUES (?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, player.toString());
                ps.setBytes(2, item);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("Sin id para pending_returns");
                    }
                    return rs.getLong(1);
                }
            }
        });
    }

    /** Las no reclamadas de un jugador, de la más antigua a la más nueva. */
    public CompletableFuture<List<PendingRow>> pending(UUID player) {
        return db.submit(c -> {
            List<PendingRow> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT id, item, created_at FROM pending_returns"
                    + " WHERE player = ? AND claimed_at IS NULL ORDER BY id")) {
                ps.setString(1, player.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new PendingRow(rs.getLong(1), player, rs.getBytes(2), rs.getLong(3)));
                    }
                }
            }
            return out;
        });
    }

    /** true solo la primera vez: un segundo reclamo de la misma fila devuelve false. */
    public CompletableFuture<Boolean> claim(long pendingId) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE pending_returns SET claimed_at = ? WHERE id = ? AND claimed_at IS NULL")) {
                ps.setLong(1, System.currentTimeMillis());
                ps.setLong(2, pendingId);
                return ps.executeUpdate() == 1;
            }
        });
    }

    /** true si el libro ya tiene un apunte con ese id (p. ej. {@link #chargeId}: el alta de la obra llegó a la base). */
    public CompletableFuture<Boolean> hasLedger(UUID movementId) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM ledger WHERE id = ?")) {
                ps.setString(1, movementId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    /** @return id de la fila nueva de dinero pendiente ({@code cents} en céntimos) */
    public CompletableFuture<Long> addPendingMoney(UUID player, long cents) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO pending_money(player, amount_cents, created_at) VALUES (?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, player.toString());
                ps.setLong(2, cents);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("Sin id para pending_money");
                    }
                    return rs.getLong(1);
                }
            }
        });
    }

    /** Dinero sin pagar de un jugador, de la fila más antigua a la más nueva. */
    public CompletableFuture<List<PendingMoneyRow>> pendingMoney(UUID player) {
        return db.submit(c -> {
            List<PendingMoneyRow> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT id, amount_cents, created_at FROM pending_money"
                    + " WHERE player = ? AND paid_at IS NULL ORDER BY id")) {
                ps.setString(1, player.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new PendingMoneyRow(rs.getLong(1), player, rs.getLong(2), rs.getLong(3)));
                    }
                }
            }
            return out;
        });
    }

    /** true solo la primera vez: marcar otra vez la misma fila devuelve false. */
    public CompletableFuture<Boolean> markMoneyPaid(long pendingId) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE pending_money SET paid_at = ? WHERE id = ? AND paid_at IS NULL")) {
                ps.setLong(1, System.currentTimeMillis());
                ps.setLong(2, pendingId);
                return ps.executeUpdate() == 1;
            }
        });
    }

    /** Construcciones terminadas de un jugador, de la más nueva a la más vieja (menú «Mis construcciones»). */
    public CompletableFuture<List<BuildRecord>> buildsOf(UUID owner) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT " + BUILD_COLUMNS + " FROM builds"
                    + " WHERE owner = ? ORDER BY completed_at DESC, id")) {
                ps.setString(1, owner.toString());
                return builds(ps);
            }
        });
    }

    /** Todas las construcciones terminadas (los módulos al arrancar). */
    public CompletableFuture<List<BuildRecord>> allBuilds() {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT " + BUILD_COLUMNS + " FROM builds ORDER BY id")) {
                return builds(ps);
            }
        });
    }

    /** Borra el registro de una construcción (los bloques del mundo no se tocan). false si no estaba. */
    public CompletableFuture<Boolean> deleteBuild(UUID id) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM builds WHERE id = ?")) {
                ps.setString(1, id.toString());
                return ps.executeUpdate() == 1;
            }
        });
    }

    /** Lo que guardó el módulo de una construcción ({@link #saveModuleData}); vacío si nada. */
    public CompletableFuture<Optional<String>> moduleData(UUID buildId) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT data FROM build_modules WHERE build_id = ?")) {
                ps.setString(1, buildId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
                }
            }
        });
    }

    /** Guarda (o reemplaza) los datos del módulo de una construcción. */
    public CompletableFuture<Void> saveModuleData(UUID buildId, String data) {
        return db.submit(c -> tx(c, () -> {
            try (PreparedStatement del = c.prepareStatement("DELETE FROM build_modules WHERE build_id = ?");
                 PreparedStatement ins = c.prepareStatement(
                         "INSERT INTO build_modules(build_id, data) VALUES (?, ?)")) {
                del.setString(1, buildId.toString());
                del.executeUpdate();
                ins.setString(1, buildId.toString());
                ins.setString(2, data);
                ins.executeUpdate();
            }
            return null;
        }));
    }

    /** Quita los datos del módulo de una construcción (ya no queda nada que deshacer). */
    public CompletableFuture<Void> deleteModuleData(UUID buildId) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM build_modules WHERE build_id = ?")) {
                ps.setString(1, buildId.toString());
                ps.executeUpdate();
            }
            return null;
        });
    }

    private static List<BuildRecord> builds(PreparedStatement ps) throws SQLException {
        List<BuildRecord> out = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new BuildRecord(UUID.fromString(rs.getString(1)), UUID.fromString(rs.getString(2)),
                        rs.getString(3), UUID.fromString(rs.getString(4)), rs.getString(5), rs.getInt(6),
                        rs.getInt(7), rs.getInt(8), Rotation.valueOf(rs.getString(9)), new Box(rs.getInt(10),
                        rs.getInt(11), rs.getInt(12), rs.getInt(13), rs.getInt(14), rs.getInt(15)),
                        rs.getLong(16), rs.getString(17)));
            }
        }
        return out;
    }

    /** Caja de cada obra activa (esquina + medida guardada), por id de obra; ProjectService la usa contra choques. */
    public CompletableFuture<Map<UUID, Box>> activeBoxes() {
        return db.submit(c -> {
            Map<UUID, Box> out = new LinkedHashMap<>();
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT id, x, y, z, x + size_x - 1, y + size_y - 1, z + size_z - 1"
                         + " FROM projects WHERE " + ACTIVE)) {
                while (rs.next()) {
                    out.put(UUID.fromString(rs.getString(1)), new Box(rs.getInt(2), rs.getInt(3), rs.getInt(4),
                            rs.getInt(5), rs.getInt(6), rs.getInt(7)));
                }
            }
            return out;
        });
    }

    /** Cajas de las obras activas (esquina + medida guardada) y de las construcciones terminadas en ese mundo. */
    public CompletableFuture<List<Box>> boxesIn(String world) {
        return db.submit(c -> {
            List<Box> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT x, y, z, x + size_x - 1, y + size_y - 1,"
                    + " z + size_z - 1 FROM projects WHERE world = ? AND " + ACTIVE
                    + " UNION ALL SELECT min_x, min_y, min_z, max_x, max_y, max_z FROM builds WHERE world = ?")) {
                ps.setString(1, world);
                ps.setString(2, world);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new Box(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5),
                                rs.getInt(6)));
                    }
                }
            }
            return out;
        });
    }

    @FunctionalInterface
    private interface TxWork<T> {
        T run() throws SQLException;
    }

    /** Una transacción; si el trabajo devuelve false o falla se deshace todo. */
    private static <T> T tx(Connection c, TxWork<T> work) throws SQLException {
        c.setAutoCommit(false);
        try {
            T result = work.run();
            if (Boolean.FALSE.equals(result)) {
                c.rollback();
            } else {
                c.commit();
            }
            return result;
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }
}
