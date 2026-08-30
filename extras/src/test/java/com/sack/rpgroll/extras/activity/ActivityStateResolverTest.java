package com.sack.rpgroll.extras.activity;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActivityStateResolverTest {

    private ActivityStateResolver resolver;
    private Player player;
    private UUID uuid;

    @BeforeEach
    void setUp() {
        resolver = new ActivityStateResolver();
        player = mock(Player.class);
        uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
    }

    @Test
    void restingByDefaultWhenNoMovementOrCombatEverRecorded() {
        assertEquals(ActivityState.RESTING, resolver.resolve(player));
    }

    @Test
    void combatTakesPriorityOverSprintingAndMovement() {
        resolver.markCombat(player);
        when(player.isSprinting()).thenReturn(true);

        assertEquals(ActivityState.COMBAT, resolver.resolve(player));
    }

    @Test
    void sprintingTakesPriorityOverWalkingWhenNotInCombat() {
        when(player.isSprinting()).thenReturn(true);

        assertEquals(ActivityState.SPRINTING, resolver.resolve(player));
    }

    @Test
    void walkingWhenRecentlyMovedAndNotSprintingOrInCombat() {
        PlayerMoveEvent event = movementEvent(0, 0, 0, 1, 0, 0);
        resolver.onMove(event);

        assertEquals(ActivityState.WALKING, resolver.resolve(player));
    }

    @Test
    void identicalFromAndToCoordinatesDoesNotCountAsMovement() {
        PlayerMoveEvent event = movementEvent(0, 0, 0, 0, 0, 0);
        resolver.onMove(event);

        assertEquals(ActivityState.RESTING, resolver.resolve(player));
    }

    @Test
    void clearRemovesCombatAndMovementState() {
        resolver.markCombat(player);
        PlayerMoveEvent event = movementEvent(0, 0, 0, 1, 0, 0);
        resolver.onMove(event);

        resolver.clear(player);

        assertEquals(ActivityState.RESTING, resolver.resolve(player));
    }

    private PlayerMoveEvent movementEvent(double fromX, double fromY, double fromZ, double toX, double toY,
            double toZ) {

        Location from = mock(Location.class);
        Location to = mock(Location.class);

        when(from.getX()).thenReturn(fromX);
        when(from.getY()).thenReturn(fromY);
        when(from.getZ()).thenReturn(fromZ);
        when(to.getX()).thenReturn(toX);
        when(to.getY()).thenReturn(toY);
        when(to.getZ()).thenReturn(toZ);

        PlayerMoveEvent event = mock(PlayerMoveEvent.class);
        when(event.getFrom()).thenReturn(from);
        when(event.getTo()).thenReturn(to);
        when(event.getPlayer()).thenReturn(player);

        return event;
    }
}
