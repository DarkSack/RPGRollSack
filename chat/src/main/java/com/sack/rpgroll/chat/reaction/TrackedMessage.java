package com.sack.rpgroll.chat.reaction;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Reacciones acumuladas de un mensaje ya enviado (no editable, ver {@link ReactionManager}). */
public class TrackedMessage {

    private final long id;
    private final UUID senderId;
    private final String senderName;
    private final String channelId;
    private final Map<ReactionType, Set<UUID>> reactions = new EnumMap<>(ReactionType.class);

    public TrackedMessage(long id, UUID senderId, String senderName, String channelId) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.channelId = channelId;
    }

    public long id() {
        return id;
    }

    public UUID senderId() {
        return senderId;
    }

    public String senderName() {
        return senderName;
    }

    public String channelId() {
        return channelId;
    }

    /** @return true si quedó agregada (false si se sacó porque ya estaba). */
    public boolean toggle(UUID reactorId, ReactionType type) {

        Set<UUID> reactors = reactions.computeIfAbsent(type, k -> new LinkedHashSet<>());

        if (!reactors.remove(reactorId)) {
            reactors.add(reactorId);
            return true;
        }

        return false;
    }

    public int countOf(ReactionType type) {
        return reactions.getOrDefault(type, Set.of()).size();
    }

}
