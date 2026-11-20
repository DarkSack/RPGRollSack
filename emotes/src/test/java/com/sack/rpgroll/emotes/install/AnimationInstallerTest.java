package com.sack.rpgroll.emotes.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class AnimationInstallerTest {

    private static final String FACTORY = AnimationInstaller.FACTORY_FILE;

    @TempDir
    Path tmp;

    private Path data;
    private Path players;
    private final List<String> warns = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        data = tmp.resolve("RPGRoll-Emotes");
        players = tmp.resolve("BetterModel/players");
        Files.createDirectories(data.resolve("animations"));
    }

    /** El de fábrica llega con los bytes del jar, como en EmotesPlugin. */
    private AnimationInstaller installer(String factory) {
        return new AnimationInstaller(data.toFile(), players.toFile(),
                factory == null ? null : factory.getBytes(StandardCharsets.UTF_8), warns::add);
    }

    private void source(String name, String content) throws Exception {
        Files.writeString(data.resolve("animations").resolve(name), content);
    }

    @Test
    void copiesWhenMissing() throws Exception {
        assertTrue(installer("v1").install());

        assertEquals("v1", Files.readString(players.resolve(FACTORY)));
        assertTrue(Files.exists(data.resolve("data/installed-hashes.yml")));
        assertFalse(installer("v1").install(), "sin cambios no hay nada que copiar ni que recargar");
        assertTrue(warns.isEmpty());
    }

    @Test
    void replacesWhenUnchangedSinceLastInstall() throws Exception {
        installer("v1").install();

        assertTrue(installer("v2").install());

        assertEquals("v2", Files.readString(players.resolve(FACTORY)));
        assertTrue(warns.isEmpty());
    }

    @Test
    void keepsAndWarnsWhenAdminEdited() throws Exception {
        installer("v1").install();
        Files.writeString(players.resolve(FACTORY), "editado por el admin");

        assertFalse(installer("v2").install());

        assertEquals("editado por el admin", Files.readString(players.resolve(FACTORY)));
        assertEquals(1, warns.size());
        assertTrue(warns.get(0).contains(FACTORY));
    }

    @Test
    void copiesPackAnimationsToo() throws Exception {
        source("mareas_emotes.bbmodel", "pack");
        source("LEEME.txt", "no es un modelo");

        installer("base").install();

        assertEquals("base", Files.readString(players.resolve(FACTORY)));
        assertEquals("pack", Files.readString(players.resolve("mareas_emotes.bbmodel")));
        assertFalse(Files.exists(players.resolve("LEEME.txt")));
    }

    @Test
    void packCannotOverrideFactoryModel() throws Exception {
        source(FACTORY, "de un pack");

        installer("fabrica").install();

        assertEquals("fabrica", Files.readString(players.resolve(FACTORY)));
        assertEquals(1, warns.size());
        assertTrue(warns.get(0).contains(FACTORY));
    }

    @Test
    void factoryCopyLeftByResourceCopierIsNotWarned() throws Exception {
        source(FACTORY, "fabrica");

        installer("fabrica").install();

        assertTrue(warns.isEmpty());
    }

    @Test
    void corruptHashesFileIsBackedUp() throws Exception {
        Path hashes = data.resolve("data/installed-hashes.yml");
        Files.createDirectories(hashes.getParent());
        Files.writeString(hashes, "esto: [no se cierra");

        installer("v1").install();

        File[] backups = hashes.getParent().toFile()
                .listFiles((d, n) -> n.startsWith("installed-hashes.yml.corrupto-"));
        assertEquals(1, backups.length);
        assertEquals("esto: [no se cierra", Files.readString(backups[0].toPath()));
        assertEquals(1, warns.size());
        assertTrue(warns.get(0).contains(backups[0].getName()));
        YamlConfiguration rewritten = new YamlConfiguration();
        rewritten.options().pathSeparator('/');
        rewritten.loadFromString(Files.readString(hashes));
        assertTrue(rewritten.isString(FACTORY), "el registro nuevo se escribe tras apartar el roto");
    }
}
