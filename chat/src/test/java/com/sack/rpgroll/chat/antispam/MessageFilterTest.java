package com.sack.rpgroll.chat.antispam;

import com.sack.rpgroll.chat.channel.ChannelScope;
import com.sack.rpgroll.chat.channel.ChatChannel;
import com.sack.rpgroll.chat.channel.ChatTextFormat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageFilterTest {

    private final AntiSpamConfig config = new AntiSpamConfig(5, 10_000, 3, 70, 8,
            List.of("badword"), List.of());

    private ChatChannel channel(boolean filterProfanity, boolean filterCaps) {
        return new ChatChannel("global", "Global", "PAPER", "WHITE", 0, ChannelScope.GLOBAL, 0,
                null, null, 0, null, ChatTextFormat.LEGACY, null, filterProfanity, filterCaps, false, true, true,
                false);
    }

    @Test
    void replacementsAreAppliedRegardlessOfFilters() {
        MessageFilter filter = new MessageFilter(config, Map.of(":)", "☺"));
        String result = filter.apply(channel(false, false), "hello :)");

        assertEquals("hello ☺", result);
    }

    @Test
    void profanityFilterCensorsBannedWordsCaseInsensitively() {
        MessageFilter filter = new MessageFilter(config, Map.of());
        String result = filter.apply(channel(true, false), "you are a BADWORD");

        assertEquals("you are a " + "*".repeat("badword".length()), result);
    }

    @Test
    void profanityFilterDisabledLeavesBannedWordsUntouched() {
        MessageFilter filter = new MessageFilter(config, Map.of());
        String result = filter.apply(channel(false, false), "you are a badword");

        assertEquals("you are a badword", result);
    }

    @Test
    void capsFilterSoftensMostlyUppercaseMessages() {
        MessageFilter filter = new MessageFilter(config, Map.of());
        String result = filter.apply(channel(false, true), "THIS IS SHOUTING");

        assertEquals("This is shouting", result);
    }

    @Test
    void capsFilterLeavesShortMessagesUntouched() {
        MessageFilter filter = new MessageFilter(config, Map.of());
        String result = filter.apply(channel(false, true), "HI");

        assertEquals("HI", result);
    }

    @Test
    void capsFilterDisabledLeavesMessageUntouched() {
        MessageFilter filter = new MessageFilter(config, Map.of());
        String result = filter.apply(channel(false, false), "THIS IS SHOUTING");

        assertEquals("THIS IS SHOUTING", result);
    }

    @Test
    void mixedCaseMessageBelowThresholdIsNotSoftened() {
        MessageFilter filter = new MessageFilter(config, Map.of());
        String result = filter.apply(channel(false, true), "This Is Mostly Lowercase Text");

        assertEquals("This Is Mostly Lowercase Text", result);
    }
}
