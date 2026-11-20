package com.sack.rpgroll.emotes.gui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuLayoutTest {

    private static List<Integer> items(int n) {
        return IntStream.range(0, n).boxed().toList();
    }

    @Test
    void pagesOf28() {
        assertEquals(2, MenuLayout.pages(30, 28));
        assertEquals(28, MenuLayout.page(items(30), 0, 28).size());
        assertEquals(2, MenuLayout.page(items(30), 1, 28).size());
    }

    @Test
    void zeroEmotesIsOnePage() {
        assertEquals(1, MenuLayout.pages(0, 28));
        assertTrue(MenuLayout.page(List.<Integer>of(), 0, 28).isEmpty());
    }

    @Test
    void outOfRangePageIsClamped() {
        assertEquals(2, MenuLayout.page(items(30), 9, 28).size());
        assertEquals(28, MenuLayout.page(items(30), -3, 28).size());
    }

    @Test
    void slotsDoNotOverlap() {
        List<Integer> all = new ArrayList<>();
        all.addAll(MenuLayout.CATEGORY_SLOTS);
        all.addAll(MenuLayout.CONTENT_SLOTS);
        all.addAll(MenuLayout.FAVORITE_SLOTS);
        all.addAll(List.of(MenuLayout.PREV, MenuLayout.NEXT, MenuLayout.STOP));
        Set<Integer> unique = new HashSet<>(all);
        assertEquals(all.size(), unique.size());
        assertTrue(all.stream().allMatch(s -> s >= 0 && s <= 53));
        assertEquals(28, MenuLayout.CONTENT_SLOTS.size());
    }

    @Test
    void onlyLeftClickLaunches() {
        for (org.bukkit.event.inventory.ClickType c : org.bukkit.event.inventory.ClickType.values()) {
            MenuLayout.ClickAction expected = switch (c) {
                case LEFT, SHIFT_LEFT -> MenuLayout.ClickAction.LAUNCH;
                case RIGHT, SHIFT_RIGHT -> MenuLayout.ClickAction.TOGGLE_FAVORITE;
                default -> MenuLayout.ClickAction.NONE;
            };
            assertEquals(expected, MenuLayout.action(c), c.name());
        }
        assertEquals(MenuLayout.ClickAction.NONE, MenuLayout.action(org.bukkit.event.inventory.ClickType.NUMBER_KEY));
    }
}
