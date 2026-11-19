package com.sack.rpgroll.common.command;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

class SendersTest {

    @Test
    void aPlayerIsReturnedDirectly() {
        Player player = mock(Player.class);

        assertSame(player, Senders.asPlayer(player));
        assertTrue(Senders.isPlayer(player));
    }

    // El caso que motivó la clase: execute as <jugador> run <comando>.
    @Test
    void aPlayerBehindExecuteAsIsUnwrapped() {
        Player player = mock(Player.class);
        ProxiedCommandSender proxied = mock(ProxiedCommandSender.class);
        when(proxied.getCallee()).thenReturn(player);

        assertSame(player, Senders.asPlayer(proxied));
        assertTrue(Senders.isPlayer(proxied));
    }

    @Test
    void theConsoleIsNotAPlayer() {
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);

        assertNull(Senders.asPlayer(console));
        assertFalse(Senders.isPlayer(console));
    }

    // execute as sobre algo que no es un jugador (un mob, por ejemplo).
    @Test
    void executeAsANonPlayerIsStillNotAPlayer() {
        ProxiedCommandSender proxied = mock(ProxiedCommandSender.class);
        when(proxied.getCallee()).thenReturn(mock(ConsoleCommandSender.class));

        assertNull(Senders.asPlayer(proxied));
    }

    @Test
    void anUnknownSenderKindIsNotAPlayer() {
        assertNull(Senders.asPlayer(mock(CommandSender.class)));
    }

    // /magicadmin givecatalyst <id> [jugador]: con nombre, recibe ese jugador.
    @Test
    void aNamedRecipientWinsOverTheSender() {
        Player admin = mock(Player.class);
        Player named = mock(Player.class);

        assertSame(named, Senders.recipient(admin, new String[] { "givecatalyst", "x", "Ana" }, 2,
                Map.of("Ana", named)::get));
    }

    // Desde la consola (recompensas, menús, cajas) el nombre es la única forma.
    @Test
    void theConsoleCanNameARecipient() {
        Player named = mock(Player.class);

        assertSame(named, Senders.recipient(mock(ConsoleCommandSender.class),
                new String[] { "givecatalyst", "x", "Ana" }, 2, Map.of("Ana", named)::get));
    }

    @Test
    void withoutANameTheSenderReceives() {
        Player admin = mock(Player.class);

        assertSame(admin, Senders.recipient(admin, new String[] { "givecatalyst", "x" }, 2, name -> null));
    }

    @Test
    void anOfflineNameHasNoRecipient() {
        assertNull(Senders.recipient(mock(Player.class), new String[] { "givecatalyst", "x", "Nadie" }, 2,
                name -> null));
    }

    @Test
    void theConsoleWithoutANameHasNoRecipient() {
        assertNull(Senders.recipient(mock(ConsoleCommandSender.class), new String[] { "givecatalyst", "x" }, 2,
                name -> null));
    }
}
