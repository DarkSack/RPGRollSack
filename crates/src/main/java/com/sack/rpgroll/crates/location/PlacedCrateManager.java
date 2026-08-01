package com.sack.rpgroll.crates.location;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Persiste todas las ubicaciones físicas de crates en un único archivo
 * (locations.yml). A diferencia de Crate (un archivo por tipo, vía
 * ContentManager de :common), acá tiene más sentido un solo archivo:
 * suele haber muchas ubicaciones repartidas por pocos tipos de crate.
 */
public class PlacedCrateManager {

    private final Plugin plugin;
    private final File file;
    private final Map<String, PlacedCrate> placements = new LinkedHashMap<>();

    public PlacedCrateManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "locations.yml");
    }

    public void load() {

        placements.clear();

        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("locations");

        if (section == null) {
            return;
        }

        for (String placementId : section.getKeys(false)) {

            ConfigurationSection entry = section.getConfigurationSection(placementId);
            if (entry == null) {
                continue;
            }

            try {

                PlacedCrate placed = new PlacedCrate(
                        placementId,
                        entry.getString("crate-id"),
                        entry.getString("world"),
                        entry.getInt("x"),
                        entry.getInt("y"),
                        entry.getInt("z"));

                placements.put(placementId, placed);

            } catch (Exception e) {
                plugin.getLogger().warning(
                        "✘ Ubicación de crate inválida '" + placementId + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("✔ " + placements.size() + " ubicación(es) de crate cargadas.");
    }

    public void save() {

        YamlConfiguration config = new YamlConfiguration();

        for (PlacedCrate placed : placements.values()) {

            String path = "locations." + placed.placementId();

            config.set(path + ".crate-id", placed.crateId());
            config.set(path + ".world", placed.world());
            config.set(path + ".x", placed.x());
            config.set(path + ".y", placed.y());
            config.set(path + ".z", placed.z());
        }

        try {

            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            config.save(file);

        } catch (IOException e) {
            plugin.getLogger().severe("✘ Error guardando locations.yml: " + e.getMessage());
        }
    }

    public PlacedCrate add(String crateId, Location location) {

        String placementId = UUID.randomUUID().toString().substring(0, 8);

        PlacedCrate placed = new PlacedCrate(
                placementId,
                crateId,
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());

        placements.put(placementId, placed);
        save();

        return placed;
    }

    public boolean remove(String placementId) {

        boolean removed = placements.remove(placementId) != null;

        if (removed) {
            save();
        }

        return removed;
    }

    public Optional<PlacedCrate> get(String placementId) {
        return Optional.ofNullable(placements.get(placementId));
    }

    public Optional<PlacedCrate> findAt(Location location) {

        if (location.getWorld() == null) {
            return Optional.empty();
        }

        String world = location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return placements.values().stream()
                .filter(p -> p.world().equals(world) && p.x() == x && p.y() == y && p.z() == z)
                .findFirst();
    }

    public Collection<PlacedCrate> getAll() {
        return placements.values();
    }

}
