package com.sack.rpgroll.chat.mention;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * {@code GuildsAPI.isReady()} devuelve false en este entorno (RPGRollGuilds nunca se
 * inicializa), así que @guild/@team caen al lookup por nombre — que necesita a
 * {@code Bukkit} mockeado estáticamente. {@code highlight} es pura regex.
 */
class MentionResolverTest {

    private final MentionResolver resolver = new MentionResolver();

    private Player sender() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return player;
    }

    @Test
    void messageWithNoMentionsResolvesEmpty() {
        MentionResolver.MentionResult result = resolver.resolve("hello world, no mentions here", sender());

        assertTrue(result.mentionedPlayers().isEmpty());
        assertFalse(result.mentionsAll());
    }

    @Test
    void atAllMarksMentionsAll() {
        MentionResolver.MentionResult result = resolver.resolve("hey @all check this out", sender());
        assertTrue(result.mentionsAll());
    }

    @Test
    void atTodosMarksMentionsAllCaseInsensitively() {
        MentionResolver.MentionResult result = resolver.resolve("hola @TODOS", sender());
        assertTrue(result.mentionsAll());
    }

    @Test
    void guildAndTeamMentionsResolveToNoOneWhenGuildsAddonIsNotLoaded() {
        // Con GuildsAPI no inicializada, @guild/@team no entran a su rama y caen al
        // lookup por nombre — que sin servidor real necesita a Bukkit mockeado.
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact(anyString())).thenReturn(null);

            MentionResolver.MentionResult result = resolver.resolve("@guild @team status?", sender());

            assertTrue(result.mentionedPlayers().isEmpty());
            assertFalse(result.mentionsAll());
        }
    }

    @Test
    void unknownPlayerMentionIsIgnored() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Ghost")).thenReturn(null);

            MentionResolver.MentionResult result = resolver.resolve("hey @Ghost", sender());

            assertTrue(result.mentionedPlayers().isEmpty());
        }
    }

    @Test
    void onlinePlayerMentionIsResolved() {
        Player target = mock(Player.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Steve")).thenReturn(target);

            MentionResolver.MentionResult result = resolver.resolve("hey @Steve", sender());

            assertEquals(1, result.mentionedPlayers().size());
            assertTrue(result.mentionedPlayers().contains(target));
        }
    }

    @Test
    void highlightWrapsMentionsInLegacyColorCodes() {
        String highlighted = resolver.highlight("hey @Steve how's it going");
        assertEquals("hey &e&l@Steve&r&f how's it going", highlighted);
    }

    @Test
    void highlightHandlesMultipleMentions() {
        String highlighted = resolver.highlight("@all @Steve");
        assertEquals("&e&l@all&r&f &e&l@Steve&r&f", highlighted);
    }

    @Test
    void highlightLeavesTextWithoutMentionsUnchanged() {
        assertEquals("no mentions here", resolver.highlight("no mentions here"));
    }
}
