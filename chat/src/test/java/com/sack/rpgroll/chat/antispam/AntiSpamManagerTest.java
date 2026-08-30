package com.sack.rpgroll.chat.antispam;

import com.sack.rpgroll.chat.channel.ChannelScope;
import com.sack.rpgroll.chat.channel.ChatChannel;
import com.sack.rpgroll.chat.channel.ChatTextFormat;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AntiSpamManagerTest {

    private AntiSpamManager manager;
    private Player player;
    private ChatChannel channel;

    @BeforeEach
    void setUp() {
        AntiSpamConfig config = new AntiSpamConfig(3, 10_000, 2, 70, 8,
                List.of("badword"), List.of("discord.gg"));
        manager = new AntiSpamManager(config);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.hasPermission("rpgrollchat.antispam.bypass")).thenReturn(false);

        channel = new ChatChannel("global", "Global", "PAPER", "WHITE", 0, ChannelScope.GLOBAL, 0,
                null, null, 0, null, ChatTextFormat.LEGACY, null, true, true, false, true, true, false);
    }

    @Test
    void bypassPermissionSkipsAllChecks() {
        when(player.hasPermission("rpgrollchat.antispam.bypass")).thenReturn(true);

        assertEquals(AntiSpamManager.Result.OK, manager.check(player, channel, "SPAM SPAM SPAM SPAM"));
        assertEquals(AntiSpamManager.Result.OK, manager.check(player, channel, "SPAM SPAM SPAM SPAM"));
    }

    @Test
    void firstMessageIsAlwaysOk() {
        assertEquals(AntiSpamManager.Result.OK, manager.check(player, channel, "hello there"));
    }

    @Test
    void cooldownBlocksSecondMessageOnSameChannelBeforeExpiry() {
        ChatChannel cooldownChannel = new ChatChannel("global", "Global", "PAPER", "WHITE", 0,
                ChannelScope.GLOBAL, 0, null, null, 60_000, null, ChatTextFormat.LEGACY, null, true, true, false,
                true, true, false);

        assertEquals(AntiSpamManager.Result.OK, manager.check(player, cooldownChannel, "hello"));
        assertEquals(AntiSpamManager.Result.ON_COOLDOWN, manager.check(player, cooldownChannel, "hello again"));
    }

    @Test
    void floodIsDetectedWhenTooManyMessagesArriveInWindow() {
        for (int i = 0; i < 3; i++) {
            manager.check(player, channel, "message " + i);
        }

        assertEquals(AntiSpamManager.Result.FLOOD, manager.check(player, channel, "one too many"));
    }

    @Test
    void repetitionIsDetectedAfterThresholdIdenticalMessages() {
        manager.check(player, channel, "same message");
        assertEquals(AntiSpamManager.Result.REPETITION, manager.check(player, channel, "same message"));
    }

    @Test
    void repetitionCheckIsCaseAndWhitespaceInsensitive() {
        manager.check(player, channel, "Same Message");
        assertEquals(AntiSpamManager.Result.REPETITION, manager.check(player, channel, "  same message  "));
    }

    @Test
    void differentMessagesDoNotTriggerRepetition() {
        manager.check(player, channel, "first message");
        assertEquals(AntiSpamManager.Result.OK, manager.check(player, channel, "second message"));
    }

    @Test
    void urlIsBlockedWhenChannelDisallowsThem() {
        assertEquals(AntiSpamManager.Result.URL_BLOCKED,
                manager.check(player, channel, "join my discord.gg/invite"));
    }

    @Test
    void urlIsAllowedWhenChannelPermitsIt() {
        ChatChannel urlAllowedChannel = new ChatChannel("global", "Global", "PAPER", "WHITE", 0,
                ChannelScope.GLOBAL, 0, null, null, 0, null, ChatTextFormat.LEGACY, null, true, true, true,
                true, true, false);

        assertEquals(AntiSpamManager.Result.OK, manager.check(player, urlAllowedChannel, "discord.gg/invite"));
    }

    @Test
    void bannedWordIsDetectedCaseInsensitively() {
        assertEquals(AntiSpamManager.Result.BANNED_WORD, manager.check(player, channel, "you are a BadWord"));
    }

    @Test
    void excessiveCapsIsDetectedAboveThreshold() {
        assertEquals(AntiSpamManager.Result.EXCESSIVE_CAPS, manager.check(player, channel, "THIS IS SHOUTING"));
    }

    @Test
    void shortMessagesAreExemptFromCapsCheck() {
        assertEquals(AntiSpamManager.Result.OK, manager.check(player, channel, "HI"));
    }

    @Test
    void clearResetsAllTrackedStateForPlayer() {
        manager.check(player, channel, "same message");
        manager.clear(player.getUniqueId());

        assertEquals(AntiSpamManager.Result.OK, manager.check(player, channel, "same message"));
    }
}
