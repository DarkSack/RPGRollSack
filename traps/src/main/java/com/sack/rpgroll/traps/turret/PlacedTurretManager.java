package com.sack.rpgroll.traps.turret;

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
 * Persiste las ubicaciones de torretas en su propio archivo
 * (turret-locations.yml, separado de locations.yml de trampas) — mismo
 * patrón que {@code PlacedTrapManager} pero sin campos de state machine.
 */
public class PlacedTurretManager {

    private final Plugin plugin;
    private final File file;
    private final Map<String, PlacedTurret> placements = new LinkedHashMap<>();

    public PlacedTurretManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "turret-locations.yml");
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
                PlacedTurret placed = new PlacedTurret(
                        placementId,
                        entry.getString("turret-id"),
                        entry.getString("world"),
                        entry.getInt("x"),
                        entry.getInt("y"),
                        entry.getInt("z"));

                placements.put(placementId, placed);

            } catch (Exception e) {
                plugin.getLogger().warning(
                        "✘ Ubicación de torreta inválida '" + placementId + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("✔ " + placements.size() + " torreta(s) colocada(s) cargadas.");
    }

    public void save() {

        YamlConfiguration config = new YamlConfiguration();

        for (PlacedTurret placed : placements.values()) {

            String path = "locations." + placed.placementId();

            config.set(path + ".turret-id", placed.turretId());
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
            plugin.getLogger().severe("✘ Error guardando turret-locations.yml: " + e.getMessage());
        }
    }

    public PlacedTurret add(String turretId, Location location) {

        String placementId = UUID.randomUUID().toString().substring(0, 8);

        PlacedTurret placed = new PlacedTurret(
                placementId, turretId, location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());

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

    public Optional<PlacedTurret> get(String placementId) {
        return Optional.ofNullable(placements.get(placementId));
    }

    public Collection<PlacedTurret> getAll() {
        return placements.values();
    }

}
