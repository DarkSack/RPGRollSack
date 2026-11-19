package com.sack.rpgroll.machines.area;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Una máquina de área puesta en el mundo (Pescadora, Granja o Leñadora): dónde está, de quién es,
 * sus mejoras y lo que tiene pendiente de meter en el cofre.
 */
public final class AreaMachine {

    private final String kind;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final UUID owner;
    private String ownerName;
    private final Map<String, Integer> levels = new HashMap<>();
    private boolean enabled = true;
    private final List<ItemStack> buffer = new ArrayList<>();
    private double credit;
    private AreaStatus status = AreaStatus.IDLE;
    private double gauge;
    private int cursor;

    public AreaMachine(String kind, String world, int x, int y, int z, UUID owner, String ownerName) {
        this.kind = kind;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.owner = owner;
        this.ownerName = ownerName;
    }

    /** El mismo formato que {@code Displays.key(block)}: {@code mundo;x;y;z}. */
    public String key() {
        return world + ";" + x + ";" + y + ";" + z;
    }

    public String kind() {
        return kind;
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

    public int level(String upgrade) {
        return levels.getOrDefault(upgrade, 0);
    }

    public void level(String upgrade, int level) {
        levels.put(upgrade, Math.max(0, level));
    }

    public Map<String, Integer> levels() {
        return levels;
    }

    public boolean enabled() {
        return enabled;
    }

    public void enabled(boolean enabled) {
        this.enabled = enabled;
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

    public AreaStatus status() {
        return status;
    }

    public void status(AreaStatus status) {
        this.status = status;
    }

    /** Lo lleno que está el indicador del menú, de 0 a 1 (lo pone cada tipo). */
    public double gauge() {
        return gauge;
    }

    public void gauge(double gauge) {
        this.gauge = gauge;
    }

    /** Por dónde va recorriendo su cuadrado (índice de casilla). */
    public int cursor() {
        return cursor;
    }

    public void cursor(int cursor) {
        this.cursor = cursor;
    }
}
