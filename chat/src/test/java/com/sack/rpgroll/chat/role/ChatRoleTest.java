package com.sack.rpgroll.chat.role;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatRoleTest {

    @Test
    void matchesAnyoneWhenPermissionIsNullOrBlank() {
        ChatRole role = new ChatRole("default", "", "", "WHITE", "", 0, null);
        Player player = mock(Player.class);

        assertTrue(role.matches(player));
    }

    @Test
    void matchesOnlyPlayersWithPermission() {
        ChatRole role = new ChatRole("vip", "", "", "WHITE", "", 0, "chat.vip");
        Player player = mock(Player.class);
        when(player.hasPermission("chat.vip")).thenReturn(false);

        assertFalse(role.matches(player));

        when(player.hasPermission("chat.vip")).thenReturn(true);
        assertTrue(role.matches(player));
    }
}
