package com.sack.rpgroll.player.cache;

import com.sack.rpgroll.player.RPGPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlayerCache mantiene jugadores en memoria para acceso rápido.
 * 
 * Responsabilidades:
 * - Caché de jugadores activos en servidor
 * - Actualizar caché cuando cambian datos
 * - Evitar consultas innecesarias a BD
 * - Thread-safe con ConcurrentHashMap
 */
public class PlayerCache {

    // Cache por UUID
    private final Map<UUID, RPGPlayer> playersByUUID = new ConcurrentHashMap<>();

    // Cache inverso: UUID -> Minecraft Player Name
    private final Map<String, UUID> uuidByUsername = new ConcurrentHashMap<>();

    /**
     * Agrega un jugador al caché.
     */
    public void cache(RPGPlayer player) {
        playersByUUID.put(player.getUUID(), player);
        uuidByUsername.put(player.getUsername(), player.getUUID());
    }

    /**
     * Obtiene un jugador del caché por UUID.
     */
    public Optional<RPGPlayer> getByUUID(UUID uuid) {
        return Optional.ofNullable(playersByUUID.get(uuid));
    }

    /**
     * Obtiene un jugador del caché por nombre de usuario.
     */
    public Optional<RPGPlayer> getByUsername(String username) {
        UUID uuid = uuidByUsername.get(username);
        if (uuid == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(playersByUUID.get(uuid));
    }

    /**
     * Remueve un jugador del caché.
     */
    public void remove(UUID uuid) {
        RPGPlayer player = playersByUUID.remove(uuid);
        if (player != null) {
            uuidByUsername.remove(player.getUsername());
        }
    }

    /**
     * Actualiza un jugador en el caché.
     * Se llama después de modificar datos del jugador.
     */
    public void update(RPGPlayer player) {
        playersByUUID.put(player.getUUID(), player);
        uuidByUsername.put(player.getUsername(), player.getUUID());
    }

    /**
     * Verifica si un jugador está en caché.
     */
    public boolean contains(UUID uuid) {
        return playersByUUID.containsKey(uuid);
    }

    /**
     * Obtiene todos los jugadores en caché.
     */
    public Collection<RPGPlayer> getAll() {
        return Collections.unmodifiableCollection(playersByUUID.values());
    }

    /**
     * Limpia completamente el caché.
     * Usar al apagar el servidor.
     */
    public void clear() {
        playersByUUID.clear();
        uuidByUsername.clear();
    }

    /**
     * Obtiene el número de jugadores en caché.
     */
    public int size() {
        return playersByUUID.size();
    }

}
