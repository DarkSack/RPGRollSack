package com.sack.rpgroll.recipes.index;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchTextTest {

    @Test
    void normalizeQuitaTildesColoresYGuionesBajos() {
        assertEquals("espada runica", SearchText.normalize("&bEspada <gold>Rúnica</gold>"));
        assertEquals("diamond sword", SearchText.normalize("DIAMOND_SWORD"));
        assertEquals("", SearchText.normalize(null));
    }

    @Test
    void todasLasPalabrasTienenQueAparecer() {
        String haystack = SearchText.normalize("diamond_sword Espada de diamante Mesa de crafteo");
        assertTrue(SearchText.matches(SearchText.terms("espada diamante"), haystack, Set.of()));
        assertTrue(SearchText.matches(SearchText.terms("  DIAMOND  "), haystack, Set.of()));
        assertFalse(SearchText.matches(SearchText.terms("espada hierro"), haystack, Set.of()));
    }

    @Test
    void arrobaBuscaEnElOrigen() {
        Set<String> sources = Set.of("rpgroll-crafting");
        assertTrue(SearchText.matches(SearchText.terms("@crafting"), "lo que sea", sources));
        assertTrue(SearchText.matches(SearchText.terms("@crafting lo"), "lo que sea", sources));
        assertFalse(SearchText.matches(SearchText.terms("@minecraft"), "lo que sea", sources));
    }

    @Test
    void consultaVaciaNoFiltra() {
        assertEquals(List.of(), SearchText.terms("   "));
        assertEquals(List.of(), SearchText.terms("@"));
        assertTrue(SearchText.matches(List.of(), "x", Set.of()));
    }
}
