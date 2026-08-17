package com.sack.rpgroll.chat.reaction;

import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reacciones a mensajes — spec "los jugadores pueden reaccionar". Minecraft
 * no permite editar un mensaje de chat ya entregado (más aún con firma de
 * chat desde 1.19.1+), así que reaccionar no le agrega un contador en vivo
 * al mensaje original: se guarda contra el id del mensaje y se avisa con
 * una línea de seguimiento — el historial ({@code /chatlog}) sí refleja
 * el conteo real.
 */
public class ReactionManager {

    private static final int MAX_TRACKED = 2000;

    private final Map<Long, TrackedMessage> tracked = new LinkedHashMap<>();
    private final Map<UUID, Long> lastSeenMessageId = new ConcurrentHashMap<>();
    private final LangManager lang;

    public ReactionManager(LangManager lang) {
        this.lang = lang;
    }

    public synchronized void registerMessage(long messageId, UUID senderId, String channelId) {

        String senderName = Bukkit.getOfflinePlayer(senderId).getName();
        tracked.put(messageId, new TrackedMessage(messageId, senderId, senderName != null ? senderName : "?",
                channelId));

        while (tracked.size() > MAX_TRACKED) {
            var iterator = tracked.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    public void markSeen(Player viewer, long messageId) {
        lastSeenMessageId.put(viewer.getUniqueId(), messageId);
    }

    public Long lastSeen(UUID viewerId) {
        return lastSeenMessageId.get(viewerId);
    }

    public enum ReactResult {
        OK,
        MESSAGE_NOT_FOUND
    }

    public synchronized ReactResult react(Player reactor, long messageId, ReactionType type) {

        TrackedMessage message = tracked.get(messageId);

        if (message == null) {
            return ReactResult.MESSAGE_NOT_FOUND;
        }

        boolean added = message.toggle(reactor.getUniqueId(), type);
        notifyReaction(reactor, message, type, added);

        return ReactResult.OK;
    }

    private void notifyReaction(Player reactor, TrackedMessage message, ReactionType type, boolean added) {

        String key = added ? "reaction.notify_added" : "reaction.notify_removed";
        Object[] placeholders = {"symbol", type.symbol(), "reactor", reactor.getName(), "sender",
                message.senderName(), "count", message.countOf(type)};

        Player sender = Bukkit.getPlayer(message.senderId());
        if (sender != null) {
            lang.send(sender, key, placeholders);
        }

        if (!reactor.getUniqueId().equals(message.senderId())) {
            lang.send(reactor, key, placeholders);
        }
    }

}
