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
                        entry.getInt("z"),
                        entry.getString("owner") == null
                                ? null
                                : UUID.fromString(entry.getString("owner")),
                        readAmmo(entry),
                        readTargeting(entry));

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

            if (placed.owner() != null) {
                config.set(path + ".owner", placed.owner().toString());
            }

            for (var slot : placed.ammo().entrySet()) {
                config.set(path + ".ammo." + slot.getKey(), slot.getValue());
            }

            config.set(path + ".targeting.allies", placed.targeting().allies());
            config.set(path + ".targeting.enemies", placed.targeting().enemies());
            config.set(path + ".targeting.hostile-mobs", placed.targeting().hostileMobs());
            config.set(path + ".targeting.passive-mobs", placed.targeting().passiveMobs());
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
        return add(turretId, location, null, new TurretTargeting(false, true, true, false));
    }

    /** @param owner quién la coloca; null si viene de un comando de admin. */
    public PlacedTurret add(String turretId, Location location, UUID owner, TurretTargeting targeting) {

        String placementId = UUID.randomUUID().toString().substring(0, 8);

        PlacedTurret placed = new PlacedTurret(
                placementId, turretId, location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ(), owner, java.util.Map.of(), targeting);

        placements.put(placementId, placed);
        save();

        return placed;
    }

    /** Sin sección "targeting" (instancias viejas) se asume el comportamiento previo. */
    private TurretTargeting readTargeting(ConfigurationSection entry) {

        ConfigurationSection section = entry.getConfigurationSection("targeting");

        if (section == null) {
            return new TurretTargeting(false, true, true, false);
        }

        return new TurretTargeting(
                section.getBoolean("allies", false),
                section.getBoolean("enemies", true),
                section.getBoolean("hostile-mobs", true),
                section.getBoolean("passive-mobs", false));
    }

    /** Cambia a quién apunta una instancia y persiste. */
    public void setTargeting(String placementId, TurretTargeting targeting) {

        PlacedTurret current = placements.get(placementId);

        if (current == null) {
            return;
        }

        placements.put(placementId, new PlacedTurret(current.placementId(), current.turretId(),
                current.world(), current.x(), current.y(), current.z(), current.owner(),
                current.ammo(), targeting));

        save();
    }

    private java.util.Map<String, Integer> readAmmo(ConfigurationSection entry) {

        ConfigurationSection section = entry.getConfigurationSection("ammo");

        if (section == null) {
            return java.util.Map.of();
        }

        java.util.Map<String, Integer> ammo = new java.util.HashMap<>();

        for (String ammoId : section.getKeys(false)) {
            int amount = section.getInt(ammoId);
            if (amount > 0) {
                ammo.put(ammoId, amount);
            }
        }

        return ammo;
    }

    /**
     * Reemplaza la munición de una instancia y persiste.
     * <p>
     * Se guarda en cada cambio a propósito: la munición es lo que hace que
     * una torreta dispare, y perderla en una caída del server sería peor que
     * el costo de escribir el archivo.
     */
    public void setAmmo(String placementId, java.util.Map<String, Integer> ammo) {

        PlacedTurret current = placements.get(placementId);

        if (current == null) {
            return;
        }

        placements.put(placementId, new PlacedTurret(current.placementId(), current.turretId(),
                current.world(), current.x(), current.y(), current.z(), current.owner(), ammo,
                current.targeting()));

        save();
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
