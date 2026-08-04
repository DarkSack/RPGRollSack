package com.sack.rpgroll.quests.player;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caché en memoria de {@link QuestPlayerState}, con carga al entrar y
 * guardado al salir — mismo patrón que PlayerCache/PlayerManager de :core,
 * pero self-contained (sin BD).
 */
public class QuestPlayerStateManager {

    private final QuestPlayerStateStore store;
    private final Map<UUID, QuestPlayerState> cache = new ConcurrentHashMap<>();

    public QuestPlayerStateManager(Plugin plugin) {
        this.store = new QuestPlayerStateStore(plugin);
    }

    public QuestPlayerState getOrLoad(UUID uuid) {
        return cache.computeIfAbsent(uuid, store::load);
    }

    public QuestPlayerState getOrLoad(Player player) {
        return getOrLoad(player.getUniqueId());
    }

    public void save(UUID uuid) {

        QuestPlayerState state = cache.get(uuid);

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
