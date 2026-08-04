package com.sack.rpgroll.chat.language;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerLanguageStateManager {

    private final PlayerLanguageStateStore store;
    private final Map<UUID, PlayerLanguageState> cache = new ConcurrentHashMap<>();

    public PlayerLanguageStateManager(Plugin plugin) {
        this.store = new PlayerLanguageStateStore(plugin);
    }

    public PlayerLanguageState getOrLoad(UUID uuid) {
        return cache.computeIfAbsent(uuid, store::load);
    }

    public PlayerLanguageState getOrLoad(Player player) {
        return getOrLoad(player.getUniqueId());
    }

    public void save(UUID uuid) {
        PlayerLanguageState state = cache.get(uuid);
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
