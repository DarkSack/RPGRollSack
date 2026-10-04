package com.sack.rpgroll.machines.quarry;

import com.sack.rpgroll.machines.quarry.QuarrySettings.Numeric;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Unlock;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Todas las canteras, en memoria y en {@code data/quarries.yml}. Son pocas (un tope por jugador),
 * así que se guardan enteras cuando algo cambia, como mucho una vez cada pocos segundos.
 */
public class QuarryStore {

    private final Plugin plugin;
    private final File file;
    private final Map<String, Quarry> quarries = new LinkedHashMap<>();
    private boolean dirty;

    public QuarryStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/quarries.yml");
    }

    public Collection<Quarry> all() {
        return quarries.values();
    }

    public Optional<Quarry> at(String key) {
        return Optional.ofNullable(quarries.get(key));
    }

    public void add(Quarry quarry) {
        quarries.put(quarry.key(), quarry);
        dirty();
    }

    public void remove(Quarry quarry) {
        quarries.remove(quarry.key());
        dirty();
    }

    public long count(UUID owner) {
        return quarries.values().stream().filter(q -> q.owner().equals(owner)).count();
    }

    public void dirty() {
        dirty = true;
    }

    public void load() {

        quarries.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("quarries");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            try {
                Quarry quarry = new Quarry(s.getString("world"), s.getInt("x"), s.getInt("y"), s.getInt("z"),
                        UUID.fromString(s.getString("owner", "")), s.getString("owner-name", "?"));
                for (Numeric numeric : Numeric.values()) {
                    quarry.level(numeric, s.getInt("levels." + numeric.id(), 0));
                }
                for (Unlock unlock : Unlock.values()) {
                    if (s.getBoolean("unlocked." + unlock.id(), false)) {
                        quarry.unlock(unlock, s.getBoolean("active." + unlock.id(), true));
                    }
                }
                quarry.enabled(s.getBoolean("enabled", true));
                quarry.cursor(s.getInt("cursor.y", quarry.y() - 1), s.getInt("cursor.index", 0));
                quarry.finished(s.getBoolean("finished", false));
                for (String encoded : s.getStringList("buffer")) {
                    quarry.buffer().add(ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded)));
                }
                quarries.put(quarry.key(), quarry);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("✘ data/quarries.yml: la cantera '" + id + "' no se pudo leer: " + e.getMessage());
            }
        }
    }

    /** Guarda si hubo cambios (o siempre, con {@code force}). */
    public void save(boolean force) {

        if (!dirty && !force) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(List.of("Canteras de RPGRoll-Machines. Lo escribe el plugin: no editar con el servidor encendido."));
        int i = 0;
        for (Quarry quarry : quarries.values()) {
            ConfigurationSection s = yaml.createSection("quarries.q" + i++);
            s.set("world", quarry.world());
            s.set("x", quarry.x());
            s.set("y", quarry.y());
            s.set("z", quarry.z());
            s.set("owner", quarry.owner().toString());
            s.set("owner-name", quarry.ownerName());
            for (Numeric numeric : Numeric.values()) {
                s.set("levels." + numeric.id(), quarry.level(numeric));
            }
            for (Unlock unlock : Unlock.values()) {
                if (quarry.unlocked(unlock)) {
                    s.set("unlocked." + unlock.id(), true);
                    s.set("active." + unlock.id(), quarry.active(unlock));
                }
            }
            s.set("enabled", quarry.enabled());
            s.set("cursor.y", quarry.cursorY());
            s.set("cursor.index", quarry.cursorIndex());
            s.set("finished", quarry.finished());
            List<String> buffer = new ArrayList<>();
            for (ItemStack item : quarry.buffer()) {
                buffer.add(Base64.getEncoder().encodeToString(item.serializeAsBytes()));
            }
            s.set("buffer", buffer);
        }
        try {
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IOException("no se pudo crear " + parent);
            }
            File temp = new File(parent, "quarries.yml.tmp");
            yaml.save(temp);
            java.nio.file.Files.move(temp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("✘ No se pudo guardar data/quarries.yml: " + e.getMessage());
        }
    }
}
