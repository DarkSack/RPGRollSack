package com.sack.rpgroll.chat.role;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatRoleParserTest {

    private final ChatRoleParser parser = new ChatRoleParser();

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
        YamlConfiguration config = load("color: RED");
        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }

    @Test
    void minimalRoleFallsBackToDefaults() throws Exception {
        YamlConfiguration config = load("id: vip");
        ChatRole role = parser.parse(config);

        assertEquals("", role.prefix());
        assertEquals("", role.suffix());
        assertEquals("WHITE", role.color());
        assertEquals(0, role.priority());
        assertNull(role.permission());
    }

    @Test
    void parsesAllFields() throws Exception {
        YamlConfiguration config = load("""
                id: vip
                prefix: "[VIP]"
                suffix: "!"
                color: GOLD
                icon: STAR
                priority: 10
                permission: chat.vip
                """);

        ChatRole role = parser.parse(config);

        assertEquals("[VIP]", role.prefix());
        assertEquals("!", role.suffix());
        assertEquals("GOLD", role.color());
        assertEquals("STAR", role.icon());
        assertEquals(10, role.priority());
        assertEquals("chat.vip", role.permission());
    }
}
