package com.sack.rpgroll.traps.location;

import com.sack.rpgroll.traps.core.TrapState;

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
 * Persiste todas las ubicaciones físicas de trampas en un único archivo
 * (locations.yml), incluyendo su estado de instancia — a diferencia de
 * PlacedCrateManager (solo posición), acá el estado importa: al reiniciar
 * el server una trampa DEPLETED o en COOLDOWN debe seguir así, no volver a
 * ARMED de gratis.
 */
public class PlacedTrapManager {

    private final Plugin plugin;
    private final File file;
    private final Map<String, PlacedTrap> placements = new LinkedHashMap<>();

    public PlacedTrapManager(Plugin plugin) {
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

                Integer x2 = entry.contains("x2") ? entry.getInt("x2") : null;
                Integer y2 = entry.contains("y2") ? entry.getInt("y2") : null;
                Integer z2 = entry.contains("z2") ? entry.getInt("z2") : null;

                TrapState state;
                try {
                    state = TrapState.valueOf(entry.getString("state", "ARMED"));
                } catch (IllegalArgumentException e) {
                    state = TrapState.ARMED;
                }

                PlacedTrap placed = new PlacedTrap(
                        placementId,
                        entry.getString("trap-id"),
                        entry.getString("world"),
                        entry.getInt("x"),
                        entry.getInt("y"),
                        entry.getInt("z"),
                        x2, y2, z2,
                        state,
                        entry.getInt("charges-remaining", -1),
                        entry.getLong("cooldown-expires-at", 0));

                placements.put(placementId, placed);

            } catch (Exception e) {
                plugin.getLogger().warning(
                        "✘ Ubicación de trampa inválida '" + placementId + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("✔ " + placements.size() + " ubicación(es) de trampa cargadas.");
    }

    public void save() {

        YamlConfiguration config = new YamlConfiguration();

        for (PlacedTrap placed : placements.values()) {

            String path = "locations." + placed.placementId();

            config.set(path + ".trap-id", placed.trapId());
            config.set(path + ".world", placed.world());
            config.set(path + ".x", placed.x());
            config.set(path + ".y", placed.y());
            config.set(path + ".z", placed.z());

            if (placed.isZone()) {
                config.set(path + ".x2", placed.x2());
                config.set(path + ".y2", placed.y2());
                config.set(path + ".z2", placed.z2());
            }

            config.set(path + ".state", placed.state().name());
            config.set(path + ".charges-remaining", placed.chargesRemaining());
            config.set(path + ".cooldown-expires-at", placed.cooldownExpiresAtMillis());
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

    public PlacedTrap addPoint(String trapId, Location location, int initialCharges) {

        String placementId = UUID.randomUUID().toString().substring(0, 8);

        PlacedTrap placed = new PlacedTrap(
                placementId, trapId, location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                null, null, null,
                TrapState.ARMED, initialCharges, 0);

        placements.put(placementId, placed);
        save();

        return placed;
    }

    public PlacedTrap addZone(String trapId, Location cornerA, Location cornerB, int initialCharges) {

        String placementId = UUID.randomUUID().toString().substring(0, 8);

        PlacedTrap placed = new PlacedTrap(
                placementId, trapId, cornerA.getWorld().getName(),
                cornerA.getBlockX(), cornerA.getBlockY(), cornerA.getBlockZ(),
                cornerB.getBlockX(), cornerB.getBlockY(), cornerB.getBlockZ(),
                TrapState.ARMED, initialCharges, 0);

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

    public Optional<PlacedTrap> get(String placementId) {
        return Optional.ofNullable(placements.get(placementId));
    }

    /** Actualiza y persiste una instancia existente (usado por el motor tras cada transición de estado). */
    public void update(PlacedTrap placed) {
        placements.put(placed.placementId(), placed);
        save();
    }

    public Optional<PlacedTrap> findAt(Location location) {
        return PlacedTrap.tryFind(placements.values(), location);
    }

    /** Todas las instancias cuyo punto/zona contiene esta location — dos trampas pueden solaparse a propósito (ej. una protege el bloque, otra dispara al pisarlo). */
    public java.util.List<PlacedTrap> findAllAt(Location location) {
        return placements.values().stream().filter(p -> p.contains(location)).toList();
    }

    public Collection<PlacedTrap> getAll() {
        return placements.values();
    }

}
