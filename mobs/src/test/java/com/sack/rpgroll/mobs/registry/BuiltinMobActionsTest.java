package com.sack.rpgroll.mobs.registry;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuiltinMobActionsTest {

    @Test
    void towardPointsFromOneToTheOtherWithTheGivenStrength() {
        Vector v = BuiltinMobActions.toward(new Vector(0, 64, 0), new Vector(3, 64, 4), 2.0);
        assertEquals(1.2, v.getX(), 1e-9);
        assertEquals(1.6, v.getZ(), 1e-9);
    }

    @Test
    void towardTheSameSpotIsZeroInsteadOfNaN() {
        Vector v = BuiltinMobActions.toward(new Vector(5, 64, 5), new Vector(5, 64, 5), 1.6);
        assertEquals(new Vector(0, 0, 0), v);
        v.setY(0.3).checkFinite(); // lo que hace PUSH antes de setVelocity
    }
}
