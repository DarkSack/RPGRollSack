package com.sack.rpgroll.licensing;

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

    /** Hace de carpeta plugins/, que es donde vive el id compartido. */
    @TempDir
    File pluginsFolder;

    private Plugin plugin;

    /** Un plugin con su carpeta de datos dentro de plugins/, como en un servidor real. */
    private Plugin pluginNamed(String name) {
        File dataFolder = new File(pluginsFolder, name);
        Plugin created = mock(Plugin.class);
        when(created.getDataFolder()).thenReturn(dataFolder);
        when(created.getLogger()).thenReturn(Logger.getLogger("ServerIdentityTest"));
        return created;
    }

    @BeforeEach
    void setUp() {
        plugin = pluginNamed("RPGRoll");
    }

    private File idFile() {
        return new File(pluginsFolder, ".rpgroll-server-id");
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
        assertEquals(ServerIdentity.resolve(plugin), ServerIdentity.resolve(plugin));
    }

    // Los 24 módulos verifican licencia por separado. Si cada uno generara su
    // propio id, un solo servidor aparecería 24 veces en el panel.
    @Test
    void everyModuleOnTheSameServerSharesOneId() {
        String fromCore = ServerIdentity.resolve(plugin);
        String fromMagic = ServerIdentity.resolve(pluginNamed("RPGRoll-Magic"));
        String fromTraps = ServerIdentity.resolve(pluginNamed("RPGRoll-Traps"));

        assertEquals(fromCore, fromMagic);
        assertEquals(fromCore, fromTraps);
    }

    @Test
    void theIdLivesOutsideAnySinglePluginFolder() {
        ServerIdentity.resolve(plugin);

        assertTrue(idFile().exists());
        assertTrue(!new File(new File(pluginsFolder, "RPGRoll"), ".rpgroll-server-id").exists());
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

        assertDoesNotThrow(() -> UUID.fromString(ServerIdentity.resolve(plugin)));
    }

    // Una estadística no debe poder tumbar la verificación de licencia.
    @Test
    void anUnwritableLocationStillYieldsAnId() throws Exception {
        File blocked = new File(pluginsFolder, "bloqueado");
        Files.writeString(blocked.toPath(), "soy un archivo, no una carpeta", StandardCharsets.UTF_8);

        Plugin broken = mock(Plugin.class);
        when(broken.getDataFolder()).thenReturn(new File(blocked, "RPGRoll"));
        when(broken.getLogger()).thenReturn(Logger.getLogger("ServerIdentityTest"));

        assertDoesNotThrow(() -> assertNotNull(ServerIdentity.resolve(broken)));
    }
}
