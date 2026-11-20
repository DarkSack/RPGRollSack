package com.sack.rpgroll.common.event.magic;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class SpellEventsTest {

    @Test
    void castEventExposesFieldsAndCopiesTags() {
        Player player = mock(Player.class);
        Set<String> tags = new HashSet<>(Set.of("fuego", "area"));
        SpellCastEvent event = new SpellCastEvent(player, "bola_de_fuego", "piromancia", tags);
        tags.add("extra");
        tags.remove("fuego");

        assertSame(player, event.getPlayer());
        assertEquals("bola_de_fuego", event.spellId());
        assertEquals("piromancia", event.schoolId());
        assertEquals(Set.of("fuego", "area"), event.tags());
        assertThrows(UnsupportedOperationException.class, () -> event.tags().add("x"));
    }

    @Test
    void channelStartExposesFields() {
        Player player = mock(Player.class);
        SpellChannelStartEvent event = new SpellChannelStartEvent(player, "rayo", "tormenta", 40);
        assertSame(player, event.getPlayer());
        assertEquals("rayo", event.spellId());
        assertEquals("tormenta", event.schoolId());
        assertEquals(40, event.castTimeTicks());
    }

    @Test
    void eachEventHasItsOwnHandlerList() {
        Player player = mock(Player.class);
        SpellCastEvent cast = new SpellCastEvent(player, "a", "b", Set.of());
        SpellChannelStartEvent start = new SpellChannelStartEvent(player, "a", "b", 1);
        SpellChannelEndEvent end = new SpellChannelEndEvent(player, "a", SpellChannelEndEvent.Reason.OTHER);

        assertNotNull(SpellCastEvent.getHandlerList());
        assertNotNull(SpellChannelStartEvent.getHandlerList());
        assertNotNull(SpellChannelEndEvent.getHandlerList());
        assertNotSame(SpellCastEvent.getHandlerList(), SpellChannelStartEvent.getHandlerList());
        assertNotSame(SpellCastEvent.getHandlerList(), SpellChannelEndEvent.getHandlerList());
        assertNotSame(SpellChannelStartEvent.getHandlerList(), SpellChannelEndEvent.getHandlerList());
        assertSame(SpellCastEvent.getHandlerList(), cast.getHandlers());
        assertSame(SpellChannelStartEvent.getHandlerList(), start.getHandlers());
        assertSame(SpellChannelEndEvent.getHandlerList(), end.getHandlers());
    }

    @Test
    void channelEndCarriesReason() {
        Player player = mock(Player.class);
        SpellChannelEndEvent event = new SpellChannelEndEvent(player, "rayo", SpellChannelEndEvent.Reason.DAMAGED);
        assertSame(player, event.getPlayer());
        assertEquals("rayo", event.spellId());
        assertEquals(SpellChannelEndEvent.Reason.DAMAGED, event.reason());
        assertEquals(4, SpellChannelEndEvent.Reason.values().length);
    }
}
