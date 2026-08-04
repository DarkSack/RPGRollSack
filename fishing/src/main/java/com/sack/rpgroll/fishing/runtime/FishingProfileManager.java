package com.sack.rpgroll.fishing.runtime;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Caché en memoria de {@link PlayerFishingProfile}, con carga al entrar y guardado al salir. */
public class FishingProfileManager {

    private final FishingProfileStore store;
    private final Map<UUID, PlayerFishingProfile> cache = new ConcurrentHashMap<>();

    public FishingProfileManager(Plugin plugin) {
        this.store = new FishingProfileStore(plugin);
    }

    public PlayerFishingProfile getOrLoad(UUID uuid) {
        return cache.computeIfAbsent(uuid, store::load);
    }

    public PlayerFishingProfile getOrLoad(Player player) {
        return getOrLoad(player.getUniqueId());
    }

    public void save(UUID uuid) {

        PlayerFishingProfile profile = cache.get(uuid);

        if (profile != null) {
            store.save(profile);
        }
    }

    public void unload(UUID uuid) {
        save(uuid);
        cache.remove(uuid);
    }

    public void saveAll() {
        cache.values().forEach(store::save);
    }

}
