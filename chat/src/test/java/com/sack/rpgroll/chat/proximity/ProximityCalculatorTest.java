package com.sack.rpgroll.chat.proximity;

import com.sack.rpgroll.chat.channel.ChannelScope;
import com.sack.rpgroll.chat.channel.ChatChannel;
import com.sack.rpgroll.chat.channel.ChatTextFormat;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProximityCalculatorTest {

    private ChatChannel channel(ChannelScope scope, double distance) {
        return new ChatChannel("chan", "Chan", "PAPER", "WHITE", 0, scope, distance,
                null, null, 0, null, ChatTextFormat.LEGACY, null, true, true, false, true, true, false);
    }

    private Player playerAt(World world, double x, double y, double z) {
        Player player = mock(Player.class);
        Location location = new Location(world, x, y, z);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);
        when(player.getEyeLocation()).thenReturn(location);
        return player;
    }

    @Test
    void nonProximityScopeAlwaysHears() {
        ChatChannel globalChannel = channel(ChannelScope.GLOBAL, 0);
        Player sender = mock(Player.class);
        Player receiver = mock(Player.class);

        assertTrue(ProximityCalculator.canHear(sender, receiver, globalChannel));
    }

    @Test
    void senderAlwaysHearsThemselves() {
        ChatChannel proximityChannel = channel(ChannelScope.PROXIMITY, 10);
        Player sender = mock(Player.class);

        assertTrue(ProximityCalculator.canHear(sender, sender, proximityChannel));
    }

    @Test
    void differentWorldsCannotHearEachOther() {
        ChatChannel proximityChannel = channel(ChannelScope.PROXIMITY, 10);
        World worldA = mock(World.class);
        World worldB = mock(World.class);

        Player sender = playerAt(worldA, 0, 64, 0);
        Player receiver = playerAt(worldB, 0, 64, 0);

        assertFalse(ProximityCalculator.canHear(sender, receiver, proximityChannel));
    }

    @Test
    void outOfRangeCannotHear() {
        ChatChannel proximityChannel = channel(ChannelScope.PROXIMITY, 10);
        World world = mock(World.class);

        Player sender = playerAt(world, 0, 64, 0);
        Player receiver = playerAt(world, 50, 64, 0);

        assertFalse(ProximityCalculator.canHear(sender, receiver, proximityChannel));
    }

    @Test
    void withinRangeAndUnobstructedCanHear() {
        ChatChannel proximityChannel = channel(ChannelScope.PROXIMITY, 10);
        World world = mock(World.class);
        when(world.rayTraceBlocks(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(null);

        Player sender = playerAt(world, 0, 64, 0);
        Player receiver = playerAt(world, 5, 64, 0);

        assertTrue(ProximityCalculator.canHear(sender, receiver, proximityChannel));
    }

    @Test
    void zeroDistanceMeansUnlimitedRange() {
        ChatChannel unlimitedChannel = channel(ChannelScope.PROXIMITY, 0);
        World world = mock(World.class);
        when(world.rayTraceBlocks(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(null);

        Player sender = playerAt(world, 0, 64, 0);
        Player receiver = playerAt(world, 1000, 64, 0);

        assertTrue(ProximityCalculator.canHear(sender, receiver, unlimitedChannel));
    }

    @Test
    void isAttenuatedFalseForNonProximityOrUnlimitedRange() {
        Player sender = mock(Player.class);
        Player receiver = mock(Player.class);

        assertFalse(ProximityCalculator.isAttenuated(sender, receiver, channel(ChannelScope.GLOBAL, 10)));
        assertFalse(ProximityCalculator.isAttenuated(sender, receiver, channel(ChannelScope.PROXIMITY, 0)));
    }

    @Test
    void isAttenuatedTrueBeyondHalfRadius() {
        ChatChannel proximityChannel = channel(ChannelScope.PROXIMITY, 10);
        World world = mock(World.class);

        Player sender = playerAt(world, 0, 64, 0);
        Player farReceiver = playerAt(world, 6, 64, 0);
        Player nearReceiver = playerAt(world, 2, 64, 0);

        assertTrue(ProximityCalculator.isAttenuated(sender, farReceiver, proximityChannel));
        assertFalse(ProximityCalculator.isAttenuated(sender, nearReceiver, proximityChannel));
    }
}
