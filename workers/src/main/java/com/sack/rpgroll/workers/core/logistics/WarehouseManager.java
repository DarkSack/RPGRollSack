package com.sack.rpgroll.workers.core.logistics;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Todos los almacenes designados viven en un único {@code warehouses.yml}
 * — a diferencia de Species/Breed/etc. de Ranching, un Warehouse no es
 * contenido reusable, es una ubicación puntual del mundo, así que una
 * lista simple alcanza (mismo criterio que el {@code state.yml} de
 * RPGRoll-Seasons para el reloj de calendario).
 */
public class WarehouseManager {

    private final Plugin plugin;
    private final File file;
    private final List<Warehouse> warehouses = new ArrayList<>();

    public WarehouseManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "warehouses.yml");
    }

    public void load() {

        warehouses.clear();

        if (!file.isFile()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<?> rawList = config.getList("warehouses");

        if (rawList == null) {
            return;
        }

        for (Object rawEntry : rawList) {

            if (!(rawEntry instanceof java.util.Map<?, ?> rawMap)) {
                continue;
            }

            World world = Bukkit.getWorld(String.valueOf(rawMap.get("world")));

            if (world == null) {
                continue;
            }

            double x = toDouble(rawMap.get("x"));
            double y = toDouble(rawMap.get("y"));
            double z = toDouble(rawMap.get("z"));
            String filter = rawMap.get("filter") != null ? String.valueOf(rawMap.get("filter")) : "";

            warehouses.add(new Warehouse(new Location(world, x, y, z), filter));
        }
    }

    public void save() {

        YamlConfiguration config = new YamlConfiguration();
        List<java.util.Map<String, Object>> rawList = new ArrayList<>();

        for (Warehouse warehouse : warehouses) {

            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            Location location = warehouse.location();
            map.put("world", location.getWorld().getName());
            map.put("x", location.getBlockX() + 0.5);
            map.put("y", location.getBlockY());
            map.put("z", location.getBlockZ() + 0.5);
            map.put("filter", warehouse.resourceFilter());
            rawList.add(map);
        }

        config.set("warehouses", rawList);

        try {
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("✘ No se pudo guardar warehouses.yml: " + e.getMessage());
        }
    }

    public void designate(Location location, String resourceFilter) {
        warehouses.removeIf(w -> sameBlock(w.location(), location));
        warehouses.add(new Warehouse(location, resourceFilter));
        save();
    }

    public boolean remove(Location location) {

        boolean removed = warehouses.removeIf(w -> sameBlock(w.location(), location));

        if (removed) {
            save();
        }

        return removed;
    }

    public List<Warehouse> getAll() {
        return List.copyOf(warehouses);
    }

    public Optional<Warehouse> findNearest(Location from, double maxRadius) {

        return warehouses.stream()
                .filter(w -> w.location().getWorld().equals(from.getWorld()))
                .filter(w -> w.location().distanceSquared(from) <= maxRadius * maxRadius)
                .min(java.util.Comparator.comparingDouble(w -> w.location().distanceSquared(from)));
    }

    private boolean sameBlock(Location a, Location b) {
        return a.getWorld().equals(b.getWorld()) && a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private double toDouble(Object raw) {
        return raw instanceof Number number ? number.doubleValue() : 0;
    }

}
