package com.sack.rpgroll.chat.channel;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelParserTest {

    private final ChannelParser parser = new ChannelParser();

    private YamlConfiguration load(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return config;
    }

    @Test
    void missingIdThrows() {
        YamlConfiguration config = load("scope: GLOBAL");
        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }

    @Test
    void minimalChannelFallsBackToDefaults() {
        YamlConfiguration config = load("id: global");
        ChatChannel channel = parser.parse(config);

        assertEquals("global", channel.id());
        assertEquals(ChannelScope.GLOBAL, channel.scope());
        assertEquals(ChatTextFormat.LEGACY, channel.textFormat());
        assertTrue(channel.filterProfanity());
        assertTrue(channel.filterCaps());
        assertFalse(channel.allowUrls());
        assertTrue(channel.defaultJoined());
    }

    @Test
    void unknownScopeFallsBackToGlobal() {
        YamlConfiguration config = load("""
                id: local
                scope: NOT_A_SCOPE
                """);

        assertEquals(ChannelScope.GLOBAL, parser.parse(config).scope());
    }

    @Test
    void parsesExplicitProximityScopeAndDistance() {
        YamlConfiguration config = load("""
                id: local
                scope: proximity
                distance: 50
                """);

        ChatChannel channel = parser.parse(config);

        assertEquals(ChannelScope.PROXIMITY, channel.scope());
        assertEquals(50.0, channel.distance());
    }

    @Test
    void parsesPermissionsAndRequiresPermissionFlags() {
        YamlConfiguration config = load("""
                id: staff
                view-permission: chat.staff.view
                speak-permission: chat.staff.speak
                """);

        ChatChannel channel = parser.parse(config);

        assertTrue(channel.requiresViewPermission());
        assertTrue(channel.requiresSpeakPermission());
    }

    @Test
    void channelWithoutPermissionsDoesNotRequireThem() {
        YamlConfiguration config = load("id: global");
        ChatChannel channel = parser.parse(config);

        assertFalse(channel.requiresViewPermission());
        assertFalse(channel.requiresSpeakPermission());
    }
}
