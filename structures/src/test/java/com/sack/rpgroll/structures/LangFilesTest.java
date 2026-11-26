package com.sack.rpgroll.structures;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Los lang se leen con el YamlConfiguration de Bukkit, como en el servidor: ahí {@code yes:}/{@code no:} son
 * booleanos y la clave deja de existir. Cada clave que el código pide con un literal tiene que dar un texto.
 */
class LangFilesTest {

    static final List<String> FILES = List.of("es", "en", "pt_BR");
    /** {@code component("a.b"}, {@code send(x, "a.b"}, {@code raw("a.b"}: solo literales completos, sin concatenar. */
    static final Pattern USE = Pattern.compile(
            "\\.(?:component|send|raw)\\(\\s*(?:[\\w().]+,\\s*)?\"([a-z0-9_-]+(?:\\.[a-z0-9_-]+)+)\"\\s*[,)]");

    static YamlConfiguration load(String name) throws IOException {
        try (InputStream in = LangFilesTest.class.getResourceAsStream("/lang/" + name + ".yml")) {
            assertNotNull(in, name + ".yml");
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    static Set<String> usedKeys() throws IOException {
        Set<String> out = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path f : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher m = USE.matcher(Files.readString(f));
                while (m.find()) {
                    out.add(m.group(1));
                }
            }
        }
        return out;
    }

    @Test void everyLeafIsATextAndNoKeyTurnedIntoABoolean() throws IOException {
        for (String name : FILES) {
            YamlConfiguration y = load(name);
            for (String key : y.getKeys(true)) {
                if (y.isConfigurationSection(key)) {
                    continue;
                }
                assertTrue(y.isString(key), name + ": " + key + " no es un texto");
                for (String part : key.split("\\.")) {
                    assertFalse(part.equals("true") || part.equals("false"), name + ": " + key);
                }
            }
        }
    }

    @Test void confirmButtonsResolve() throws IOException {
        for (String name : FILES) {
            YamlConfiguration y = load(name);
            assertNotNull(y.getString("menu.confirm.accept"), name);
            assertNotNull(y.getString("menu.confirm.cancel"), name);
        }
    }

    @Test void everyKeyTheCodeUsesResolvesInEveryFile() throws IOException {
        Set<String> used = usedKeys();
        assertTrue(used.contains("menu.confirm.title") || used.size() > 20, "el escáner no encontró claves: " + used);
        for (String name : FILES) {
            YamlConfiguration y = load(name);
            for (String key : used) {
                assertNotNull(y.getString(key), name + ": falta " + key);
            }
        }
    }

    @Test void startResultsAndProtectionDenialsResolve() throws IOException {
        // Claves que no pasan por un literal en send(...): salen de un switch o de AreaGuard.Denial.
        List<String> keys = new java.util.ArrayList<>(List.of("protect.worldguard", "protect.griefprevention",
                "protect.error"));
        for (var r : com.sack.rpgroll.structures.build.ConstructionEngine.StartResult.values()) {
            keys.add(com.sack.rpgroll.structures.gui.MaterialsMenu.startKey(r));
        }
        for (String name : FILES) {
            YamlConfiguration y = load(name);
            for (String key : keys) {
                assertNotNull(y.getString(key), name + ": falta " + key);
            }
        }
    }

    @Test void allFilesHaveTheSameKeys() throws IOException {
        Set<String> es = new TreeSet<>(load("es").getKeys(true));
        for (String name : List.of("en", "pt_BR")) {
            assertEquals(es, new TreeSet<>(load(name).getKeys(true)), name);
        }
    }

    @Test void cancelLoreDoesNotPromiseTheRefundToWhoeverClicks() throws IOException {
        // El dueño de la mesa también cancela, pero la devolución va siempre al dueño de la obra.
        for (String lang : List.of("es", "en", "pt_BR")) {
            String lore = load(lang).getString("build.cancel-confirm-lore");
            assertNotNull(lore, lang);
            String l = lore.toLowerCase(java.util.Locale.ROOT);
            assertFalse(l.contains("se te devuelve") || l.contains("you get back") || l.contains("você recebe"),
                    lang + ": " + lore);
            assertTrue(l.contains("{percent}"), lang + ": " + lore);
        }
    }
}
