package com.sack.rpgroll.extras.sort;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventorySorterTest {

    /** Un montón de prueba: id, grupo, cantidad y tope. */
    record Stack(String id, int group, int amount, int max) {
    }

    static final InventorySorter.Ops<Stack> OPS = new InventorySorter.Ops<>() {
        public boolean empty(Stack s) { return s == null || s.amount() <= 0; }
        public int group(Stack s) { return s.group(); }
        public String name(Stack s) { return s.id(); }
        public boolean similar(Stack a, Stack b) { return a.id().equals(b.id()); }
        public int amount(Stack s) { return s.amount(); }
        public int max(Stack s) { return s.max(); }
        public Stack withAmount(Stack s, int amount) { return new Stack(s.id(), s.group(), amount, s.max()); }
    };

    private static List<Stack> sort(Stack... stacks) {
        return InventorySorter.sort(new ArrayList<>(Arrays.asList(stacks)), OPS);
    }

    @Test
    void mergesPartialStacksUpToTheirMax() {
        assertEquals(List.of(new Stack("dirt", 0, 64, 64), new Stack("dirt", 0, 6, 64)),
                sort(new Stack("dirt", 0, 40, 64), null, new Stack("dirt", 0, 30, 64)));
    }

    @Test
    void ordersByGroupThenName() {
        List<Stack> sorted = sort(new Stack("bread", 3, 1, 64), new Stack("stone", 0, 1, 64),
                new Stack("andesite", 0, 1, 64), new Stack("sword", 1, 1, 1));
        assertEquals(List.of("andesite", "stone", "sword", "bread"), sorted.stream().map(Stack::id).toList());
    }

    @Test
    void neverExceedsTheMaxOfUnstackableItems() {
        assertEquals(List.of(new Stack("sword", 1, 1, 1), new Stack("sword", 1, 1, 1)),
                sort(new Stack("sword", 1, 1, 1), new Stack("sword", 1, 1, 1)));
    }

    @Test
    void keepsEveryItem() {
        Stack[] input = {new Stack("a", 2, 13, 16), new Stack("b", 0, 64, 64), new Stack("a", 2, 15, 16),
                new Stack("a", 2, 16, 16), new Stack("c", 5, 3, 64), new Stack("b", 0, 1, 64)};
        List<Stack> sorted = sort(input);
        for (String id : List.of("a", "b", "c")) {
            int before = Arrays.stream(input).filter(s -> s.id().equals(id)).mapToInt(Stack::amount).sum();
            int after = sorted.stream().filter(s -> s.id().equals(id)).mapToInt(Stack::amount).sum();
            assertEquals(before, after, id);
        }
        assertEquals(6, sorted.size()); // a: 16+16+12, b: 64+1, c: 3
    }
}
