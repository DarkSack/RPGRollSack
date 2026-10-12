package com.sack.rpgroll.extras.travel;

import com.sack.rpgroll.common.yaml.YamlSaver;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/** Los warps de los jugadores, en {@code warps.yml}. Se guarda en cada cambio: son pocos y cambian poco. */
public class Warps {

    /** El mundo va por nombre: puede no estar cargado cuando se lee el archivo. */
    public record Warp(String name, UUID owner, String ownerName, String world, double x, double y, double z,
            float yaw, float pitch) {
    }

    private final File file;
    private final Map<String, Warp> warps = new TreeMap<>();

    public Warps(File file) {
        this.file = file;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String name : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(name);
            if (section == null || section.getString("owner") == null || section.getString("world") == null) {
                continue;
            }
            warps.put(name, new Warp(name, UUID.fromString(section.getString("owner")),
                    section.getString("owner-name", "?"), section.getString("world"), section.getDouble("x"),
                    section.getDouble("y"), section.getDouble("z"), (float) section.getDouble("yaw"),
                    (float) section.getDouble("pitch")));
        }
    }

    public Optional<Warp> get(String name) {
        return Optional.ofNullable(warps.get(name));
    }

    public Iterable<Warp> all() {
        return warps.values();
    }

    public long ownedBy(UUID owner) {
        return warps.values().stream().filter(warp -> warp.owner().equals(owner)).count();
    }

    public void put(Warp warp) throws IOException {
        warps.put(warp.name(), warp);
        save();
    }

    public void remove(String name) throws IOException {
        warps.remove(name);
        save();
    }

    private void save() throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Warp warp : warps.values()) {
            ConfigurationSection section = yaml.createSection(warp.name());
            section.set("owner", warp.owner().toString());
            section.set("owner-name", warp.ownerName());
            section.set("world", warp.world());
            section.set("x", warp.x());
            section.set("y", warp.y());
            section.set("z", warp.z());
            section.set("yaw", warp.yaw());
            section.set("pitch", warp.pitch());
        }
        YamlSaver.save(yaml, file);
    }

}
