package com.sack.rpgroll.sackresourcepack.cmd;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Asigna un CustomModelData a una "clave lógica" libre (ej. {@code
 * "rpgroll_fishing:fish_salmon"}) la primera vez que se pide, y esa
 * asignación queda fija para siempre en {@code cmd-registry.yml} — nunca
 * se reasigna un número existente, así que el mismo item ya entregado a
 * un jugador sigue mostrando el modelo correcto después de cualquier
 * rebuild futuro.
 */
public class CustomModelDataManager {

    private final Plugin plugin;
    private final File registryFile;
    private final Map<String, Integer> assigned = new LinkedHashMap<>();
    private int nextValue;

    public CustomModelDataManager(Plugin plugin, int base) {
        this.plugin = plugin;
        this.registryFile = new File(plugin.getDataFolder(), "cmd-registry.yml");
        this.nextValue = base;
        load();
    }

    private void load() {

        if (!registryFile.isFile()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(registryFile);

        for (String key : config.getKeys(false)) {

            int value = config.getInt(key);
            assigned.put(key, value);
            nextValue = Math.max(nextValue, value + 1);
        }
    }

    /** Devuelve la asignación existente, o crea y persiste una nueva. */
    public synchronized int allocate(String key) {

        Integer existing = assigned.get(key);

        if (existing != null) {
            return existing;
        }

        int value = nextValue++;
        assigned.put(key, value);
        save();

        return value;
    }

    public Optional<Integer> get(String key) {
        return Optional.ofNullable(assigned.get(key));
    }

    public Map<String, Integer> allAssigned() {
        return assigned;
    }

    private void save() {

        YamlConfiguration config = new YamlConfiguration();

        for (var entry : assigned.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }

        try {
            config.save(registryFile);
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error guardando cmd-registry.yml: " + e.getMessage());
        }
    }

}
