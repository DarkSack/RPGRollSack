package com.sack.rpgroll.extras.backpack;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Un archivo por mochila en data/backpacks/&lt;uuid&gt;.yml. Cada ítem se guarda
 * con el formato binario de Paper (serializeAsBytes), que conserva todo su
 * NBT, y se cachea mientras alguien la usa.
 */
public class BackpackStorage {

    private final File folder;
    private final Logger logger;
    private final Map<UUID, BackpackData> cache = new HashMap<>();
    private final Map<UUID, Map<String, Object>> unreadable = new HashMap<>();

    public BackpackStorage(File dataFolder, Logger logger) {
        this.folder = new File(dataFolder, "data/backpacks");
        this.logger = logger;
    }

    public BackpackData get(UUID id) {
        return cache.computeIfAbsent(id, this::load);
    }

    /** Suelta la copia en memoria (tras guardarla) cuando nadie la usa. */
    public void evict(UUID id) {
        cache.remove(id);
        unreadable.remove(id);
    }

    private BackpackData load(UUID id) {

        File file = file(id);

        if (!file.isFile()) {
            return new BackpackData(id, null, null, new ItemStack[0]);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        UUID owner = null;

        try {
            String raw = yaml.getString("owner");
            owner = raw == null || raw.isEmpty() ? null : UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            logger.warning("Mochila " + id + ": dueño ilegible, queda sin ligar.");
        }

        ConfigurationSection section = yaml.getConfigurationSection("items");
        int size = yaml.getInt("size", 0);
        ItemStack[] items = new ItemStack[size];
        BackpackData data = new BackpackData(id, owner, owner == null ? null : yaml.getString("owner-name", "?"), items);
        Map<String, Object> lost = new HashMap<>();

        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int index = Integer.parseInt(key);
                    data.set(index, ItemStack.deserializeBytes(Base64.getDecoder().decode(section.getString(key, ""))));
                } catch (RuntimeException e) {
                    // Un ítem de un plugin que ya no existe no debe vaciar la mochila entera.
                    logger.log(Level.WARNING, "Mochila " + id + ": no se pudo leer el espacio " + key
                            + "; se conserva en el archivo, bajo unreadable.", e);
                    lost.put(key, section.getString(key));
                }
            }
        }

        ConfigurationSection previous = yaml.getConfigurationSection("unreadable");
        if (previous != null) {
            previous.getValues(false).forEach((key, value) -> lost.putIfAbsent(key, value));
        }
        if (!lost.isEmpty()) {
            unreadable.put(id, lost);
        }

        return data;
    }

    public void save(BackpackData data) {

        YamlConfiguration yaml = new YamlConfiguration();
        File file = file(data.id());

        // Lo ilegible se arrastra: puede volver a leerse si vuelve el plugin que lo creó.
        unreadable.getOrDefault(data.id(), Map.of()).forEach((key, value) -> yaml.set("unreadable." + key, value));

        if (data.isBound()) {
            yaml.set("owner", data.owner().toString());
            yaml.set("owner-name", data.ownerName());
        }

        ItemStack[] items = data.items();
        yaml.set("size", items.length);

        for (int i = 0; i < items.length; i++) {
            if (items[i] != null && !items[i].getType().isAir()) {
                yaml.set("items." + i, Base64.getEncoder().encodeToString(items[i].serializeAsBytes()));
            }
        }

        try {
            folder.mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "No se pudo guardar la mochila " + data.id(), e);
        }
    }

    public void saveAll() {
        cache.values().forEach(this::save);
    }

    private File file(UUID id) {
        return new File(folder, id + ".yml");
    }

}
