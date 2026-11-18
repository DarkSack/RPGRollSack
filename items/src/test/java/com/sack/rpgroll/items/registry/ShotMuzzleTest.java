package com.sack.rpgroll.items.registry;

import com.sack.rpgroll.items.core.ItemAction;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.MainHand;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShotMuzzleTest {
    @Test void scalesAndMirrorsTheMuzzleWithoutChangingAim() {
        Player player = mock(Player.class);
        when(player.getMainHand()).thenReturn(MainHand.RIGHT);
        ShotMuzzle muzzle = new ShotMuzzle(.9, .32, .62);
        Location eye = new Location(null, 0, 65, 0);
        for (double value : new double[]{.7, 1, 1.5}) {
            Location right = muzzle.at(value, false, eye, eye.getDirection());
            Location left = muzzle.at(value, true, eye, eye.getDirection());
            assertEquals(-.32 * value, right.getX(), 1e-8);
            assertEquals(-right.getX(), left.getX(), 1e-8);
            assertEquals(65 - .62 * value, right.getY(), 1e-8);
            assertEquals(right.getY(), left.getY(), 1e-8);
            assertEquals(.9 * value, right.getZ(), 1e-8);
            try (var scale = org.mockito.Mockito.mockStatic(ShotMuzzle.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
                scale.when(() -> ShotMuzzle.playerScale(player)).thenReturn(value);
                when(player.getMainHand()).thenReturn(MainHand.LEFT);
                assertEquals(left, muzzle.at(player, EquipmentSlot.HAND, eye, eye.getDirection()));
                when(player.getMainHand()).thenReturn(MainHand.RIGHT);
                assertEquals(left, muzzle.at(player, EquipmentSlot.OFF_HAND, eye, eye.getDirection()));
            }
        }
    }

    @Test void remainsFiniteLookingStraightUpOrDown() {
        Player player = mock(Player.class);
        ShotMuzzle muzzle = new ShotMuzzle(.9, .32, .62);
        for (float pitch : new float[]{-90, 90}) {
            Location eye = new Location(null, 0, 65, 0, 42, pitch);
            muzzle.at(1, false, eye, eye.getDirection()).checkFinite();
        }
    }

    @Test void rejectsInvalidOffsets() {
        for (String key : new String[]{"muzzle-forward", "muzzle-right", "muzzle-down"}) {
            for (String value : new String[]{"NaN", "Infinity", "-4.1", "5", "oops"}) {
                assertThrows(IllegalArgumentException.class,
                        () -> ShotMuzzle.from(new ItemAction("SHOOT", Map.of(key, value))));
            }
        }
    }
}
