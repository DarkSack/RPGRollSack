package com.sack.rpgroll.player;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.database.DatabaseManager;
import com.sack.rpgroll.player.cache.PlayerCache;
import com.sack.rpgroll.player.repository.PlayerRepository;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * PlayerManager es el servicio principal para gestionar jugadores RPG.
 * 
 * Responsabilidades:
 * - Coordinar PlayerRepository (BD) y PlayerCache (memoria)
 * - Cargar jugadores al entrar
 * - Guardar jugadores al salir
 * - Actualizar datos en tiempo real
 * - Punto de entrada único para operaciones de jugador
 */
public class PlayerManager {

    private final RPGRoll plugin;
    private final PlayerRepository repository;
    private final PlayerCache cache;

    public PlayerManager(RPGRoll plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.repository = new PlayerRepository(plugin, databaseManager);
        this.cache = new PlayerCache();
    }

    /**
     * Carga o crea un jugador.
     * 
     * Flujo:
     * 1. Buscar en caché
     * 2. Si no está, buscar en BD
     * 3. Si no existe, crear nuevo
     * 4. Guardar en caché
     * 
     * @param bukkit el jugador de Minecraft
     * @return el RPGPlayer
     */
    public RPGPlayer loadOrCreate(Player bukkit) {

        UUID uuid = bukkit.getUniqueId();

        // 1. Buscar en caché
        Optional<RPGPlayer> cached = cache.getByUUID(uuid);
        if (cached.isPresent()) {
            plugin.getLogger().info("✔ Jugador en caché: " + bukkit.getName());
            return cached.get();
        }

        // 2. Buscar en BD
        Optional<RPGPlayer> fromDatabase = repository.findByUUID(uuid);
        if (fromDatabase.isPresent()) {
            RPGPlayer player = fromDatabase.get();
            cache.cache(player);
            plugin.getLogger().info("✔ Jugador cargado de BD: " + bukkit.getName());
            return player;
        }

        // 3. Crear nuevo
        RPGPlayer newPlayer = RPGPlayer.createNew(bukkit);
        if (repository.save(newPlayer)) {
            cache.cache(newPlayer);
            plugin.getLogger().info("✔ Jugador creado: " + bukkit.getName());
            return newPlayer;
        }

        plugin.getLogger().severe("Error al crear jugador: " + bukkit.getName());
        return newPlayer; // Devolver el jugador aunque falló guardar (mejor que null)

    }

    /**
     * Obtiene un jugador por UUID.
     * Primero busca en caché, luego en BD.
     */
    public Optional<RPGPlayer> getPlayer(UUID uuid) {

        // Buscar en caché
        Optional<RPGPlayer> cached = cache.getByUUID(uuid);
        if (cached.isPresent()) {
            return cached;
        }

        // Buscar en BD
        Optional<RPGPlayer> fromDatabase = repository.findByUUID(uuid);
        if (fromDatabase.isPresent()) {
            RPGPlayer player = fromDatabase.get();
            cache.cache(player);
            return Optional.of(player);
        }

        return Optional.empty();

    }

    /**
     * Obtiene un jugador por nombre.
     */
    public Optional<RPGPlayer> getPlayerByName(String username) {
        return cache.getByUsername(username);
    }

    /**
     * Guarda cambios de un jugador en la BD.
     * También actualiza el caché.
     */
    public void savePlayer(RPGPlayer player) {

        // Guardar en BD
        if (repository.update(player)) {
            // Actualizar caché
            cache.update(player);
            plugin.getLogger().info("✔ Jugador guardado: " + player.getUsername());
        } else {
            plugin.getLogger().warning("⚠ Error al guardar jugador: " + player.getUsername());
        }

    }

    /**
     * Guarda todos los jugadores en caché a la BD.
     * Útil para shutdown.
     */
    public void saveAll() {

        int count = 0;

        for (RPGPlayer player : cache.getAll()) {
            if (repository.update(player)) {
                count++;
            }
        }

        plugin.getLogger().info("✔ " + count + " jugadores guardados");

    }

    /**
     * Descarga un jugador del caché y guarda en BD.
     * Se llama cuando el jugador se desconecta.
     */
    public void unloadPlayer(UUID uuid) {

        Optional<RPGPlayer> player = cache.getByUUID(uuid);

        if (player.isPresent()) {
            // Guardar en BD antes de descargar
            repository.update(player.get());
        }

        // Remover del caché
        cache.remove(uuid);

    }

    /**
     * Obtiene el caché (solo lectura).
     */
    public PlayerCache getCache() {
        return cache;
    }

    /**
     * Obtiene el repositorio (solo lectura).
     */
    public PlayerRepository getRepository() {
        return repository;
    }

}
