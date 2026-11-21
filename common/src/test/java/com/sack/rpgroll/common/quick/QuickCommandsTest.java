package com.sack.rpgroll.common.quick;

import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QuickCommandsTest {

    private static final Predicate<String> ONLINE = name -> name.equalsIgnoreCase("Sack");

    @Test
    void thePlayerGoesFirstLikeVanillaGive() {
        var a = QuickCommands.GiveArgs.parse(new String[] { "Sack", "item:medallon", "5" }, ONLINE);
        assertEquals("Sack", a.target());
        assertEquals("item:medallon", a.thing());
        assertEquals(5, a.amount());
    }

    @Test
    void withoutAPlayerItIsForYou() {
        var a = QuickCommands.GiveArgs.parse(new String[] { "medallon", "3" }, ONLINE);
        assertNull(a.target());
        assertEquals("medallon", a.thing());
        assertEquals(3, a.amount());
        assertEquals(1, QuickCommands.GiveArgs.parse(new String[] { "medallon" }, ONLINE).amount());
        // Una sola palabra es siempre la cosa, aunque coincida con un jugador.
        assertEquals("Sack", QuickCommands.GiveArgs.parse(new String[] { "Sack" }, ONLINE).thing());
    }

    @Test
    void amountsAreKeptSane() {
        assertEquals(-1, QuickCommands.amount("muchos", 64));
        assertEquals(-1, QuickCommands.amount("0", 64));
        assertEquals(64, QuickCommands.amount("9999", 64));
        assertEquals(7, QuickCommands.amount("7", 64));
    }
}
