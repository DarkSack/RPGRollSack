package com.sack.rpgroll.machines.quarry;

import com.sack.rpgroll.machines.quarry.QuarrySettings.Numeric;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Unlock;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Una cantera puesta. Excava un cuadrado de lado {@link Numeric#AREA} centrado en ella, capa a
 * capa desde el bloque de debajo hasta el fondo del mundo; el cursor ({@link #cursorY},
 * {@link #cursorIndex}) dice por dónde va y se guarda, así que sigue donde lo dejó.
 */
public final class Quarry {

    public enum Status { MINING, OFF, NO_OWNER, NO_CHEST, FULL, WAITING_CHUNK, FINISHED }

    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final UUID owner;
    private String ownerName;

    private final Map<Numeric, Integer> levels = new EnumMap<>(Numeric.class);
    private final Set<Unlock> unlocked = EnumSet.noneOf(Unlock.class);
    private final Set<Unlock> active = EnumSet.noneOf(Unlock.class);
    private boolean enabled = true;
    private int cursorY;
    private int cursorIndex;
    private boolean finished;
    private final List<ItemStack> buffer = new ArrayList<>();

    // No se guarda: se recalcula al trabajar.
    private double credit;
    private Status status = Status.MINING;

    public Quarry(String world, int x, int y, int z, UUID owner, String ownerName) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.owner = owner;
        this.ownerName = ownerName;
        this.cursorY = y - 1;
    }

    public String key() {
        return world + ";" + x + ";" + y + ";" + z;
    }

    public String world() {
        return world;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public void ownerName(String name) {
        this.ownerName = name;
    }

    public int level(Numeric numeric) {
        return levels.getOrDefault(numeric, 0);
    }

    public void level(Numeric numeric, int level) {
        levels.put(numeric, Math.max(0, level));
    }

    public boolean unlocked(Unlock unlock) {
        return unlocked.contains(unlock);
    }

    public boolean active(Unlock unlock) {
        return unlocked.contains(unlock) && active.contains(unlock);
    }

    public void unlock(Unlock unlock, boolean on) {
        unlocked.add(unlock);
        if (on) {
            active.add(unlock);
        } else {
            active.remove(unlock);
        }
    }

    public boolean enabled() {
        return enabled;
    }

    public void enabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int cursorY() {
        return cursorY;
    }

    public int cursorIndex() {
        return cursorIndex;
    }

    public void cursor(int y, int index) {
        this.cursorY = y;
        this.cursorIndex = index;
    }

    /** Vuelve a empezar desde arriba (tras ampliar el área: las columnas nuevas están enteras). */
    public void restart() {
        this.cursorY = y - 1;
        this.cursorIndex = 0;
        this.finished = false;
    }

    public boolean finished() {
        return finished;
    }

    public void finished(boolean finished) {
        this.finished = finished;
    }

    public List<ItemStack> buffer() {
        return buffer;
    }

    public double credit() {
        return credit;
    }

    public void credit(double credit) {
        this.credit = credit;
    }

    public Status status() {
        return status;
    }

    public void status(Status status) {
        this.status = status;
    }

    // ---------------------------------------------------------------- área

    /** Esquina menor del área en X (o Z) para un lado {@code side}: la cantera queda en medio. */
    public static int min(int center, int side) {
        return center - side / 2;
    }

    /** El bloque {@code index} de una capa de lado {@code side}: {x, z}. */
    public static int[] column(int centerX, int centerZ, int side, int index) {
        return new int[] {min(centerX, side) + index % side, min(centerZ, side) + index / side};
    }

    /**
     * Avanza el cursor un bloque. Devuelve {y, index} del siguiente, o null si ya pasó del
     * fondo del mundo.
     */
    public static int[] advance(int cursorY, int cursorIndex, int side, int minHeight) {
        int index = cursorIndex + 1;
        int y = cursorY;
        if (index >= side * side) {
            index = 0;
            y--;
        }
        return y < minHeight ? null : new int[] {y, index};
    }

    /** Cuánto lleva, de 0 a 1: capas hechas desde su altura hasta el fondo. */
    public double progress(int side, int minHeight) {
        if (finished) {
            return 1.0;
        }
        double total = (double) (y - minHeight) * side * side;
        double done = (double) (y - 1 - cursorY) * side * side + cursorIndex;
        return total <= 0 ? 1.0 : Math.clamp(done / total, 0.0, 1.0);
    }
}
