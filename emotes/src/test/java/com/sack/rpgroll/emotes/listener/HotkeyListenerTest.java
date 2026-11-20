package com.sack.rpgroll.emotes.listener;

import com.sack.rpgroll.emotes.EmotesSettings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HotkeyListenerTest {

    private final List<Player> opened = new ArrayList<>();

    private static Player player(boolean sneaking, boolean permission) {
        Player p = mock(Player.class);
        when(p.isSneaking()).thenReturn(sneaking);
        when(p.hasPermission("rpgrollemotes.use")).thenReturn(permission);
        return p;
    }

    private PlayerSwapHandItemsEvent swap(Player p, boolean sneakSwap) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("hotkeys.sneak-swap", sneakSwap);
        EmotesSettings settings = EmotesSettings.from(yaml);
        PlayerSwapHandItemsEvent event = new PlayerSwapHandItemsEvent(p, null, null);
        new HotkeyListener(() -> settings, opened::add).onSwap(event);
        return event;
    }

    @Test
    void sneakSwapOpensMenuAndCancels() {
        Player p = player(true, true);
        PlayerSwapHandItemsEvent event = swap(p, true);
        assertTrue(event.isCancelled());
        assertEquals(List.of(p), opened);
    }

    @Test
    void normalSwapUntouched() {
        PlayerSwapHandItemsEvent event = swap(player(false, true), true);
        assertFalse(event.isCancelled());
        assertTrue(opened.isEmpty());
    }

    @Test
    void disabledInConfigUntouched() {
        PlayerSwapHandItemsEvent event = swap(player(true, true), false);
        assertFalse(event.isCancelled());
        assertTrue(opened.isEmpty());
    }

    @Test
    void noPermissionUntouched() {
        PlayerSwapHandItemsEvent event = swap(player(true, false), true);
        assertFalse(event.isCancelled());
        assertTrue(opened.isEmpty());
    }
}
