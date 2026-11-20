package com.sack.rpgroll.tab.core;

import com.sack.rpgroll.tab.profile.PlayerStateManager;
import com.sack.rpgroll.tab.profile.TABProfile;
import com.sack.rpgroll.tab.scoreboard.ScoreboardDefinition;
import com.sack.rpgroll.tab.scoreboard.ScoreboardEngine;
import com.sack.rpgroll.tab.scoreboard.ScoreboardManager;
import com.sack.rpgroll.tab.tablist.TabListEngine;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RefreshCoordinatorTest {

    @Test
    void periodicallyRendersCurrentScoreboardWithoutResolvingContextOrOtherEngines() {
        Player player = mock(Player.class);
        PlayerStateManager states = mock(PlayerStateManager.class);
        TabListEngine tabList = mock(TabListEngine.class);
        ScoreboardManager scoreboards = mock(ScoreboardManager.class);
        ScoreboardEngine engine = mock(ScoreboardEngine.class);
        ScoreboardDefinition first = mock(ScoreboardDefinition.class);
        ScoreboardDefinition updated = mock(ScoreboardDefinition.class);
        when(states.activeProfile(player)).thenReturn(Optional.of(
                new TABProfile("profile", null, "sidebar", null, null, null, null, null, null)));
        when(scoreboards.getResolved("sidebar")).thenReturn(Optional.of(first), Optional.of(updated));
        RefreshCoordinator coordinator = coordinator(states, tabList, scoreboards, engine);

        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(player));

            // Cada pasada (10 ticks, 0,5 s): así se mueve el nombre animado de un gremio.
            coordinator.tickAnimations();
            verify(engine).apply(player, first);
            coordinator.tickAnimations();
            verify(engine).apply(player, updated);
        }

        verify(tabList, times(2)).reapplyAnimatedElements();
        verify(states, never()).refresh(player);
        verifyNoInteractions(first, updated);
    }

    @Test
    void skipsPlayersWithoutAnActiveScoreboard() {
        Player noProfile = mock(Player.class);
        Player noSidebar = mock(Player.class);
        PlayerStateManager states = mock(PlayerStateManager.class);
        TabListEngine tabList = mock(TabListEngine.class);
        ScoreboardManager scoreboards = mock(ScoreboardManager.class);
        ScoreboardEngine engine = mock(ScoreboardEngine.class);
        when(states.activeProfile(noSidebar)).thenReturn(Optional.of(
                new TABProfile("profile", null, null, null, null, null, null, null, null)));
        RefreshCoordinator coordinator = coordinator(states, tabList, scoreboards, engine);

        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(noProfile, noSidebar));
            coordinator.tickAnimations();
            coordinator.tickAnimations();
        }

        verifyNoInteractions(scoreboards, engine);
    }

    private RefreshCoordinator coordinator(PlayerStateManager states, TabListEngine tabList,
            ScoreboardManager scoreboards, ScoreboardEngine engine) {
        return new RefreshCoordinator(states, tabList, null, null, null, null, scoreboards, engine,
                null, null, null);
    }
}
