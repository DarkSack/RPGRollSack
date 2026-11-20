package com.sack.rpgroll.emotes.listener;

import com.sack.rpgroll.emotes.catalog.CancelReason;
import com.sack.rpgroll.emotes.catalog.Emote;
import com.sack.rpgroll.emotes.service.EmoteSession;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CancelRulesTest {

    private final World world = mock(World.class);
    private final Location start = new Location(world, 10, 64, 10, 0, 0);

    private static Emote emote(Set<CancelReason> cancel) {
        return new Emote("wave", "wave", "STONE", "general", "wave", "m", false, false, "common",
                null, null, cancel, false, null, null);
    }

    private static EmoteSession session(boolean magic) {
        return new EmoteSession("wave", false, null, 0L, magic);
    }

    @Test
    void headTurnIsNotMovement() {
        assertFalse(CancelRules.movedTooFar(start, new Location(world, 10, 64, 10, 90, 45), 0.3));
    }

    @Test
    void jumpInPlaceIsNotMovement() {
        assertFalse(CancelRules.movedTooFar(start, new Location(world, 10, 65, 10), 0.3));
    }

    @Test
    void horizontalBeyond03Cancels() {
        assertTrue(CancelRules.movedTooFar(start, new Location(world, 10.31, 64, 10), 0.3));
        assertFalse(CancelRules.movedTooFar(start, new Location(world, 10.29, 64, 10), 0.3));
    }

    @Test
    void otherWorldCancels() {
        assertTrue(CancelRules.movedTooFar(start, new Location(mock(World.class), 10, 64, 10), 0.3));
    }

    @Test
    void magicSessionsIgnoreCancelReasons() {
        assertFalse(CancelRules.cancels(emote(EnumSet.allOf(CancelReason.class)), session(true), CancelReason.MOVE));
    }

    @Test
    void emoteWithoutMoveInCancelSurvivesMove() {
        Emote e = emote(EnumSet.of(CancelReason.DAMAGE));
        assertFalse(CancelRules.cancels(e, session(false), CancelReason.MOVE));
        assertTrue(CancelRules.cancels(e, session(false), CancelReason.DAMAGE));
    }

    @Test
    void combatNeedsADifferentLivingVictim() {
        Player attacker = mock(Player.class);
        assertTrue(CancelRules.countsAsCombat(attacker, mock(Pig.class)));
        assertFalse(CancelRules.countsAsCombat(attacker, attacker));
        assertFalse(CancelRules.countsAsCombat(attacker, mock(ArmorStand.class)));
        assertFalse(CancelRules.countsAsCombat(attacker, mock(Entity.class)));
    }
}
