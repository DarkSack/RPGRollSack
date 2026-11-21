package com.sack.rpgroll.common.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrigadierCommandsTest {

    private static CommandWords words() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(String.join("\n",
                "commands:",
                "  guild:",
                "    names: {es: gremio}",
                "    subcommands:",
                "      create: {es: crear}",
                "      invite: {es: invitar}"));
        return CommandWords.parse(yaml);
    }

    private static CommandSourceStack console() {
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.getSender()).thenReturn(mock(CommandSender.class));
        return source;
    }

    @Test
    void theCommandReceivesTheUsualSubcommandWhateverTheLanguage() throws Exception {
        AtomicReference<String[]> got = new AtomicReference<>();
        CommandExecutor executor = (sender, command, label, args) -> {
            got.set(args);
            return true;
        };
        var bridge = new BrigadierCommands.ExecutorBridge("guild", executor, null, null, words());

        bridge.execute(console(), new String[] { "crear", "Lobos" });
        assertArrayEquals(new String[] { "create", "Lobos" }, got.get());
    }

    @Test
    void theFirstWordIsSuggestedInThePlayersLanguage() throws Exception {
        List<String[]> asked = new ArrayList<>();
        TabCompleter completer = (sender, command, label, args) -> {
            asked.add(args);
            if (args.length == 1) {
                return List.of("create", "invite").stream().filter(s -> s.startsWith(args[0])).toList();
            }
            return args[0].equals("invite") ? List.of("Steve") : List.of();
        };
        var bridge = new BrigadierCommands.ExecutorBridge("guild", (s, c, l, a) -> true, completer, null, words());

        assertEquals(List.of("crear", "invitar"), List.copyOf(bridge.suggest(console(), new String[0])));
        assertEquals(List.of("invitar"), List.copyOf(bridge.suggest(console(), new String[] { "inv" })));
        // Más allá del primero, el comando ve su palabra de siempre.
        assertEquals(List.of("Steve"), List.copyOf(bridge.suggest(console(), new String[] { "invitar", "" })));
        assertEquals("invite", asked.get(asked.size() - 1)[0]);
    }

    @Test
    void withoutATableNothingChanges() {
        AtomicReference<String[]> got = new AtomicReference<>();
        CommandExecutor executor = (sender, command, label, args) -> {
            got.set(args);
            return true;
        };
        var bridge = new BrigadierCommands.ExecutorBridge("quest", executor, null, null, CommandWords.empty());
        bridge.execute(console(), new String[] { "crear" });
        assertArrayEquals(new String[] { "crear" }, got.get());
    }

    @Test
    void aCommandWithoutSubcommandsInTheTableCompletesAsBefore() throws Exception {
        TabCompleter completer = (sender, command, label, args) -> args[0].isEmpty() ? List.of("tipos:")
                : List.of("item:" + args[0]);
        var bridge = new BrigadierCommands.ExecutorBridge("dar", (s, c, l, a) -> true, completer, null, words());
        assertEquals(List.of("item:med"), List.copyOf(bridge.suggest(console(), new String[] { "med" })));
    }

    @Test
    void localizedNamesAreAddedAsAliases() throws Exception {
        assertEquals(List.of("g", "gremio"), BrigadierCommands.aliases("guild", List.of("g"), words()));
        assertEquals(List.of("gremio"), BrigadierCommands.aliases("guild", null, words()));
    }
}
