package com.sack.rpgroll.dungeons.player;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DungeonPlayerStateManager {

    private final DungeonPlayerStateStore store;
    private final Map<UUID, DungeonPlayerState> cache = new ConcurrentHashMap<>();

    public DungeonPlayerStateManager(Plugin plugin) {
        this.store = new DungeonPlayerStateStore(plugin);
    }

    public DungeonPlayerState getOrLoad(UUID uuid) {
        return cache.computeIfAbsent(uuid, store::load);
    }

    public DungeonPlayerState getOrLoad(Player player) {
        return getOrLoad(player.getUniqueId());
    }

    public void save(UUID uuid) {

        DungeonPlayerState state = cache.get(uuid);

        if (state != null) {
            store.save(state);
        }
    }

    public void unload(UUID uuid) {
        save(uuid);
        cache.remove(uuid);
    }

    public void saveAll() {
        cache.keySet().forEach(this::save);
    }

}
