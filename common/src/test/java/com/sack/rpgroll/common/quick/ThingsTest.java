package com.sack.rpgroll.common.quick;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThingsTest {

    private record Fake(String type, Collection<String> ids) implements Things.Source {
    }

    private static final List<Things.Source> SOURCES = List.of(
            new Fake("item", List.of("espada_de_fuego", "medallon", "x")),
            new Fake("mueble", List.of("silla", "x")),
            new Fake(Things.VANILLA, List.of("diamond", "diamond_sword", "zombie")));

    @Test
    void aTypedReferenceGoesToThatSource() {
        Things.Match m = Things.resolve(SOURCES, "item:medallon").orElseThrow();
        assertEquals("item", m.source().type());
        assertEquals("medallon", m.id());
        assertEquals("mueble", Things.resolve(SOURCES, "MUEBLE:Silla").orElseThrow().source().type());
    }

    @Test
    void aBareIdIsFoundWhereverItIs() {
        assertEquals("item", Things.resolve(SOURCES, "medallon").orElseThrow().source().type());
        assertEquals(Things.VANILLA, Things.resolve(SOURCES, "diamond").orElseThrow().source().type());
        assertEquals(Things.VANILLA, Things.resolve(SOURCES, "minecraft:zombie").orElseThrow().source().type());
    }

    @Test
    void unknownOrAmbiguousResolvesToNothing() {
        assertTrue(Things.resolve(SOURCES, "nada").isEmpty());
        assertTrue(Things.resolve(SOURCES, "otro:silla").isEmpty());
        assertTrue(Things.resolve(SOURCES, "x").isEmpty());
        assertEquals(List.of("item:x", "mueble:x"), Things.candidates(SOURCES, "x"));
    }

    @Test
    void suggestionsStartWithTheTypesAndFindIdsAnywhere() {
        assertEquals(List.of("item:", "mueble:", "minecraft:"), Things.suggest(SOURCES, "", 50));
        assertEquals(List.of("item:medallon", "mueble:"), Things.suggest(SOURCES, "m", 50).subList(0, 2));
        assertEquals(List.of("item:espada_de_fuego"), Things.suggest(SOURCES, "esp", 50));
        assertEquals(List.of("minecraft:diamond", "minecraft:diamond_sword"), Things.suggest(SOURCES, "dia", 50));
        assertEquals(List.of("item:espada_de_fuego"), Things.suggest(SOURCES, "item:e", 50));
        assertEquals(1, Things.suggest(SOURCES, "dia", 1).size());
    }
}
