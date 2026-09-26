package com.sack.rpgroll.gameplay.levelup;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class PlayerLevelUpHandlerTest {

    private PlayerManager players;
    private Player bukkitPlayer;
    private PlayerLevelUpHandler handler;
    private UUID uuid;

    @BeforeEach
    void setUp() {
        players = mock(PlayerManager.class);
        bukkitPlayer = mock(Player.class);
        uuid = UUID.randomUUID();
        when(bukkitPlayer.getUniqueId()).thenReturn(uuid);
        handler = spy(new PlayerLevelUpHandler(players, mock(LevelUpRewardsConfig.class), mock(LangManager.class)));
    }

    private static RPGPlayer atLevel(int level) {
        RPGPlayer player = mock(RPGPlayer.class);
        when(player.getLevel()).thenReturn(level);
        return player;
    }

    @Test
    void levelUpAllKeepsGoingWhileTheExperienceReaches() {

        // Tres umbrales cruzados de golpe: sube 3 veces y a la cuarta ya no alcanza.
        AtomicInteger level = new AtomicInteger(1);
        doAnswer(invocation -> {
            if (level.get() >= 4) {
                return false;
            }
            level.incrementAndGet();
            return true;
        }).when(handler).tryLevelUp(any(), any());
        when(players.getPlayer(uuid)).thenAnswer(invocation -> Optional.of(atLevel(level.get())));

        assertEquals(3, handler.levelUpAll(bukkitPlayer, atLevel(1)));
    }

    @Test
    void levelUpAllReturnsZeroWhenTheExperienceDoesNotReach() {

        doReturn(false).when(handler).tryLevelUp(any(), any());

        assertEquals(0, handler.levelUpAll(bukkitPlayer, atLevel(5)));
    }

    @Test
    void levelUpAllStopsWhenTheLevelNoLongerRises() {

        // En el nivel máximo tryLevelUp puede devolver true sin subir: no debe colgarse.
        RPGPlayer maxed = atLevel(100);
        doReturn(true).when(handler).tryLevelUp(any(), any());
        when(players.getPlayer(uuid)).thenReturn(Optional.of(maxed));

        assertEquals(0, handler.levelUpAll(bukkitPlayer, maxed));
    }

}
