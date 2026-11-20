package com.sack.rpgroll.sackresourcepack.api;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** {@link AssetRegistrationService#registerArchive}: el zip de otro plugin pasa a ser un módulo de content/. */
class RegisterArchiveTest {

    @TempDir
    File dir;

    private File content;
    private AssetRegistrationService service;

    @BeforeEach
    void setUp() {
        content = new File(dir, "content");
        Plugin plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("RegisterArchiveTest"));
        service = new AssetRegistrationService(plugin, content);
    }

    @Test
    void copiesAssetsAndWritesPackYml() throws IOException {
        File zip = zip(Map.of("assets/bettermodel/models/item/a.json", "{\"a\":1}",
                "assets/bettermodel/textures/item/b.png", "png"));

        assertTrue(service.registerArchive("bettermodel", zip, "bettermodel", 20, "Modelos y emotes de BetterModel"));

        File module = new File(content, "bettermodel");
        assertEquals("{\"a\":1}", read(new File(module, "assets/bettermodel/models/item/a.json")));
        assertEquals("png", read(new File(module, "assets/bettermodel/textures/item/b.png")));
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(new File(module, "pack.yml"));
        assertEquals("bettermodel", yml.getString("id"));
        assertEquals("bettermodel", yml.getString("name"));
        assertEquals("1.0", yml.getString("version"));
        assertEquals("BetterModel", yml.getString("author"));
        assertEquals("bettermodel", yml.getString("namespace"));
        assertEquals(20, yml.getInt("priority"));
        assertEquals("Modelos y emotes de BetterModel", yml.getString("description"));
    }

    @Test
    void unchangedZipIsNotCopiedAgain() throws IOException {
        // Copiar miles de archivos en cada recarga de BetterModel cuesta: si el zip es el mismo, no se toca nada.
        File zip = zip(Map.of("assets/bettermodel/models/item/a.json", "{\"a\":1}"));
        assertTrue(service.registerArchive("bettermodel", zip, "bettermodel", 20, "d"));
        File copied = new File(content, "bettermodel/assets/bettermodel/models/item/a.json");
        Files.writeString(copied.toPath(), "marca");

        assertTrue(service.registerArchive("bettermodel", zip, "bettermodel", 20, "d"));

        assertEquals("marca", read(copied));
    }

    @Test
    void sameZipWithOtherPriorityRewritesPackYml() throws IOException {
        // El salto por hash no puede dejar viejos priority, namespace ni description.
        File zip = zip(Map.of("assets/bettermodel/models/item/a.json", "{\"a\":1}"));
        assertTrue(service.registerArchive("bettermodel", zip, "bettermodel", 20, "d"));

        assertTrue(service.registerArchive("bettermodel", zip, "bettermodel", 35, "otra"));

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(new File(content, "bettermodel/pack.yml"));
        assertEquals(35, yml.getInt("priority"));
        assertEquals("otra", yml.getString("description"));
    }

    @Test
    void changedZipIsCopiedAgain() throws IOException {
        File first = zip(Map.of("assets/bettermodel/models/item/a.json", "{\"a\":1}"));
        assertTrue(service.registerArchive("bettermodel", first, "bettermodel", 20, "d"));

        File second = zip(Map.of("assets/bettermodel/models/item/a.json", "{\"a\":2}"));
        assertTrue(service.registerArchive("bettermodel", second, "bettermodel", 20, "d"));

        assertEquals("{\"a\":2}", read(new File(content, "bettermodel/assets/bettermodel/models/item/a.json")));
    }

    @Test
    void skipsMcmetaAndPng() throws IOException {
        File zip = zip(Map.of("pack.mcmeta", "{}", "pack.png", "png", "assets/bettermodel/items/x.json", "{}"));

        assertTrue(service.registerArchive("bettermodel", zip, "bettermodel", 20, "d"));

        File module = new File(content, "bettermodel");
        assertFalse(new File(module, "pack.mcmeta").exists());
        assertFalse(new File(module, "pack.png").exists());
        assertTrue(new File(module, "assets/bettermodel/items/x.json").isFile());
    }

    @Test
    void clearsOldFiles() throws IOException {
        File old = new File(content, "bettermodel/assets/bettermodel/models/viejo.json");
        Files.createDirectories(old.getParentFile().toPath());
        Files.writeString(old.toPath(), "{}");
        File other = new File(content, "otro/pack.yml");
        Files.createDirectories(other.getParentFile().toPath());
        Files.writeString(other.toPath(), "id: otro");

        assertTrue(service.registerArchive("bettermodel", zip(Map.of("assets/bettermodel/nuevo.json", "{}")),
                "bettermodel", 20, "d"));

        assertFalse(old.exists());
        assertTrue(new File(content, "bettermodel/assets/bettermodel/nuevo.json").isFile());
        assertTrue(other.isFile(), "los demás módulos no se tocan");
    }

    @Test
    void missingZipReturnsFalse() throws IOException {
        File kept = new File(content, "bettermodel/assets/bettermodel/a.json");
        Files.createDirectories(kept.getParentFile().toPath());
        Files.writeString(kept.toPath(), "{}");

        assertFalse(service.registerArchive("bettermodel", new File(dir, "no-existe.zip"), "bettermodel", 20, "d"));
        assertTrue(kept.isFile(), "sin zip no se borra lo que había");
    }

    @Test
    void entriesOutsideTheModuleAreRejected() throws IOException {
        File zip = zip(Map.of("../fuera.json", "{}", "assets/bettermodel/a.json", "{}"));

        assertFalse(service.registerArchive("bettermodel", zip, "bettermodel", 20, "d"));
        assertFalse(new File(content, "fuera.json").exists());
    }

    @Test
    void rejectsDotModuleIds() throws IOException {
        File kept = new File(content, "otro/pack.yml");
        Files.createDirectories(kept.getParentFile().toPath());
        Files.writeString(kept.toPath(), "id: otro");
        File zip = zip(Map.of("assets/bettermodel/a.json", "{}"));

        for (String id : new String[] {".", "..", "a.b", "A", ""}) {
            assertFalse(service.registerArchive(id, zip, "bettermodel", 20, "d"), "id «" + id + "»");
        }
        assertTrue(kept.isFile(), "no se borra nada de content/");
        assertTrue(content.getParentFile().isDirectory());
        assertFalse(new File(content, "bettermodel").exists());
    }

    @Test
    void copiesOnlyItsNamespace() throws IOException {
        File zip = zip(Map.of("assets/bettermodel/models/a.json", "{}",
                "assets/minecraft/models/item/leather_horse_armor.json", "{}",
                "assets/otro/textures/b.png", "png",
                "overlay_1/assets/bettermodel/c.json", "{}",
                "assets/bettermodelx/d.json", "{}"));

        assertTrue(service.registerArchive("bettermodel", zip, "bettermodel", 20, "d"));

        File module = new File(content, "bettermodel");
        assertTrue(new File(module, "assets/bettermodel/models/a.json").isFile());
        assertFalse(new File(module, "assets/minecraft").exists());
        assertFalse(new File(module, "assets/otro").exists());
        assertFalse(new File(module, "overlay_1").exists());
        assertFalse(new File(module, "assets/bettermodelx").exists());
    }

    @Test
    void failureMidCopyKeepsOldContent() throws IOException {
        File old = new File(content, "bettermodel/assets/bettermodel/viejo.json");
        Files.createDirectories(old.getParentFile().toPath());
        Files.writeString(old.toPath(), "{}");
        Files.writeString(new File(content, "bettermodel/pack.yml").toPath(), "id: bettermodel");
        // «x» es un archivo y luego una carpeta: la segunda entrada no se puede escribir.
        File zip = zip(Map.of("assets/bettermodel/x", "1", "assets/bettermodel/x/y.json", "{}"));

        assertFalse(service.registerArchive("bettermodel", zip, "bettermodel", 20, "d"));

        assertTrue(old.isFile(), "el módulo anterior sigue entero");
        assertEquals("id: bettermodel", read(new File(content, "bettermodel/pack.yml")));
        assertNoTempFolders();
    }

    @Test
    void successLeavesNoTempFolder() throws IOException {
        File old = new File(content, "bettermodel/assets/bettermodel/viejo.json");
        Files.createDirectories(old.getParentFile().toPath());
        Files.writeString(old.toPath(), "{}");

        assertTrue(service.registerArchive("bettermodel", zip(Map.of("assets/bettermodel/a.json", "{}")),
                "bettermodel", 20, "d"));

        assertFalse(old.exists());
        assertNoTempFolders();
    }

    private void assertNoTempFolders() {
        String[] left = content.list((d, n) -> n.startsWith("."));
        assertEquals(0, left == null ? 0 : left.length, "quedó una carpeta temporal en content/");
    }

    private File zip(Map<String, String> entries) throws IOException {
        File zip = new File(dir, "build-" + System.nanoTime() + ".zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            for (Map.Entry<String, String> e : new java.util.TreeMap<>(entries).entrySet()) {
                out.putNextEntry(new ZipEntry(e.getKey()));
                out.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return zip;
    }

    private static String read(File f) throws IOException {
        return Files.readString(f.toPath());
    }
}
