package com.sack.rpgroll.chat.player;

import com.sack.rpgroll.chat.channel.ChannelManager;
import com.sack.rpgroll.chat.channel.ChannelScope;
import com.sack.rpgroll.chat.channel.ChatChannel;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerChannelStateManager {

    private final PlayerChannelStateStore store;
    private final ChannelManager channelManager;
    private final Map<UUID, PlayerChannelState> cache = new ConcurrentHashMap<>();

    public PlayerChannelStateManager(Plugin plugin, ChannelManager channelManager) {
        this.store = new PlayerChannelStateStore(plugin);
        this.channelManager = channelManager;
    }

    public PlayerChannelState getOrLoad(Player player) {

        PlayerChannelState state = cache.computeIfAbsent(player.getUniqueId(), store::load);

        if (state.joinedChannelIds().isEmpty() && state.activeChannelId() == null) {
            seedDefaults(state, player);
        }

        return state;
    }

    private void seedDefaults(PlayerChannelState state, Player player) {

        ChatChannel bestDefault = null;

        for (ChatChannel channel : channelManager.sortedByPriority()) {

            if (!channel.defaultJoined()) {
                continue;
            }

            if (channel.requiresViewPermission() && !player.hasPermission(channel.viewPermission())) {
                continue;
            }

            state.join(channel.id());

            boolean eligibleAsDefaultActive = !channel.requiresSpeakPermission()
                    && (channel.scope() == ChannelScope.GLOBAL || channel.scope() == ChannelScope.WORLD
                            || channel.scope() == ChannelScope.PROXIMITY);

            if (bestDefault == null && eligibleAsDefaultActive) {
                bestDefault = channel;
            }
        }

        if (bestDefault != null) {
            state.setActiveChannelId(bestDefault.id());
        }
    }

    public ChatChannel activeChannel(Player player) {

        PlayerChannelState state = getOrLoad(player);
        String activeId = state.activeChannelId();

        return activeId != null ? channelManager.get(activeId).orElse(null) : null;
    }

    public void save(UUID uuid) {
        PlayerChannelState state = cache.get(uuid);
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
