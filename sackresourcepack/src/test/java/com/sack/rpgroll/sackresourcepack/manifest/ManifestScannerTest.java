package com.sack.rpgroll.sackresourcepack.manifest;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManifestScannerTest {

    @TempDir
    File dir;

    @Test
    void skipsHiddenTempFolders() throws IOException {
        File content = new File(dir, "SackResourcePack/content");
        write(new File(content, "bettermodel/pack.yml"), "id: bettermodel");
        // La copia a medias de registerArchive: tiene pack.yml pero no es un módulo.
        write(new File(content, ".bettermodel.tmp-123/pack.yml"), "id: bettermodel");
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(new File(dir, "SackResourcePack"));
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ManifestScannerTest"));

        List<AssetModule> modules = new ManifestScanner(plugin, content).scan();

        assertEquals(List.of("bettermodel"), modules.stream().map(AssetModule::id).toList());
    }

    private static void write(File f, String text) throws IOException {
        Files.createDirectories(f.getParentFile().toPath());
        Files.writeString(f.toPath(), text);
    }
}
