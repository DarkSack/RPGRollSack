package com.sack.rpgroll.common.content;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentRegistryTest {

    private record Dummy(String id) implements RPGContent {
    }

    private JavaPlugin plugin;
    private ContentRegistry<Dummy> registry;

    @BeforeEach
    void setUp() {
        plugin = mock(JavaPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ContentRegistryTest"));
        registry = new ContentRegistry<>(plugin, "Dummy");
    }

    @Test
    void registerAddsNewItemAndReturnsTrue() {
        assertTrue(registry.register(new Dummy("a")));
        assertEquals(1, registry.count());
    }

    @Test
    void registerRejectsDuplicateIdAndReturnsFalse() {
        registry.register(new Dummy("a"));

        assertFalse(registry.register(new Dummy("a")));
        assertEquals(1, registry.count());
    }

    @Test
    void getReturnsEmptyForNullId() {
        assertTrue(registry.get(null).isEmpty());
    }

    @Test
    void getReturnsEmptyForUnknownId() {
        assertTrue(registry.get("missing").isEmpty());
    }

    @Test
    void getReturnsRegisteredItem() {
        Dummy dummy = new Dummy("a");
        registry.register(dummy);

        assertEquals(dummy, registry.get("a").orElseThrow());
    }

    @Test
    void existsIsFalseForNullId() {
        assertFalse(registry.exists(null));
    }

    @Test
    void existsReflectsRegisteredState() {
        assertFalse(registry.exists("a"));
        registry.register(new Dummy("a"));
        assertTrue(registry.exists("a"));
    }

    @Test
    void clearRemovesAllItems() {
        registry.register(new Dummy("a"));
        registry.register(new Dummy("b"));

        registry.clear();

        assertEquals(0, registry.count());
        assertFalse(registry.exists("a"));
    }

    @Test
    void getAllReturnsAllRegisteredItemsInInsertionOrder() {
        registry.register(new Dummy("a"));
        registry.register(new Dummy("b"));

        assertEquals(2, registry.getAll().size());
    }
}
