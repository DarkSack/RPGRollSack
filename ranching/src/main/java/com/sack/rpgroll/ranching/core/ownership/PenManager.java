package com.sack.rpgroll.ranching.core.ownership;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** El corral de cada jugador: el sitio adonde "mandar al corral" lleva sus animales. En {@code pens.yml}. */
public class PenManager {

    private final Plugin plugin;
    private final File file;
    private final Map<UUID, Location> pens = new HashMap<>();

    public PenManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pens.yml");
    }

    public void load() {

        pens.clear();
        ConfigurationSection root = YamlConfiguration.loadConfiguration(file).getConfigurationSection("pens");

        if (root == null) {
            return;
        }

        for (String key : root.getKeys(false)) {

            ConfigurationSection pen = root.getConfigurationSection(key);
            World world = pen == null ? null : Bukkit.getWorld(pen.getString("world", ""));

            if (world == null) {
                continue;
            }

            try {
                pens.put(UUID.fromString(key), new Location(world, pen.getDouble("x"), pen.getDouble("y"),
                        pen.getDouble("z"), (float) pen.getDouble("yaw"), 0f));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public Optional<Location> get(UUID playerId) {
        Location pen = pens.get(playerId);
        return pen == null ? Optional.empty() : Optional.of(pen.clone());
    }

    public void set(UUID playerId, Location location) {
        pens.put(playerId, location.clone());
        save();
    }

    public boolean remove(UUID playerId) {
        boolean removed = pens.remove(playerId) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    private void save() {

        YamlConfiguration config = new YamlConfiguration();

        pens.forEach((id, pen) -> {
            String path = "pens." + id;
            config.set(path + ".world", pen.getWorld().getName());
            config.set(path + ".x", pen.getX());
            config.set(path + ".y", pen.getY());
            config.set(path + ".z", pen.getZ());
            config.set(path + ".yaw", pen.getYaw());
        });

        try {
            plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("✘ No se pudieron guardar los corrales: " + e.getMessage());
        }
    }

}
