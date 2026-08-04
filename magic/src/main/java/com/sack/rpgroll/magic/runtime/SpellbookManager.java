package com.sack.rpgroll.magic.runtime;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caché en memoria de {@link PlayerSpellbook}, con carga al entrar y
 * guardado al salir — mismo patrón que QuestPlayerStateManager.
 */
public class SpellbookManager {

    private final SpellbookStore store;
    private final Map<UUID, PlayerSpellbook> cache = new ConcurrentHashMap<>();

    public SpellbookManager(Plugin plugin) {
        this.store = new SpellbookStore(plugin);
    }

    public PlayerSpellbook getOrLoad(UUID uuid) {
        return cache.computeIfAbsent(uuid, store::load);
    }

    public PlayerSpellbook getOrLoad(Player player) {
        return getOrLoad(player.getUniqueId());
    }

    public void save(UUID uuid) {

        PlayerSpellbook spellbook = cache.get(uuid);

        if (spellbook != null) {
            store.save(spellbook);
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
