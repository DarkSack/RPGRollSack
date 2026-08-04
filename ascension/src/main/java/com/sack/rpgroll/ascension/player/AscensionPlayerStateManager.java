package com.sack.rpgroll.ascension.player;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AscensionPlayerStateManager {

    private final AscensionPlayerStateStore store;
    private final Map<UUID, AscensionPlayerState> cache = new ConcurrentHashMap<>();

    public AscensionPlayerStateManager(Plugin plugin) {
        this.store = new AscensionPlayerStateStore(plugin);
    }

    public AscensionPlayerState getOrLoad(UUID uuid) {
        return cache.computeIfAbsent(uuid, store::load);
    }

    public AscensionPlayerState getOrLoad(Player player) {
        return getOrLoad(player.getUniqueId());
    }

    public void save(UUID uuid) {

        AscensionPlayerState state = cache.get(uuid);

        if (state != null) {
            store.save(state);
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
