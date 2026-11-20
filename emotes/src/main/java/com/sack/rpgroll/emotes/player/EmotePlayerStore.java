package com.sack.rpgroll.emotes.player;

import com.sack.rpgroll.common.yaml.YamlSaver;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Un YAML por jugador en {@code data/players/<uuid>.yml} con las claves
 * {@code unlocked}, {@code favorites} (un hueco vacío se guarda como cadena vacía)
 * y {@code last-use}.
 */
public class EmotePlayerStore {

    /** Huecos de favoritos por defecto, si la config trae un valor no válido. */
    public static final int DEFAULT_MAX_FAVORITES = 7;

    private static final Logger LOGGER = Logger.getLogger("RPGRoll-Emotes");

    private final File folder;
    private final int maxFavorites;
    private final Map<UUID, EmotePlayer> loaded = new ConcurrentHashMap<>();
    // Jugadores cuyo YAML no se pudo leer ni apartar: no se guardan para no pisar el original.
    private final Set<UUID> protectedFiles = ConcurrentHashMap.newKeySet();

    public EmotePlayerStore(File dataFolder, int maxFavorites) {
        this.folder = new File(dataFolder, "data/players");
        this.maxFavorites = maxFavorites <= 0 ? DEFAULT_MAX_FAVORITES : maxFavorites;
    }

    /** Lo carga de disco, o lo crea vacío si no hay archivo. */
    public EmotePlayer load(UUID uuid) {
        protectedFiles.remove(uuid);
        EmotePlayer player = read(uuid);
        loaded.put(uuid, player);
        return player;
    }

    /** El jugador cargado; si no lo está, lo carga. */
    public EmotePlayer get(UUID uuid) {
        return loaded.computeIfAbsent(uuid, this::read);
    }

    public void save(UUID uuid) {
        EmotePlayer player = loaded.get(uuid);
        if (player != null) {
            write(player);
        }
    }

    /** Lo guarda y lo saca de memoria. */
    public void unload(UUID uuid) {
        EmotePlayer player = loaded.remove(uuid);
        if (player != null) {
            write(player);
        }
        protectedFiles.remove(uuid);
    }

    /** true si su YAML no se pudo leer ni apartar: {@code save} no escribe y el original se conserva. */
    public boolean isProtected(UUID uuid) {
        return protectedFiles.contains(uuid);
    }

    public void saveAll() {
        loaded.values().forEach(this::write);
    }

    private void write(EmotePlayer player) {
        if (protectedFiles.contains(player.uuid())) {
            LOGGER.warning("No se guardan los emotes de " + player.uuid()
                    + ": su archivo no se pudo leer y se conserva tal cual.");
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("unlocked", new ArrayList<>(player.unlocked()));
        List<String> favorites = new ArrayList<>();
        for (String id : player.favorites()) {
            favorites.add(id == null ? "" : id);
        }
        yaml.set("favorites", favorites);
        yaml.set("last-use", player.lastUseMillis());

        try {
            folder.mkdirs();
            YamlSaver.save(yaml, file(player.uuid()));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "No se pudieron guardar los emotes de " + player.uuid(), e);
        }
    }

    private EmotePlayer read(UUID uuid) {
        EmotePlayer player = new EmotePlayer(uuid, maxFavorites);
        File file = file(uuid);
        if (!file.exists()) {
            return player;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (InvalidConfigurationException e) {
            // No se sobrescribe: el archivo puede traer emotes pagados.
            File backup = new File(folder, uuid + ".yml.corrupto-" + System.currentTimeMillis());
            boolean moved = rename(file, backup);
            LOGGER.log(Level.WARNING, "YAML de emotes ilegible de " + uuid
                    + (moved ? "; copia en " + backup.getName() : "; no se pudo apartar, no se guardará"), e);
            if (!moved) {
                protectedFiles.add(uuid);
            }
            return player;
        } catch (IOException e) {
            // Fallo de lectura, no de contenido: el archivo puede estar sano, no se toca.
            LOGGER.log(Level.WARNING, "No se pudo leer el YAML de emotes de " + uuid + "; no se guardará", e);
            protectedFiles.add(uuid);
            return player;
        }
        yaml.getStringList("unlocked").forEach(player::unlock);
        List<String> favorites = yaml.getStringList("favorites");
        for (int i = 0; i < favorites.size(); i++) {
            player.setFavoriteSlot(i, favorites.get(i));
        }
        player.lastUseMillis(yaml.getLong("last-use"));
        return player;
    }

    /** Aparta el archivo; sobrescribible en los tests para simular un fallo. */
    boolean rename(File from, File to) {
        return from.renameTo(to);
    }

    private File file(UUID uuid) {
        return new File(folder, uuid + ".yml");
    }

}
