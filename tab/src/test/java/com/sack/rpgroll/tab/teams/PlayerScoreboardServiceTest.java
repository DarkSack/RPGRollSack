package com.sack.rpgroll.tab.teams;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerScoreboardServiceTest {

    private final Scoreboard ours = mock(Scoreboard.class);
    private final Scoreboard main = mock(Scoreboard.class);
    private final PlayerScoreboardService service = new PlayerScoreboardService(() -> ours);

    private static Player player(UUID id, Scoreboard showing) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        when(player.getScoreboard()).thenReturn(showing);
        return player;
    }

    @Test
    void aPlayerWhoRejoinsGetsTheBoardBack() {
        UUID id = UUID.randomUUID();
        service.getOrCreate(player(id, main));

        // Al volver a entrar es otro objeto Player y Minecraft le enseña el scoreboard principal.
        Player again = player(id, main);
        assertSame(ours, service.getOrCreate(again));
        verify(again).setScoreboard(ours);
    }

    @Test
    void aBoardTakenAwayByAnotherPluginIsPutBack() {
        Player player = player(UUID.randomUUID(), ours);
        service.getOrCreate(player);
        when(player.getScoreboard()).thenReturn(main);

        service.getOrCreate(player);
        verify(player).setScoreboard(ours);
    }

    @Test
    void aBoardAlreadyShownIsNotSetAgain() {
        Player player = player(UUID.randomUUID(), ours);
        service.getOrCreate(player);
        service.getOrCreate(player);
        verify(player, never()).setScoreboard(ours);
    }
}
