package com.sack.rpgroll.machines.area;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Todas las máquinas de área, en memoria y en {@code data/machines.yml} (las canteras siguen en su
 * propio fichero). Son pocas, así que se guardan enteras cuando algo cambia.
 */
public class AreaStore {

    private final File file;
    private final Logger logger;
    private final Map<String, AreaMachine> machines = new LinkedHashMap<>();
    private boolean dirty;

    public AreaStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    public Collection<AreaMachine> all() {
        return machines.values();
    }

    public Optional<AreaMachine> at(String key) {
        return Optional.ofNullable(machines.get(key));
    }

    public void add(AreaMachine machine) {
        machines.put(machine.key(), machine);
        dirty();
    }

    public void remove(AreaMachine machine) {
        machines.remove(machine.key());
        dirty();
    }

    public long count(UUID owner, String kind) {
        return machines.values().stream().filter(m -> m.owner().equals(owner) && m.kind().equals(kind)).count();
    }

    public void dirty() {
        dirty = true;
    }

    public void load() {
        machines.clear();
        if (!file.exists()) {
            return;
        }
        ConfigurationSection root = YamlConfiguration.loadConfiguration(file).getConfigurationSection("machines");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            try {
                AreaMachine machine = new AreaMachine(s.getString("kind", ""), s.getString("world"), s.getInt("x"),
                        s.getInt("y"), s.getInt("z"), UUID.fromString(s.getString("owner", "")),
                        s.getString("owner-name", "?"));
                ConfigurationSection levels = s.getConfigurationSection("levels");
                if (levels != null) {
                    for (String upgrade : levels.getKeys(false)) {
                        machine.level(upgrade, levels.getInt(upgrade));
                    }
                }
                machine.enabled(s.getBoolean("enabled", true));
                machine.cursor(s.getInt("cursor", 0));
                for (String encoded : s.getStringList("buffer")) {
                    machine.buffer().add(ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded)));
                }
                machines.put(machine.key(), machine);
            } catch (RuntimeException e) {
                logger.warning("✘ data/machines.yml: la máquina '" + id + "' no se pudo leer: " + e.getMessage());
            }
        }
    }

    /** Guarda si hubo cambios (o siempre, con {@code force}). */
    public void save(boolean force) {
        if (!dirty && !force) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(List.of(
                "Máquinas de área de RPGRoll-Machines. Lo escribe el plugin: no editar con el servidor encendido."));
        int i = 0;
        for (AreaMachine machine : machines.values()) {
            ConfigurationSection s = yaml.createSection("machines.m" + i++);
            s.set("kind", machine.kind());
            s.set("world", machine.world());
            s.set("x", machine.x());
            s.set("y", machine.y());
            s.set("z", machine.z());
            s.set("owner", machine.owner().toString());
            s.set("owner-name", machine.ownerName());
            machine.levels().forEach((upgrade, level) -> s.set("levels." + upgrade, level));
            s.set("enabled", machine.enabled());
            s.set("cursor", machine.cursor());
            List<String> buffer = new ArrayList<>();
            for (ItemStack item : machine.buffer()) {
                buffer.add(Base64.getEncoder().encodeToString(item.serializeAsBytes()));
            }
            s.set("buffer", buffer);
        }
        try {
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IOException("no se pudo crear " + parent);
            }
            File temp = new File(parent, file.getName() + ".tmp");
            yaml.save(temp);
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            dirty = false;
        } catch (IOException e) {
            logger.warning("✘ No se pudo guardar data/machines.yml: " + e.getMessage());
        }
    }
}
