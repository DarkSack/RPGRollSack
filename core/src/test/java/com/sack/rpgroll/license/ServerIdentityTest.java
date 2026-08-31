package com.sack.rpgroll.license;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerIdentityTest {

    @TempDir
    File dataFolder;

    private Plugin plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ServerIdentityTest"));
    }

    private File idFile() {
        return new File(dataFolder, ".server-id");
    }

    @Test
    void firstCallGeneratesAndPersistsAnId() {
        String id = ServerIdentity.resolve(plugin);

        assertNotNull(id);
        assertTrue(idFile().exists());
        assertDoesNotThrow(() -> UUID.fromString(id));
    }

    // Lo que hace útil el conteo: reiniciar el servidor NO debe inventar
    // un servidor nuevo.
    @Test
    void restartingReusesTheSameId() {
        String first = ServerIdentity.resolve(plugin);
        String second = ServerIdentity.resolve(plugin);

        assertEquals(first, second);
    }

    @Test
    void storedIdIsReadBackVerbatim() throws Exception {
        Files.writeString(idFile().toPath(), "id-preexistente", StandardCharsets.UTF_8);

        assertEquals("id-preexistente", ServerIdentity.resolve(plugin));
    }

    @Test
    void surroundingWhitespaceInTheStoredIdIsTrimmed() throws Exception {
        Files.writeString(idFile().toPath(), "  id-con-espacios \n", StandardCharsets.UTF_8);

        assertEquals("id-con-espacios", ServerIdentity.resolve(plugin));
    }

    @Test
    void anEmptyFileIsReplacedWithAFreshId() throws Exception {
        Files.writeString(idFile().toPath(), "   \n", StandardCharsets.UTF_8);

        String id = ServerIdentity.resolve(plugin);

        assertDoesNotThrow(() -> UUID.fromString(id));
    }

    // Una estadística no debe poder tumbar la verificación de licencia.
    @Test
    void anUnwritableFolderStillYieldsAnId() {
        Plugin brokenPlugin = mock(Plugin.class);
        when(brokenPlugin.getDataFolder()).thenReturn(new File(dataFolder, "archivo-no-carpeta"));
        when(brokenPlugin.getLogger()).thenReturn(Logger.getLogger("ServerIdentityTest"));

        assertDoesNotThrow(() -> {
            String id = ServerIdentity.resolve(brokenPlugin);
            assertNotNull(id);
        });
    }
}
