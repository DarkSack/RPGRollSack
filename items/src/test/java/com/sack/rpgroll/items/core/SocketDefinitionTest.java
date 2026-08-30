package com.sack.rpgroll.items.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketDefinitionTest {

    @Test
    void acceptsAnyTypeWhenAcceptedTypesEmpty() {
        SocketDefinition socket = new SocketDefinition("socket-1", List.of());

        assertTrue(socket.accepts("OFFENSIVE"));
        assertTrue(socket.accepts("anything"));
    }

    @Test
    void acceptsAnyTypeWhenAcceptedTypesNull() {
        SocketDefinition socket = new SocketDefinition("socket-1", null);

        assertTrue(socket.accepts("OFFENSIVE"));
    }

    @Test
    void acceptsOnlyListedTypesCaseInsensitively() {
        SocketDefinition socket = new SocketDefinition("socket-1", List.of("OFFENSIVE", "DEFENSIVE"));

        assertTrue(socket.accepts("offensive"));
        assertFalse(socket.accepts("UTILITY"));
    }
}
