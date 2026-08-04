package com.sack.rpgroll.chat.ignore;

import com.sack.rpgroll.guilds.GuildsAPI;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IgnoreManager {

    private final IgnoreStateStore store;
    private final Map<UUID, PlayerIgnoreState> cache = new ConcurrentHashMap<>();

    public IgnoreManager(Plugin plugin) {
        this.store = new IgnoreStateStore(plugin);
    }

    public PlayerIgnoreState getOrLoad(UUID uuid) {
        return cache.computeIfAbsent(uuid, store::load);
    }

    public PlayerIgnoreState getOrLoad(Player player) {
        return getOrLoad(player.getUniqueId());
    }

    public void save(UUID uuid) {
        PlayerIgnoreState state = cache.get(uuid);
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

    /** @return true si {@code receiver} no debería ver un mensaje de {@code sender} en {@code channelId}. */
    public boolean blocks(Player receiver, Player sender, String channelId) {

        PlayerIgnoreState state = getOrLoad(receiver);

        if (state.isIgnoringPlayer(sender.getUniqueId()) || state.isIgnoringChannel(channelId)) {
            return true;
        }

        if (GuildsAPI.isReady()) {
            String guildId = GuildsAPI.getGuildManager().findByMember(sender.getUniqueId())
                    .map(guild -> guild.id())
                    .orElse(null);

            if (guildId != null && state.isIgnoringGuild(guildId)) {
                return true;
            }
        }

        return false;
    }

}
