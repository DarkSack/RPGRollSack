package com.sack.rpgroll.chat.player;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Qué canales tiene unidos un jugador y cuál está activo para el chat normal (sin prefijo). */
public class PlayerChannelState {

    private final UUID uuid;
    private final Set<String> joinedChannelIds = new LinkedHashSet<>();
    private String activeChannelId;

    public PlayerChannelState(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public Set<String> joinedChannelIds() {
        return Set.copyOf(joinedChannelIds);
    }

    public boolean hasJoined(String channelId) {
        return joinedChannelIds.contains(channelId.toLowerCase(java.util.Locale.ROOT));
    }

    public void join(String channelId) {
        joinedChannelIds.add(channelId.toLowerCase(java.util.Locale.ROOT));
    }

    public void leave(String channelId) {
        joinedChannelIds.remove(channelId.toLowerCase(java.util.Locale.ROOT));
    }

    public String activeChannelId() {
        return activeChannelId;
    }

    public void setActiveChannelId(String activeChannelId) {
        this.activeChannelId = activeChannelId;
    }

    public void restore(Set<String> joined, String active) {
        joinedChannelIds.addAll(joined);
        activeChannelId = active;
    }

}
