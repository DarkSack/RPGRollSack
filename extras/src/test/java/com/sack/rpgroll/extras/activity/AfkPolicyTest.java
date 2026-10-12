package com.sack.rpgroll.extras.activity;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AfkPolicyTest {

    private static Player idleFor(Duration idle) {
        Player player = mock(Player.class);
        when(player.getIdleDuration()).thenReturn(idle);
        return player;
    }

    private static AfkPolicy parse(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return AfkPolicy.from(config);
    }

    @Test
    void freezesOnlyOnceTheIdleThresholdIsReached() throws InvalidConfigurationException {

        AfkPolicy policy = parse("afk:\n  pause-stats: true\n  idle-seconds: 120\n");

        assertFalse(policy.freezes(idleFor(Duration.ofSeconds(119))));
        assertTrue(policy.freezes(idleFor(Duration.ofSeconds(120))));
    }

    @Test
    void neverFreezesWhenDisabled() throws InvalidConfigurationException {

        AfkPolicy policy = parse("afk:\n  pause-stats: false\n  idle-seconds: 1\n");

        assertFalse(policy.freezes(idleFor(Duration.ofHours(3))));
    }

    @Test
    void withoutTheSectionPausesAfterFiveMinutes() throws InvalidConfigurationException {

        AfkPolicy policy = parse("language: es\n");

        assertTrue(policy.pauseStats());
        assertEquals(Duration.ofMinutes(5), policy.idleThreshold());
    }


    @Test
    void aLobbyWorldFreezesStatsEvenForAnActivePlayer() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("stats-disabled-worlds", java.util.List.of("lobby"));
        AfkPolicy policy = AfkPolicy.from(config);

        org.bukkit.World lobby = org.mockito.Mockito.mock(org.bukkit.World.class);
        org.mockito.Mockito.when(lobby.getName()).thenReturn("lobby");
        org.bukkit.entity.Player player = org.mockito.Mockito.mock(org.bukkit.entity.Player.class);
        org.mockito.Mockito.when(player.getWorld()).thenReturn(lobby);
        org.mockito.Mockito.when(player.getIdleDuration()).thenReturn(java.time.Duration.ZERO);

        org.junit.jupiter.api.Assertions.assertTrue(policy.freezes(player));
        org.mockito.Mockito.when(lobby.getName()).thenReturn("world");
        org.junit.jupiter.api.Assertions.assertFalse(policy.freezes(player));
    }
}
