package com.sack.rpgroll.structures.project;

import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.structures.storage.MaterialRow;
import com.sack.rpgroll.structures.storage.ProjectRecord;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Map;
import java.util.UUID;

/** Obra en memoria. Mutable y solo del hilo principal; la base de datos se actualiza aparte. */
public final class Project {

    private final UUID id;
    private final UUID bench;
    private final String structure;
    private final UUID owner;
    private final String world;
    private final int x, y, z;
    private final Rotation rotation;
    private final int total;
    private final long createdAt;
    private final String sourceHash;
    private final long moneyCents;
    private final ProjectKind kind;
    private final UUID buildId;
    private final int repairBlocks;
    private ProjectState state;
    private int cursor, placed, skipped;
    private String error;
    private Map<String, MaterialRow> materials;
    private boolean busy;

    public Project(ProjectRecord r, Map<String, MaterialRow> materials) {
        this.id = r.id();
        this.bench = r.bench();
        this.structure = r.structure();
        this.owner = r.owner();
        this.world = r.world();
        this.x = r.x();
        this.y = r.y();
        this.z = r.z();
        this.rotation = r.rotation();
        this.total = r.total();
        this.createdAt = r.createdAt();
        this.sourceHash = r.sourceHash();
        this.moneyCents = r.moneyCents();
        this.kind = r.kind();
        this.buildId = r.buildId();
        this.repairBlocks = r.repairBlocks();
        this.state = r.state();
        this.cursor = r.cursor();
        this.placed = r.placed();
        this.skipped = r.skipped();
        this.error = r.error();
        this.materials = materials;
    }

    public UUID id() { return id; }
    public UUID bench() { return bench; }
    public String structure() { return structure; }
    public UUID owner() { return owner; }
    public ProjectState state() { return state; }
    public Rotation rotation() { return rotation; }
    public int cursor() { return cursor; }
    public int placed() { return placed; }
    public int skipped() { return skipped; }
    public int total() { return total; }
    public long createdAt() { return createdAt; }
    public String error() { return error; }
    public Map<String, MaterialRow> materials() { return materials; }
    public String world() { return world; }
    /** Esquina mínima de la caja ya girada. */
    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }

    /** Esquina mínima de la caja ya girada; null si el mundo no está cargado. */
    public Location origin() {
        World w = Bukkit.getServer() == null ? null : Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z);
    }

    /** Hash del esquemático al crear la obra; null en obras de antes de guardarlo (sin comprobar). */
    public String sourceHash() { return sourceHash; }
    /** Dinero cobrado al confirmar, en céntimos (0 sin dinero o en obras de antes de la v4). */
    public long moneyCents() { return moneyCents; }
    public ProjectKind kind() { return kind; }
    /** La construcción que repara o mejora; null en una BUILD. */
    public UUID buildId() { return buildId; }
    /** Bloques pagados de una REPAIR: lo más que el motor escribe (en ella {@link #placed()} cuenta lo escrito). */
    public int repairBlocks() { return repairBlocks; }
    public boolean busy() { return busy; }
    public void setBusy(boolean busy) { this.busy = busy; }
    public void setState(ProjectState state) { this.state = state; }
    public void setError(String error) { this.error = error; }
    public void setMaterials(Map<String, MaterialRow> materials) { this.materials = materials; }

    public void setProgress(int cursor, int placed, int skipped) {
        this.cursor = cursor;
        this.placed = placed;
        this.skipped = skipped;
    }
}
