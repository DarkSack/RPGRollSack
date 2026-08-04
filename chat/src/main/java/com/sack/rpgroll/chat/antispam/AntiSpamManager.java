package com.sack.rpgroll.chat.antispam;

import com.sack.rpgroll.chat.channel.ChatChannel;

import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Cooldown/flood/repetición/URLs/mayúsculas/palabras prohibidas/publicidad — todo configurable. */
public class AntiSpamManager {

    public enum Result {
        OK,
        ON_COOLDOWN,
        FLOOD,
        REPETITION,
        BANNED_WORD,
        URL_BLOCKED,
        EXCESSIVE_CAPS
    }

    private final AntiSpamConfig config;
    private final Map<UUID, Map<String, Long>> lastMessageByChannel = new HashMap<>();
    private final Map<UUID, Deque<Long>> recentTimestamps = new HashMap<>();
    private final Map<UUID, String> lastMessageContent = new HashMap<>();
    private final Map<UUID, Integer> repeatCount = new HashMap<>();

    public AntiSpamManager(AntiSpamConfig config) {
        this.config = config;
    }

    public Result check(Player player, ChatChannel channel, String message) {

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (player.hasPermission("rpgrollchat.antispam.bypass")) {
            return Result.OK;
        }

        if (channel.cooldownMillis() > 0) {
            Long last = lastMessageByChannel.computeIfAbsent(uuid, k -> new HashMap<>()).get(channel.id());
            if (last != null && now - last < channel.cooldownMillis()) {
                return Result.ON_COOLDOWN;
            }
        }

        Deque<Long> timestamps = recentTimestamps.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        timestamps.addLast(now);
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > config.floodWindowMillis()) {
            timestamps.pollFirst();
        }
        if (timestamps.size() > config.floodMaxMessages()) {
            return Result.FLOOD;
        }

        String normalized = message.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals(lastMessageContent.get(uuid))) {
            int count = repeatCount.merge(uuid, 1, Integer::sum);
            if (count >= config.repetitionThreshold()) {
                return Result.REPETITION;
            }
        } else {
            repeatCount.put(uuid, 0);
        }

        if (!channel.allowUrls() && containsUrl(normalized)) {
            return Result.URL_BLOCKED;
        }

        if (containsBannedWord(normalized)) {
            return Result.BANNED_WORD;
        }

        if (isExcessiveCaps(message)) {
            return Result.EXCESSIVE_CAPS;
        }

        lastMessageByChannel.computeIfAbsent(uuid, k -> new HashMap<>()).put(channel.id(), now);
        lastMessageContent.put(uuid, normalized);

        return Result.OK;
    }

    private boolean containsUrl(String normalized) {
        return config.adKeywords().stream().anyMatch(normalized::contains);
    }

    private boolean containsBannedWord(String normalized) {
        return config.bannedWords().stream().anyMatch(word -> normalized.contains(word.toLowerCase(Locale.ROOT)));
    }

    private boolean isExcessiveCaps(String message) {

        String letters = message.replaceAll("[^a-zA-Z]", "");

        if (letters.length() < config.minLengthForCapsCheck()) {
            return false;
        }

        long upper = letters.chars().filter(Character::isUpperCase).count();
        return (upper * 100 / letters.length()) >= config.maxCapsPercent();
    }

    public void clear(UUID uuid) {
        lastMessageByChannel.remove(uuid);
        recentTimestamps.remove(uuid);
        lastMessageContent.remove(uuid);
        repeatCount.remove(uuid);
    }

}
