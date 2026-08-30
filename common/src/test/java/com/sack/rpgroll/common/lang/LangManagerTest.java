package com.sack.rpgroll.common.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LangManagerTest {

    @TempDir
    File dataFolder;

    private Plugin plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("LangManagerTest"));
        when(plugin.getResource(anyString())).thenReturn(null);
    }

    private void writeLangFile(String locale, String content) throws IOException {
        File langDir = new File(dataFolder, "lang");
        langDir.mkdirs();
        Files.writeString(new File(langDir, locale + ".yml").toPath(), content, StandardCharsets.UTF_8);
    }

    @Test
    void rawResolvesKeyFromActiveLocale() throws IOException {
        writeLangFile("en", "greeting: 'Hello'");
        writeLangFile("es", "greeting: 'Hola'");

        LangManager lang = new LangManager(plugin, List.of("en", "es"), "en");
        lang.reload("es");

        assertEquals("Hola", lang.raw("greeting"));
    }

    @Test
    void rawFallsBackToFallbackLocaleWhenKeyMissingInActive() throws IOException {
        writeLangFile("en", "greeting: 'Hello'\nonly_in_english: 'Only here'");
        writeLangFile("es", "greeting: 'Hola'");

        LangManager lang = new LangManager(plugin, List.of("en", "es"), "en");
        lang.reload("es");

        assertEquals("Only here", lang.raw("only_in_english"));
    }

    @Test
    void rawReturnsRawKeyWhenMissingEverywhere() throws IOException {
        writeLangFile("en", "greeting: 'Hello'");

        LangManager lang = new LangManager(plugin, List.of("en"), "en");

        assertEquals("nonexistent.key", lang.raw("nonexistent.key"));
    }

    @Test
    void rawReplacesPlaceholderPairs() throws IOException {
        writeLangFile("en", "welcome: 'Welcome {player}, you are level {level}'");

        LangManager lang = new LangManager(plugin, List.of("en"), "en");

        assertEquals("Welcome Steve, you are level 5", lang.raw("welcome", "player", "Steve", "level", 5));
    }

    @Test
    void rawIgnoresDanglingPlaceholderArgumentWithoutValue() throws IOException {
        writeLangFile("en", "msg: 'Value is {value}'");

        LangManager lang = new LangManager(plugin, List.of("en"), "en");

        // Número impar de argumentos: el último placeholder sin pareja se ignora, no revienta.
        assertEquals("Value is {value}", lang.raw("msg", "value"));
    }

    @Test
    void rawNeverReturnsNullEvenWithNoLocalesLoaded() {
        LangManager lang = new LangManager(plugin, List.of("de"), "fr");

        assertNotNull(lang.raw("anything"));
        assertEquals("anything", lang.raw("anything"));
    }

    @Test
    void reloadFallsBackWhenConfiguredLocaleNeverLoaded() throws IOException {
        writeLangFile("en", "greeting: 'Hello'");

        LangManager lang = new LangManager(plugin, List.of("en"), "en");
        lang.reload("nonexistent-locale");

        assertEquals("Hello", lang.raw("greeting"));
    }

    @Test
    void componentParsesRawTextIntoComponent() throws IOException {
        writeLangFile("en", "greeting: '&6Hello'");

        LangManager lang = new LangManager(plugin, List.of("en"), "en");
        Component component = lang.component("greeting");

        assertEquals("Hello", PlainTextComponentSerializer.plainText().serialize(component));
    }

    @Test
    void sendDeliversComponentToTarget() throws IOException {
        writeLangFile("en", "greeting: 'Hello'");

        LangManager lang = new LangManager(plugin, List.of("en"), "en");
        CommandSender sender = mock(CommandSender.class);

        lang.send(sender, "greeting");

        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(sender).sendMessage(captor.capture());
        assertEquals("Hello", PlainTextComponentSerializer.plainText().serialize(captor.getValue()));
    }

    @Test
    void emptyPlaceholderKeyStillReplacesCorrectly() throws IOException {
        writeLangFile("en", "msg: 'A={a} B={b} A={a}'");

        LangManager lang = new LangManager(plugin, List.of("en"), "en");

        assertTrue(lang.raw("msg", "a", "1", "b", "2").equals("A=1 B=2 A=1"));
    }
}
